package com.example.thinkfast.scheduler;

import com.example.thinkfast.domain.survey.Survey;
import com.example.thinkfast.repository.survey.SurveyRepository;
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
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkSurveyStatus() {
        log.info("[SCHEDULER] SURVEY CHECK");
        try {
            // 현재 시간
            LocalDateTime now = LocalDateTime.now();
            
            // 종료 시간이 지난 활성화된 설문 조회
            List<Survey> expiredSurveys = surveyRepository.findByIsActiveTrueAndEndDateBefore(now);            
            
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
        } catch (Exception e) {
            log.error("스케줄러 실행 중 오류 발생", e);
        }
    }
}