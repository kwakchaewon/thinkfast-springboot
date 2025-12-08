# 설문 요약 리포트 및 응답 기능 개선 방안

## 0. 설문 결과 생성 및 수정 시점 정리

### 0.1 설문 요약 리포트 (Summary Report)

#### 생성 시점
1. **설문 종료 시**: 스케줄러(1분마다) 또는 응답 등록 후 자동 생성
2. **진행 중인 설문**: 스케줄러(1분마다)에서 응답 1개 이상인 설문 대상 실시간 업데이트 (배치 처리)
3. **API 조회**: `GET /survey/{id}/summary` - DB 저장된 리포트만 조회 (실시간 생성 없음)

#### 수정 시점
- 설문 종료 시마다 업데이트
- 진행 중인 설문 1분마다 자동 업데이트

#### 저장 위치
- **테이블**: `INSIGHT_REPORTS` (`summaryText`, `keywords` - JSON 직렬화)

---

### 0.2 워드 클라우드 (Word Cloud)

#### 생성 시점
1. **설문 종료 시**: 스케줄러 또는 응답 등록 후 - 모든 주관식 질문에 대해 생성
2. **진행 중인 설문**: 스케줄러(1분마다)에서 실시간 업데이트 (배치 처리)
3. **API 조회**: `GET /survey/{surveyId}/questions/{questionId}/wordcloud` - DB 저장된 데이터만 조회

#### 수정 시점
- 설문 종료 시마다 업데이트
- 진행 중인 설문 1분마다 자동 업데이트

#### 저장 위치
- **테이블**: `WORD_CLOUDS` (`wordCloudData` - JSON 직렬화)

#### 생성 방식 ⭐
- **Gemini API 기반 키워드 추출**: 질문 내용과 응답 샘플을 분석하여 질문과 연관된 키워드 추출
- **폴백 전략**: Gemini API 실패 시 Java 기반 키워드 추출로 자동 전환
- **설정**: `llm.wordcloud.enabled` (기본값: true)로 활성화/비활성화 가능

---

### 0.3 인사이트 (Insight)

#### 생성 시점
1. **설문 종료 시**: 스케줄러 또는 응답 등록 후 - 모든 객관식/주관식 질문에 대해 생성
2. **진행 중인 설문**: 스케줄러(1분마다)에서 실시간 업데이트 (배치 처리)
3. **API 조회**: `GET /survey/{surveyId}/questions/{questionId}/insight` - DB 저장된 데이터만 조회
4. ⭐ **향후**: LLM 기반 동적 분석으로 전환 예정 (질문 유형에 따라 자동 분석)

#### 수정 시점
- 설문 종료 시마다 업데이트
- 진행 중인 설문 1분마다 자동 업데이트

#### 저장 위치
- **테이블**: `QUESTION_INSIGHTS` (`insightText`)

---

### 0.4 통계 (Statistics)

#### 생성 시점
- **API 조회 시마다 실시간 계산**: `GET /survey/{surveyId}/questions/{questionId}/statistics`
- DB 저장 없음, 항상 실시간 계산

---

### 0.5 시점 요약

| 결과 타입 | 생성 시점 | 수정 시점 | DB 저장 | 실시간 생성 |
|---------|---------|---------|--------|-----------|
| **요약 리포트** | 1. 설문 종료 시 (스케줄러)<br>2. 설문 종료 시 (응답 등록 후)<br>3. **진행 중인 설문 실시간 업데이트 (스케줄러)** ⭐<br>4. API 조회 시 (빈 데이터 반환) | 설문 종료 시마다 업데이트<br>**진행 중인 설문 1분마다 자동 업데이트** ⭐ | ✅ `INSIGHT_REPORTS` | ❌ |
| **워드 클라우드** | 1. 설문 종료 시 (스케줄러)<br>2. 설문 종료 시 (응답 등록 후)<br>3. **진행 중인 설문 실시간 업데이트 (스케줄러)** ⭐<br>4. API 조회 시 (빈 데이터 반환) | 설문 종료 시마다 업데이트<br>**진행 중인 설문 1분마다 자동 업데이트** ⭐ | ✅ `WORD_CLOUDS` | ❌ |
| **인사이트** | 1. 설문 종료 시 (스케줄러)<br>2. 설문 종료 시 (응답 등록 후)<br>3. **진행 중인 설문 실시간 업데이트 (스케줄러)** ⭐<br>4. API 조회 시 (null 반환) | 설문 종료 시마다 업데이트<br>**진행 중인 설문 1분마다 자동 업데이트** ⭐ | ✅ `QUESTION_INSIGHTS` | ❌ |
| **통계** | API 조회 시마다 | 없음 | ❌ | ✅ |

