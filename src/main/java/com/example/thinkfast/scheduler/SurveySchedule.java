package com.example.thinkfast.scheduler;

import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.domain.survey.Survey;
import com.example.thinkfast.realtime.RedisPublisher;
import com.example.thinkfast.repository.ai.InsightReportRepository;
import com.example.thinkfast.repository.survey.QuestionRepository;
import com.example.thinkfast.repository.survey.ResponseRepository;
import com.example.thinkfast.repository.survey.SurveyRepository;
import com.example.thinkfast.service.ai.SummaryService;
import com.example.thinkfast.service.ai.WordCloudService;
import com.example.thinkfast.service.ai.InsightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SurveySchedule {

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final ResponseRepository responseRepository;
    private final RedisPublisher redisPublisher;
    private final SummaryService summaryService;
    private final WordCloudService wordCloudService;
    private final InsightService insightService;
    private final InsightReportRepository insightReportRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updateExpiredSurvey() {
        log.info("[SCHEDULER] SURVEY CHECK");
        try {
            // 현재 시간
            LocalDateTime now = LocalDateTime.now();
            
            // 종료 시간이 지난 활성화된 설문 조회
            List<Survey> expiredSurveys = surveyRepository.findByIsActiveTrueAndEndTimeBefore(now);
            
            // 설문 비활성화 처리
            expiredSurveys.forEach(survey -> {
                survey.setIsActive(false);
                log.info("[UPDATE] survey update unactive - ID: {}, 제목: {}, 종료일: {}", 
                    survey.getId(), 
                    survey.getTitle(), 
                    survey.getEndTime().toLocalDate());
            });
            
            // 변경사항 저장
            surveyRepository.saveAll(expiredSurveys);

            expiredSurveys.forEach(survey -> {
                redisPublisher.sendAlarm(survey.getId(), "SURVEY_EXPIRED");
                
                // 설문 종료 후 요약 리포트 비동기 생성
                generateSummaryReportIfNotExists(survey.getId());
                
                // 설문 종료 후 워드클라우드 비동기 생성
                wordCloudService.saveWordCloudsForSurveyAsync(survey.getId());
                
                // 설문 종료 후 인사이트 텍스트 비동기 생성
                insightService.saveInsightsForSurveyAsync(survey.getId());
            });
        } catch (Exception e) {
            log.error("스케줄러 실행 중 오류 발생", e);
        }
    }

    /**
     * 실시간 통계 업데이트 스케줄러
     * 1분마다 진행 중인 설문에 대해 요약 리포트, 워드클라우드, 인사이트, 통계를 저장 또는 수정
     * - 진행 중인 설문 (isActive = true, isDeleted = false, endTime > 현재 시간)
     * - 최소 응답 수 1개 이상인 설문만 처리
     * - 배치 처리로 성능 최적화
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updateActiveSurveyReports() {
        log.info("[실시간 통계 업데이트 스케줄러] 시작");
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 1. 진행 중인 설문 조회 (배치)
            List<Survey> activeSurveys = surveyRepository.findActiveSurveysByEndTimeAfter(now);
            
            if (activeSurveys.isEmpty()) {
                log.debug("[실시간 통계 업데이트] 진행 중인 설문 없음");
                return;
            }
            
            log.info("[실시간 통계 업데이트] 진행 중인 설문 수: {}", activeSurveys.size());
            
            // 2. 설문 ID 리스트 추출
            List<Long> surveyIds = activeSurveys.stream()
                    .map(Survey::getId)
                    .collect(Collectors.toList());
            
            // 3. 배치 처리: 여러 설문의 응답 수를 한 번에 조회
            List<Object[]> responseCounts = responseRepository.countDistinctResponseSessionsBySurveyIds(surveyIds);
            
            // 4. 설문 ID별 응답 수 매핑
            Map<Long, Long> responseCountMap = responseCounts.stream()
                    .collect(Collectors.toMap(
                            arr -> (Long) arr[0],
                            arr -> ((Number) arr[1]).longValue()
                    ));
            
            // 5. 최소 응답 수 1개 이상인 설문만 필터링
            List<Long> targetSurveyIds = surveyIds.stream()
                    .filter(surveyId -> {
                        Long count = responseCountMap.getOrDefault(surveyId, 0L);
                        return count >= 1;
                    })
                    .collect(Collectors.toList());
            
            if (targetSurveyIds.isEmpty()) {
                log.debug("[실시간 통계 업데이트] 응답이 있는 진행 중인 설문 없음");
                return;
            }
            
            log.info("[실시간 통계 업데이트] 처리 대상 설문 수: {}", targetSurveyIds.size());
            
            // 6. 배치 처리: 여러 설문의 질문을 한 번에 조회
            List<Question> allQuestions = questionRepository.findBySurveyIdIn(targetSurveyIds);
            
            // 7. 설문별로 질문 그룹화
            Map<Long, List<Question>> questionsBySurvey = allQuestions.stream()
                    .collect(Collectors.groupingBy(Question::getSurveyId));
            
            // 8. 각 설문에 대해 요약 리포트, 워드클라우드, 인사이트 업데이트 (비동기)
            targetSurveyIds.forEach(surveyId -> {
                List<Question> questions = questionsBySurvey.getOrDefault(surveyId, Collections.emptyList());
                
                if (questions.isEmpty()) {
                    log.debug("[실시간 통계 업데이트] 설문에 질문 없음: surveyId={}", surveyId);
                    return;
                }
                
                log.info("[실시간 통계 업데이트] 설문 처리 시작: surveyId={}, 질문 수={}", 
                        surveyId, questions.size());
                
                // 요약 리포트 업데이트 (비동기)
                summaryService.saveSummaryReportAsync(surveyId);
                
                // 워드클라우드 업데이트 (비동기)
                wordCloudService.saveWordCloudsForSurveyAsync(surveyId);
                
                // 인사이트 업데이트 (비동기)
                insightService.saveInsightsForSurveyAsync(surveyId);
                
                // 통계는 실시간 계산되므로 별도 저장 불필요
                // (현재 구조상 통계는 DB에 저장하지 않고 실시간 계산)
                log.debug("[실시간 통계 업데이트] 설문 처리 완료: surveyId={}", surveyId);
            });
            
            log.info("[실시간 통계 업데이트 스케줄러] 완료 - 처리된 설문 수: {}", targetSurveyIds.size());
            
        } catch (Exception e) {
            log.error("[실시간 통계 업데이트 스케줄러] 오류 발생", e);
        }
    }

    /**
     * 요약 리포트가 없으면 비동기로 생성
     *
     * @param surveyId 설문 ID
     */
    private void generateSummaryReportIfNotExists(Long surveyId) {
        // 이미 리포트가 생성되어 있는지 확인
//        boolean exists = insightReportRepository.existsBySurveyId(surveyId);
        
//        if (!exists) {
            log.info("설문 종료 감지 - 요약 리포트 생성 시작: surveyId={}", surveyId);
            // 비동기로 요약 리포트 생성 및 저장
            summaryService.saveSummaryReportAsync(surveyId);
//        } else {
            log.debug("요약 리포트가 이미 존재함: surveyId={}", surveyId);
//        }
    }
}