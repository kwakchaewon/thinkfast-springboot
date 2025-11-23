package com.example.thinkfast.repository.survey;

import com.example.thinkfast.domain.survey.SurveyResponseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyResponseHistoryRepository extends JpaRepository<SurveyResponseHistory, Long> {
    boolean existsBySurveyIdAndDeviceIdHash(Long surveyId, String deviceIdHash);
    boolean existsBySurveyIdAndIpAddressHash(Long surveyId, String ipAddressHash);
    // deviceId + IP 조합으로 중복 체크 (둘 다 같아야 중복으로 판단)
    boolean existsBySurveyIdAndDeviceIdHashAndIpAddressHash(Long surveyId, String deviceIdHash, String ipAddressHash);
}