---

### 0.6 주요 구현 위치

- **스케줄러**: `SurveySchedule.java` - 종료된 설문 처리, 진행 중인 설문 실시간 업데이트
- **응답 처리**: `ResponseService.java` - 응답 저장 및 설문 종료 체크
- **리포트 서비스**: `SummaryService.java`, `WordCloudService.java`, `InsightService.java`, `SurveyStatisticsService.java`
- **배치 처리**: `SurveyRepository.findActiveSurveysByEndTimeAfter()`, `QuestionRepository.findBySurveyIdIn()`, `ResponseRepository.countDistinctResponseSessionsBySurveyIds()`

---

## 1. 현재 기능 개요

### 1.1 설문 요약 리포트 기능

#### 1.1.1 주요 기능
- **API**: `GET /survey/{id}/summary` - 설문 소유자만 조회 가능
- **조회 방식**: DB 저장된 리포트 우선 조회 (실시간 생성 없음)

#### 1.1.2 리포트 구성 요소
1. **mainPosition** (String | null): 첫 번째 객관식 질문에서 비율이 가장 높은 옵션
2. **mainPositionPercent** (Double | null): mainPosition 옵션의 선택 비율 (%)
3. **insights** (List<String>) ⭐ **변경 예정**: 주관식 질문에서 동적으로 추출한 인사이트 리스트 (최대 5개)
   - 현재: 템플릿 기반 키워드 추출 (제거 예정)
   - 향후: LLM 기반 문맥 이해 및 자연어 생성으로 동적 분석

#### 1.1.3 리포트 저장 구조
- **저장 위치**: `INSIGHT_REPORTS` 테이블 (`summaryText`, `keywords` - JSON 직렬화)
- **비동기 저장**: `@Async`를 통한 백그라운드 저장

### 1.2 설문 응답 기능

#### 1.2.1 주요 기능
- **API**: `POST /survey/{surveyId}/responses` - 비회원, 무인증 참여 가능
- **응답 저장**: 각 질문별 응답을 `Response` 테이블에 저장
- **세션 관리**: `responseSessionId` (UUID)로 한 설문의 응답들을 그룹화

#### 1.2.2 중복 응답 방지
- **체크 방식**: DeviceId + IP Address 조합 (SHA256 해시화)
- **이력 저장**: `SURVEY_RESPONSE_HISTORY` 테이블
- **체크 로직**: DeviceId + IP 모두 존재 시 조합으로 체크, 하나만 존재 시 해당 값으로만 체크, 둘 다 없으면 허용

#### 1.2.3 응답 저장 프로세스
1. 설문 활성화 여부 확인 → 2. 중복 응답 체크 → 3. 각 질문별 응답 저장 (객관식: `optionId`, 주관식: `subjectiveContent`) → 4. 응답 이력 저장 → 5. Redis 실시간 알람 발송

#### 1.2.4 응답 조회 기능
- **API**: `GET /survey/{surveyId}/questions/{questionId}/responses`
- **페이징**: page, size (기본값: page=1, size=10, 최대 size=100)
- **정렬**: 생성일시 내림차순
- **권한**: 공개 설문은 인증 없이 접근, 비공개 설문은 소유자만 접근

---

## 2. 현재 구조의 한계점 및 개선 필요 사항

### 2.1 설문 요약 리포트 기능

#### 2.1.1 기능적 한계
1. **제한적인 리포트 내용**: 첫 번째 객관식 질문만 분석, 다른 질문 통계 부재, 척도형 질문 미지원
2. **응답 분석 정확도** ⭐ **변경 예정**: 템플릿 기반, 문맥 이해 부족, 키워드 중심 → LLM 기반 동적 분석으로 전환 예정
3. **성능**: ✅ 배치 처리 최적화 완료
4. **리포트 업데이트**: ✅ 실시간 업데이트 스케줄러 구현 완료, 버전 관리 부재

#### 2.1.2 기술적 한계
1. **캐싱 전략 부재**: 리포트 조회 시 캐싱 미적용
2. **비동기 처리**: ✅ `@Async` 지원, 스케줄러 기반 자동 생성 및 실시간 업데이트 구현 완료
3. **에러 처리**: 리포트 생성 실패 시 피드백 부족, 부분 실패 처리 방식 미정의

