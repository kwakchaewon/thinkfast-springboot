package com.example.thinkfast.scheduler;

import com.example.thinkfast.domain.survey.Survey;
import com.example.thinkfast.realtime.RedisPublisher;
import com.example.thinkfast.repository.ai.InsightReportRepository;
import com.example.thinkfast.repository.survey.SurveyRepository;
import com.example.thinkfast.service.ai.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SurveySchedule {

    private final SurveyRepository surveyRepository;
    private final RedisPublisher redisPublisher;
    private final SummaryService summaryService;
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
            });
        } catch (Exception e) {
            log.error("스케줄러 실행 중 오류 발생", e);
        }
    }

    /**
     * 요약 리포트가 없으면 비동기로 생성
     *
     * @param surveyId 설문 ID
     */
    private void generateSummaryReportIfNotExists(Long surveyId) {
        // 이미 리포트가 생성되어 있는지 확인
        boolean exists = insightReportRepository.existsBySurveyId(surveyId);
        
        if (!exists) {
            log.info("설문 종료 감지 - 요약 리포트 생성 시작: surveyId={}", surveyId);
            // 비동기로 요약 리포트 생성 및 저장
            summaryService.saveSummaryReportAsync(surveyId);
        } else {
            log.debug("요약 리포트가 이미 존재함: surveyId={}", surveyId);
        }
    }
}