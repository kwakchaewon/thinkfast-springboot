package com.example.thinkfast.service.survey;

import com.example.thinkfast.common.utils.HashUtil;
import com.example.thinkfast.domain.survey.Option;
import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.domain.survey.Response;
import com.example.thinkfast.domain.survey.SurveyResponseHistory;
import com.example.thinkfast.dto.survey.CreateResponseRequest;
import com.example.thinkfast.dto.survey.PaginationDto;
import com.example.thinkfast.dto.survey.QuestionResponsesResponseDto;
import com.example.thinkfast.dto.survey.ResponseItemDto;
import com.example.thinkfast.repository.auth.UserRepository;
import com.example.thinkfast.repository.survey.OptionRepository;
import com.example.thinkfast.repository.survey.QuestionRepository;
import com.example.thinkfast.repository.survey.ResponseRepository;
import com.example.thinkfast.repository.survey.SurveyResponseHistoryRepository;
import com.example.thinkfast.security.UserDetailImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseService {
    private final ResponseRepository responseRepository;
    private final UserRepository userRepository;
    private final SurveyResponseHistoryRepository surveyResponseHistoryRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final SurveyService surveyService;

    @Transactional
    public void createResponse(UserDetailImpl userDetail, Long surveyId, String ipAddress, CreateResponseRequest createResponseRequest){
        String responseSessionId = getRandomUuid();
        LocalDateTime now = LocalDateTime.now();

        for (CreateResponseRequest.CreateResponseDto createResponseDto : createResponseRequest.getAnswers()){
            Response response = Response.builder()
                    .responseSessionId(responseSessionId)
                    .questionId(createResponseDto.getQuestionId())
                    .optionId(createResponseDto.getOptionId())
                    .subjectiveContent(createResponseDto.getContent())
                    .questionType(createResponseDto.getType().toString())
                    .createdAt(now)
                    .build();
            Response result =  responseRepository.save(response);
        }

        String deviceId = createResponseRequest.getClientInfo() != null 
            ? createResponseRequest.getClientInfo().getDeviceId() 
            : null;
        this.createSurveyResponseHistory(surveyId, ipAddress, deviceId);

        // 응답 등록 후 설문 종료 여부 확인 및 리포트 업데이트
        // 설문이 종료되었다면 summary, insight, statistics, wordcloud가 업데이트됨
        surveyService.checkAndUpdateExpiredSurveyReports(surveyId);
    }

    public void createSurveyResponseHistory(Long surveyId, String ipAddress, String deviceId){
        log.info("[응답 이력 저장 시작] surveyId={}, deviceId={}, ipAddress={}", 
            surveyId,
            deviceId != null ? (deviceId.length() > 20 ? deviceId.substring(0, 20) + "..." : deviceId) : "null",
            ipAddress != null ? ipAddress : "null");
        
        // deviceId가 null이거나 빈 값이면 기본값 사용 (DEVICE_ID_HASH는 nullable=false이므로)
        // 단, 기본값을 사용하면 중복 체크에서 제외됨 (SurveyService에서 null 체크)
        boolean isDeviceIdEmpty = (deviceId == null || deviceId.trim().isEmpty());
        String deviceIdHash;
        if (isDeviceIdEmpty) {
            String defaultDeviceId = "UNKNOWN_DEVICE_" + surveyId + "_" + System.currentTimeMillis();
            deviceIdHash = HashUtil.encodeSha256(defaultDeviceId);
            log.warn("[응답 이력 저장] surveyId={}, deviceId가 null/빈 값이어서 기본값 사용: {}", surveyId, defaultDeviceId);
        } else {
            deviceIdHash = HashUtil.encodeSha256(deviceId);
        }
        
        // ipAddress는 nullable이므로 null 허용
        boolean isIpAddressEmpty = (ipAddress == null || ipAddress.trim().isEmpty());
        String ipAddressHash;
        if (isIpAddressEmpty) {
            ipAddressHash = null;
            log.warn("[응답 이력 저장] surveyId={}, ipAddress가 null/빈 값이어서 null로 저장", surveyId);
        } else {
            ipAddressHash = HashUtil.encodeSha256(ipAddress);
        }

        SurveyResponseHistory surveyResponseHistory = SurveyResponseHistory.builder()
                .surveyId(surveyId)
                .deviceIdHash(deviceIdHash)
                .ipAddressHash(ipAddressHash)
                .build();
        surveyResponseHistoryRepository.save(surveyResponseHistory);
        
        log.info("[응답 이력 저장 완료] surveyId={}, deviceIdHash={}, ipAddressHash={}", 
            surveyId, deviceIdHash, ipAddressHash != null ? ipAddressHash : "null");
    }

    public String getRandomUuid(){
        return UUID.randomUUID().toString();
    }

    /**
     * 질문별 전체 응답 조회 (페이징 지원)
     * 
     * @param questionId 질문 ID
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지당 응답 수
     * @return 질문별 응답 데이터 (페이징 정보 포함)
     */
    @Transactional(readOnly = true)
    public QuestionResponsesResponseDto getQuestionResponses(Long questionId, int page, int size) {
        // 1. 질문 조회
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다."));
        
        // 2. 전체 응답 수 조회
        long totalCount = responseRepository.countByQuestionId(questionId);
        
        // 3. 페이징 처리
        int totalPages = (int) Math.ceil((double) totalCount / size);
        Pageable pageable = PageRequest.of(page - 1, size); // Spring Data는 0부터 시작
        
        // 4. 응답 조회
        List<Response> responses = responseRepository.findByQuestionIdOrderByCreatedAtDesc(questionId, pageable);
        
        // 5. 옵션 맵 조회 (객관식 질문인 경우)
        final Map<Long, String> optionMap;
        if (question.getType() == Question.QuestionType.MULTIPLE_CHOICE) {
            List<Option> options = optionRepository.findByQuestionIdOrderByIdAsc(questionId);
            optionMap = options.stream()
                    .collect(Collectors.toMap(Option::getId, Option::getContent));
        } else {
            optionMap = null;
        }
        
        // 6. DTO 변환
        List<ResponseItemDto> responseItems = responses.stream()
                .map(response -> convertToResponseItemDto(response, question.getType(), optionMap))
                .collect(Collectors.toList());
        
        // 7. 페이징 정보 생성
        PaginationDto pagination = new PaginationDto(page, size, totalPages, totalCount);
        
        // 8. 응답 DTO 생성
        QuestionResponsesResponseDto responseDto = new QuestionResponsesResponseDto();
        responseDto.setQuestionId(questionId);
        responseDto.setType(question.getType());
        responseDto.setResponses(responseItems);
        responseDto.setPagination(pagination);
        
        return responseDto;
    }
    
    /**
     * Response 엔티티를 ResponseItemDto로 변환
     * 질문 타입에 따라 content를 적절히 변환
     * 참고: 척도형(SCALE)은 현재 지원하지 않음
     */
    private ResponseItemDto convertToResponseItemDto(Response response, Question.QuestionType questionType, Map<Long, String> optionMap) {
        String content = null;
        
        switch (questionType) {
            case MULTIPLE_CHOICE:
                // 객관식: 옵션 내용 조회
                if (response.getOptionId() != null && optionMap != null) {
                    content = optionMap.get(response.getOptionId());
                }
                break;
            case SUBJECTIVE:
                // 주관식: 입력 텍스트 그대로
                content = response.getSubjectiveContent();
                break;
            // 척도형(SCALE)은 현재 지원하지 않음
            // case SCALE:
            //     // 척도형: 점수를 문자열로 변환
            //     if (response.getScaleValue() != null) {
            //         content = String.valueOf(response.getScaleValue());
            //     }
            //     break;
            default:
                log.warn("지원하지 않는 질문 타입: {}", questionType);
                break;
        }
        
        return new ResponseItemDto(
                response.getId(),
                content,
                response.getCreatedAt()
        );
    }

}
