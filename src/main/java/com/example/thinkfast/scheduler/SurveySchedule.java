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
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
        String jobId = "job-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-expired";
        long startTime = System.currentTimeMillis();
        int processedCount = 0;
        AtomicInteger failedCount = new AtomicInteger(0);

        try {
            MDC.put("log_type", "scheduler");
            MDC.put("scheduler.job_name", "updateExpiredSurvey");
            MDC.put("scheduler.job_id", jobId);

            log.info("Scheduler job started: updateExpiredSurvey (jobId: {})", jobId);

            // 현재 시간
            LocalDateTime now = LocalDateTime.now();
            
            // 종료 시간이 지난 활성화된 설문 조회
            List<Survey> expiredSurveys = surveyRepository.findByIsActiveTrueAndEndTimeBefore(now);
            
            // 설문 비활성화 처리
            expiredSurveys.forEach(survey -> {
                try {
                    survey.setIsActive(false);
                    log.debug("Survey deactivated: ID={}, title={}, endDate={}", 
                        survey.getId(), survey.getTitle(), survey.getEndTime().toLocalDate());
                } catch (Exception e) {
                    log.warn("Failed to deactivate survey: ID={}", survey.getId(), e);
                }
            });
            
            // 변경사항 저장
            surveyRepository.saveAll(expiredSurveys);
            processedCount = expiredSurveys.size();

            expiredSurveys.forEach(survey -> {
                try {
                    redisPublisher.sendAlarm(survey.getId(), "SURVEY_EXPIRED");
                    
                    // 설문 종료 후 요약 리포트 비동기 생성
                    generateSummaryReportIfNotExists(survey.getId());
                    
                    // 설문 종료 후 워드클라우드 비동기 생성
                    wordCloudService.saveWordCloudsForSurveyAsync(survey.getId());
                    
                    // 설문 종료 후 인사이트 텍스트 비동기 생성
                    insightService.saveInsightsForSurveyAsync(survey.getId());
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                    log.warn("Failed to process expired survey: ID={}", survey.getId(), e);
                }
            });

            long duration = System.currentTimeMillis() - startTime;
            int failed = failedCount.get();
            MDC.put("scheduler.execution_time_ms", String.valueOf(duration));
            MDC.put("scheduler.processed_count", String.valueOf(processedCount));
            MDC.put("scheduler.failed_count", String.valueOf(failed));
            MDC.put("scheduler.status", failed > 0 ? "partial" : "success");

            log.info("Scheduler job completed: updateExpiredSurvey (jobId: {}, duration: {}ms, processed: {}, failed: {})", 
                    jobId, duration, processedCount, failed);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            int failed = failedCount.incrementAndGet();
            MDC.put("scheduler.execution_time_ms", String.valueOf(duration));
            MDC.put("scheduler.processed_count", String.valueOf(processedCount));
            MDC.put("scheduler.failed_count", String.valueOf(failed));
            MDC.put("scheduler.status", "failure");
            MDC.put("scheduler.error_message", e.getMessage());

            log.error("Scheduler job failed: updateExpiredSurvey (jobId: {}, duration: {}ms)", 
                    jobId, duration, e);
        } finally {
            // MDC 정리
            MDC.remove("log_type");
            MDC.remove("scheduler.job_name");
            MDC.remove("scheduler.job_id");
            MDC.remove("scheduler.execution_time_ms");
            MDC.remove("scheduler.processed_count");
            MDC.remove("scheduler.failed_count");
            MDC.remove("scheduler.status");
            MDC.remove("scheduler.error_message");
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
        String jobId = "job-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-reports";
        long startTime = System.currentTimeMillis();
        int processedCount = 0;
        AtomicInteger failedCount = new AtomicInteger(0);

        try {
            MDC.put("log_type", "scheduler");
            MDC.put("scheduler.job_name", "updateActiveSurveyReports");
            MDC.put("scheduler.job_id", jobId);

            log.info("Scheduler job started: updateActiveSurveyReports (jobId: {})", jobId);

            LocalDateTime now = LocalDateTime.now();
            
            // 1. 진행 중인 설문 조회 (배치)
            List<Survey> activeSurveys = surveyRepository.findActiveSurveysByEndTimeAfter(now);
            
            if (activeSurveys.isEmpty()) {
                log.debug("No active surveys found for update");
                return;
            }
            
            log.debug("Active surveys found: {}", activeSurveys.size());
            
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
                log.debug("No active surveys with responses found");
                return;
            }
            
            log.debug("Target surveys for update: {}", targetSurveyIds.size());
            
            // 6. 배치 처리: 여러 설문의 질문을 한 번에 조회
            List<Question> allQuestions = questionRepository.findBySurveyIdIn(targetSurveyIds);
            
            // 7. 설문별로 질문 그룹화
            Map<Long, List<Question>> questionsBySurvey = allQuestions.stream()
                    .collect(Collectors.groupingBy(Question::getSurveyId));
            
            // 8. 각 설문에 대해 요약 리포트, 워드클라우드, 인사이트 업데이트 (비동기)
            for (Long surveyId : targetSurveyIds) {
                try {
                    List<Question> questions = questionsBySurvey.getOrDefault(surveyId, Collections.emptyList());
                    
                    if (questions.isEmpty()) {
                        log.debug("Survey has no questions: surveyId={}", surveyId);
                        continue;
                    }
                    
                    // 요약 리포트 업데이트 (비동기)
                    summaryService.saveSummaryReportAsync(surveyId);
                    
                    // 워드클라우드 업데이트 (비동기)
                    wordCloudService.saveWordCloudsForSurveyAsync(surveyId);
                    
                    // 인사이트 업데이트 (비동기)
                    insightService.saveInsightsForSurveyAsync(surveyId);
                    
                    processedCount++;
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                    log.warn("Failed to update reports for survey: surveyId={}", surveyId, e);
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            int failed = failedCount.get();
            MDC.put("scheduler.execution_time_ms", String.valueOf(duration));
            MDC.put("scheduler.processed_count", String.valueOf(processedCount));
            MDC.put("scheduler.failed_count", String.valueOf(failed));
            MDC.put("scheduler.status", failed > 0 ? "partial" : "success");

            log.info("Scheduler job completed: updateActiveSurveyReports (jobId: {}, duration: {}ms, processed: {}, failed: {})", 
                    jobId, duration, processedCount, failed);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            int failed = failedCount.incrementAndGet();
            MDC.put("scheduler.execution_time_ms", String.valueOf(duration));
            MDC.put("scheduler.processed_count", String.valueOf(processedCount));
            MDC.put("scheduler.failed_count", String.valueOf(failed));
            MDC.put("scheduler.status", "failure");
            MDC.put("scheduler.error_message", e.getMessage());

            log.error("Scheduler job failed: updateActiveSurveyReports (jobId: {}, duration: {}ms)", 
                    jobId, duration, e);
        } finally {
            // MDC 정리
            MDC.remove("log_type");
            MDC.remove("scheduler.job_name");
            MDC.remove("scheduler.job_id");
            MDC.remove("scheduler.execution_time_ms");
            MDC.remove("scheduler.processed_count");
            MDC.remove("scheduler.failed_count");
            MDC.remove("scheduler.status");
            MDC.remove("scheduler.error_message");
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