### 2.2 설문 응답 기능

#### 2.2.1 기능적 한계
1. **중복 응답 방지**: DeviceId/IP 없을 시 체크 불가, VPN/프록시 환경 불안정, 시간 기반 제한 없음
2. **응답 데이터 검증**: 필수 질문 체크 부재, 응답 형식 검증 부재, 악의적 응답 필터링 부재
3. **응답자 정보**: 식별 정보 부족, 메타데이터 부족, 패턴 분석 불가

#### 2.2.2 기술적 한계
1. **성능**: 대량 응답 시 순차적 저장, Bulk Insert 미적용
2. **트랜잭션**: 실패 시 부분 저장 가능성, 롤백 전략 미정의
3. **알람**: Redis만 발송, DB 저장 없음, 읽음/안읽음 상태 관리 부재
4. **응답 조회**: 대량 조회 시 성능 이슈 가능성

---

## 3. 개선 방안

### 3.1 설문 요약 리포트 기능 개선

#### 3.1.1 리포트 내용 확장
- [ ] **다중 질문 분석**: 모든 객관식 질문 통계, 질문별 주요 선택지 및 비율, 질문 간 상관관계 분석
- [x] **척도형 질문 지원**: 평균값, 중앙값, 분포 통계, 시각화 데이터 (현재 지원하지 않는 기능)
- [ ] **응답 통계 정보**: 총 응답 수, 응답률, 응답 시간대 분포, 응답 소요 시간 분석

#### 3.1.2 LLM 기반 동적 분석 및 인사이트 추출 ⭐ **우선순위 높음**
- [x] **LLM 기반 문맥 이해 및 동적 자연어 생성** ⭐ **우선순위 높음**
  - **핵심 원칙**: ❌ "개선사항" 같은 고정된 키워드 사용 안 함, ✅ 질문 유형에 따라 동적 분석, ✅ 자연어 생성
  - **기술 스택**: LLM API (OpenAI GPT-4, Anthropic Claude 등)
  - **구현 방안**:
    - 질문 내용과 응답 내용을 함께 LLM에 전달하여 문맥 종합 분석
    - 질문 유형별 자동 분석 방식 선택 (피드백 요청 → 의견/제안, 경험 조사 → 경험/인사이트, 의견 조사 → 의견/트렌드)
    - 템플릿 기반이 아닌 자연어 생성으로 맞춤형 인사이트 도출
  - **고려사항**: LLM API 비용 관리, 비동기 처리, API 실패 시 폴백 전략, 프롬프트 엔지니어링 최적화

- [ ] **감성 분석 통합**: 응답의 긍정/부정 감성 분석, 우선순위 조정, 긴급 이슈 자동 식별
- [ ] **동적 카테고리 분류**: 질문 유형별 자동 분류, 우선순위 제안

#### 3.1.3 성능 최적화
- [ ] **캐싱 전략 도입**
  - Redis를 활용한 리포트 캐싱
  - 캐시 TTL 설정 (예: 1시간)
  - 캐시 무효화 전략 (새 응답 추가 시)

- [x] **리포트 생성 스케줄링** ✅ **완료**: 설문 종료 시 자동 생성, 진행 중인 설문 1분마다 자동 업데이트
- [x] **배치 처리 최적화** ✅ **완료**: 여러 설문의 질문/응답 수를 한 번에 조회
- [ ] **비동기 처리 강화**: 작업 큐 도입 (RabbitMQ/Kafka), 작업 상태 추적, 생성 상태 알림

#### 3.1.4 LLM 기반 동적 분석 구현 세부사항

**프롬프트 설계**: 질문 유형과 응답 내용을 함께 LLM에 전달하여 문맥 기반 인사이트 생성 (고정 키워드/템플릿 사용 안 함)

**구현 방안**:
- 서비스 클래스: `LLMInsightExtractionService` (신규) 또는 `InsightExtractionService` (리팩토링)
- 기존 서비스: `ImprovementExtractionService` → 제거 예정
- 성능 최적화: 배치 처리, 캐싱, 토큰 사용량 모니터링
- 설정 관리: `application.yml`에 LLM API 키 및 설정, 기능 토글
- 에러 처리: LLM API 실패 시 기존 템플릿 방식으로 폴백

**예상 효과**: 정확도 향상, 가독성 향상, 맞춤형 분석, 유연성, 확장성

**고려사항**: 비용 관리, 지연 시간 최적화, 안정성 (폴백 전략), 프라이버시 (사용자 동의 검토)

