package com.example.thinkfast.repository.survey;

import com.example.thinkfast.domain.survey.SurveyResponseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyResponseHistoryRepository extends JpaRepository<SurveyResponseHistory, Long> {
    boolean existsBySurveyIdAndDeviceIdHash(Long surveyId, String deviceIdHash);
    boolean existsBySurveyIdAndIpAddressHash(Long surveyId, String ipAddressHash);
}