#### 3.1.5 리포트 버전 관리
- [ ] **리포트 버전 관리**
  - 리포트 버전 추적
  - 변경 이력 기록
  - 이전 버전 조회 가능

- [ ] **리포트 비교 기능**
  - 시점별 리포트 비교
  - 변경 사항 하이라이트

### 3.2 설문 응답 기능 개선

#### 3.2.1 중복 응답 방지 강화
- [ ] **다중 식별자 조합**: DeviceId + IP + User-Agent 해시, 쿠키 기반 세션 ID, Fingerprinting (선택)
- [ ] **시간 기반 제한**: 설문 종료 시간 이후 응답 불가, 최소 응답 간격 설정, 응답 가능 시간대 설정
- [ ] **응답자 인증 옵션**: 선택적 이메일/SMS 인증, 소셜 로그인 연동

#### 3.2.2 응답 데이터 검증 강화
- [ ] **필수 질문 검증**: 질문별 필수 여부 설정, 미응답 시 에러 반환
- [ ] **응답 형식 검증**: 주관식 최대/최소 길이 제한, 특수 문자 필터링, 악의적 콘텐츠 탐지
- [ ] **응답 품질 검증**: 응답 시간 기반 이상치 탐지, 패턴 기반 이상치 탐지

#### 3.2.3 응답자 정보 확장
- [ ] **응답 메타데이터 수집**: 응답 시간대, 디바이스 정보, 응답 소요 시간, 응답 시작 시간
- [ ] **응답자 프로필 (선택적)**: 연령대, 성별, 지역 정보 (IP 기반), 응답 동기

#### 3.2.4 성능 최적화
- [ ] **Bulk Insert**: 여러 질문 응답을 한 번에 저장
- [ ] **비동기 처리**: 응답 저장 후 즉시 반환, 알람 발송 등 부가 작업 비동기 처리
- [ ] **데이터베이스 최적화**: 인덱스 추가 (surveyId, questionId, createdAt 등), 파티셔닝 고려

#### 3.2.5 알람 시스템 개선
- [ ] **알람 DB 저장**: 알람 이력 DB 저장, 읽음/안읽음 상태 관리, 알람 타입별 분류
- [ ] **알람 조회 API**: 사용자별 알람 목록 조회, 읽음 처리, 삭제 API
- [ ] **알람 설정**: 수신 설정 (이메일, 푸시 등), 빈도 설정 (즉시, 요약 등)

---

## 4. 우선순위별 구현 계획

### 4.1 Phase 1: 핵심 개선 (1-2주)

#### 설문 응답 기능
1. **필수 질문 검증**: Question 엔티티에 `isRequired` 필드 추가, 검증 로직 구현
2. **응답 데이터 검증**: 주관식 최대 길이 제한, 기본적인 응답 형식 검증
3. **알람 DB 저장 및 조회**: 알람 엔티티 생성, 저장 및 조회 API 구현

#### 설문 요약 리포트 기능
1. **캐싱 전략 도입**: Redis를 활용한 리포트 캐싱, 캐시 무효화 로직
2. **리포트 생성 스케줄링** ✅ **완료**: 설문 종료 시 자동 생성, 진행 중인 설문 실시간 업데이트, 배치 처리 최적화

### 4.2 Phase 2: 기능 확장 (2-3주)

#### 설문 응답 기능
1. **중복 응답 방지 강화**
   - 쿠키 기반 세션 ID 추가
   - 시간 기반 제한 추가

2. **응답 메타데이터 수집**
   - 응답 시간대, 디바이스 정보 등 수집
   - Response 엔티티 확장

3. **Bulk Insert 적용**
   - 응답 저장 성능 최적화

#### 설문 요약 리포트 기능
1. **리포트 내용 확장**
   - 다중 질문 분석 지원
   - 응답 통계 정보 추가

2. **LLM 기반 동적 분석 및 인사이트 추출** ⭐ **우선순위 높음**
   - LLM 기반 문맥 이해 및 동적 자연어 생성 (질문 유형별 맞춤 분석, "개선사항" 키워드 제거)
   - 동적 카테고리 분류 추가

### 4.3 Phase 3: 고급 기능 (3-4주)

#### 설문 응답 기능
1. **응답자 인증 옵션**: 선택적 이메일 인증, 응답 인증 API 구현
2. **응답 품질 검증**: 이상치 탐지 로직, 악의적 콘텐츠 탐지

#### 설문 요약 리포트 기능
1. **리포트 버전 관리**: 버전 추적, 비교 기능
2. **고급 분석 기능**: 질문 간 상관관계 분석, 시계열 분석, 예측 분석

---

## 5. 기술 스택 제안

### 5.1 캐싱
- **Redis**: 리포트 캐싱, 세션 관리

### 5.2 메시지 큐
- **RabbitMQ** 또는 **Apache Kafka**: 비동기 작업 처리

### 5.3 AI/ML 서비스
- **Google Gemini API** (gemini-2.0-flash): ✅ **구현 완료**
  - 요약 리포트 인사이트 생성 (문맥 이해 기반 자연어 생성)
  - 워드클라우드 키워드 추출 (질문과 연관된 키워드 추출)
  - 질문-응답 쌍 동적 분석
  - 질문 유형별 맞춤형 분석
- **OpenAI API** (GPT-4, GPT-3.5-turbo) 또는 **Anthropic Claude API**: 
  - 동적 인사이트 추출 (문맥 이해 기반 자연어 생성)
  - 질문-응답 쌍 동적 분석
  - 질문 유형별 맞춤형 분석
  - 감성 분석
- **한국어 NLP 라이브러리**: 키워드 추출, 형태소 분석 (KoNLPy 등)
  - LLM API 실패 시 폴백용

### 5.4 모니터링
- **Prometheus + Grafana**: 성능 모니터링
- **ELK Stack**: 로그 분석

---

## 6. 데이터베이스 스키마 변경 제안

### 6.1 설문 요약 리포트 관련

#### INSIGHT_REPORTS 테이블 확장
```sql
ALTER TABLE INSIGHT_REPORTS ADD COLUMN VERSION INT DEFAULT 1;
ALTER TABLE INSIGHT_REPORTS ADD COLUMN UPDATED_AT TIMESTAMP;
ALTER TABLE INSIGHT_REPORTS ADD COLUMN GENERATED_AT TIMESTAMP;
ALTER TABLE INSIGHT_REPORTS ADD COLUMN STATUS VARCHAR(20); -- 'GENERATING', 'COMPLETED', 'FAILED'
```

#### INSIGHT_REPORT_HISTORY 테이블 생성 (버전 관리용)
```sql
CREATE TABLE INSIGHT_REPORT_HISTORY (
    ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    SURVEY_ID BIGINT NOT NULL,
    VERSION INT NOT NULL,
    SUMMARY_TEXT TEXT,
    KEYWORDS TEXT,
    CREATED_AT TIMESTAMP NOT NULL,
    INDEX IDX_SURVEY_VERSION (SURVEY_ID, VERSION)
);
```

### 6.2 설문 응답 관련

#### RESPONSES 테이블 확장
```sql
ALTER TABLE RESPONSES ADD COLUMN RESPONSE_TIME_SECONDS INT;
ALTER TABLE RESPONSES ADD COLUMN DEVICE_INFO VARCHAR(255);
ALTER TABLE RESPONSES ADD COLUMN USER_AGENT VARCHAR(500);
```

#### QUESTIONS 테이블 확장
```sql
ALTER TABLE QUESTIONS ADD COLUMN IS_REQUIRED BOOLEAN DEFAULT FALSE;
ALTER TABLE QUESTIONS ADD COLUMN MAX_LENGTH INT; -- 주관식 질문 최대 길이
```

#### ALARMS 테이블 생성 (알람 관리)
```sql
CREATE TABLE ALARMS (
    ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    USER_ID BIGINT NOT NULL,
    SURVEY_ID BIGINT NOT NULL,
    TYPE VARCHAR(50) NOT NULL, -- 'SURVEY_RESPONSE', 'SURVEY_ENDED', etc.
    TITLE VARCHAR(255),
    MESSAGE TEXT,
    IS_READ BOOLEAN DEFAULT FALSE,
    CREATED_AT TIMESTAMP NOT NULL,
    INDEX IDX_USER_READ (USER_ID, IS_READ),
    INDEX IDX_SURVEY (SURVEY_ID)
);
```

#### SURVEY_RESPONSE_HISTORY 테이블 확장
```sql
ALTER TABLE SURVEY_RESPONSE_HISTORY ADD COLUMN SESSION_ID VARCHAR(255);
ALTER TABLE SURVEY_RESPONSE_HISTORY ADD COLUMN USER_AGENT_HASH VARCHAR(255);
CREATE INDEX IDX_SURVEY_SESSION ON SURVEY_RESPONSE_HISTORY(SURVEY_ID, SESSION_ID);
```

---

## 7. API 변경 제안

### 7.1 설문 요약 리포트 API

#### 리포트 상태 조회 API 추가
```
GET /survey/{id}/summary/status
```
- 리포트 생성 상태 조회 (생성 중, 완료, 실패)

#### 리포트 재생성 API 추가
```
POST /survey/{id}/summary/regenerate
```
- 리포트 강제 재생성

### 7.2 설문 응답 API

#### 응답 미리보기 API
```
GET /survey/{surveyId}/responses/preview
```
- 응답 전 최종 확인

#### 응답 수정 API (옵션)
```
PUT /survey/{surveyId}/responses/{responseSessionId}
```
- 응답 기간 내 응답 수정 가능

### 7.3 알람 API

#### 알람 목록 조회
```
GET /alarms?page=1&size=20&isRead=false
```

#### 알람 읽음 처리
```
PUT /alarms/{id}/read
```

#### 알람 삭제
```
DELETE /alarms/{id}
```

---

## 8. 모니터링 및 로깅

### 8.1 주요 메트릭
- 리포트 생성 시간
- 응답 저장 처리 시간
- 중복 응답 차단율
- 알람 발송 성공률

### 8.2 로깅 개선
- 구조화된 로그 형식 (JSON)
- 중요 이벤트 로깅 (응답 생성, 리포트 생성 등)
- 에러 상세 정보 로깅

---

## 9. 보안 고려사항

### 9.1 개인정보 보호
- IP 주소 해시화 (현재 구현됨)
- 디바이스 ID 해시화 (현재 구현됨)
- 응답 데이터 암호화 (선택적)

### 9.2 DDoS 방어
- Rate Limiting 적용
- CAPTCHA 도입 (의심스러운 요청 시)

### 9.3 데이터 무결성
- 응답 데이터 검증
- 트랜잭션 보장

---

## 10. 테스트 계획

### 10.1 단위 테스트
- 각 서비스 레이어 단위 테스트
- 엣지 케이스 테스트

### 10.2 통합 테스트
- API 엔드포인트 통합 테스트
- 데이터베이스 통합 테스트

### 10.3 성능 테스트
- 대량 응답 처리 테스트
- 리포트 생성 성능 테스트
- 동시 접속 부하 테스트

---

## 11. 문서화

### 11.1 API 문서
- Swagger/OpenAPI 문서 업데이트
- 에러 코드 명세

### 11.2 사용자 가이드
- 리포트 해석 가이드
- 응답 작성 가이드

---

## 12. 참고 자료

- **주요 파일**: `SurveyController.java`, `SummaryService.java`, `WordCloudService.java`, `InsightService.java`, `SurveyStatisticsService.java`
- **스케줄러**: `SurveySchedule.java` (실시간 업데이트)
- **Repository**: `SurveyRepository`, `QuestionRepository`, `ResponseRepository` (배치 처리 메서드)
- **향후 구현**: `LLMInsightExtractionService.java` (LLM 기반 동적 인사이트 추출)

---

## 13. 최근 변경사항 요약 (업데이트: 2025-12-10)

### ✅ 완료된 개선사항

1. **실시간 통계 업데이트 스케줄러**: 진행 중인 설문 1분마다 요약 리포트, 워드클라우드, 인사이트 자동 업데이트
2. **배치 처리 최적화**: 여러 설문의 질문/응답 수를 한 번에 조회하여 DB 쿼리 수 대폭 감소
3. **스케줄러 구조 개선**: 설문 종료 시 + 진행 중인 설문 실시간 업데이트
4. **워드클라우드 Gemini API 기반 전환** ⭐: Java 기반 키워드 추출에서 Gemini API 기반으로 전환
   - 질문 내용과 응답 샘플을 분석하여 질문과 연관된 키워드 추출
   - Gemini API 실패 시 Java 기반 폴백 자동 전환
   - `llm.wordcloud.enabled` 설정으로 활성화/비활성화 가능

### 📝 향후 개선 계획

1. **LLM 기반 동적 분석** ⭐ **우선순위 높음**: 질문 유형별 맞춤 분석, 자연어 생성, "개선사항" 키워드 제거
2. **캐싱 전략 도입**: Redis를 활용한 리포트 캐싱
3. **리포트 버전 관리**: 변경 이력 추적 및 비교 기능
4. **작업 큐 도입**: RabbitMQ/Kafka를 통한 비동기 작업 처리 강화
5. **증분 업데이트**: 전체 재생성 대신 변경된 부분만 업데이트

