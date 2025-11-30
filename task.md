# 설문 요약 리포트 및 응답 기능 개선 방안

## 0. 설문 결과 생성 및 수정 시점 정리

### 0.1 설문 요약 리포트 (Summary Report)

#### 생성 시점
1. **설문 종료 시 (스케줄러)**
   - 1분마다 실행되는 스케줄러에서 종료 시간이 지난 활성화된 설문을 찾아 비활성화 처리
   - 종료된 설문에 대해 요약 리포트 비동기 생성

2. **설문 종료 시 (응답 등록 후)**
   - 응답 저장 후 설문 종료 여부 확인
   - 설문이 종료되었으면 요약 리포트 비동기 생성

3. **진행 중인 설문 실시간 업데이트 (스케줄러)** 
   - 1분마다 실행되는 스케줄러에서 진행 중인 설문 처리
   - 진행 중인 설문 (isActive = true, isDeleted = false, endTime > 현재 시간) 중 응답이 1개 이상인 설문 대상
   - 배치 처리로 성능 최적화 (여러 설문의 질문/응답 수를 한 번에 조회)
   - 요약 리포트 비동기 업데이트

4. **API 조회 시 (실시간 생성 없음)**
   - `GET /survey/{id}/summary` API 호출 시
   - DB에 저장된 리포트만 조회, 없으면 빈 데이터 반환 (실시간 생성 안 함)

#### 수정 시점
- **설문 종료 시마다 업데이트**
  - 기존 리포트가 있으면 업데이트, 없으면 생성
- **진행 중인 설문 실시간 업데이트** 
  - 1분마다 진행 중인 설문에 대해 자동 업데이트
  - 새로운 응답이 추가되면 최신 데이터로 리포트 갱신

#### 저장 위치
- **테이블**: `INSIGHT_REPORTS`
- **컬럼**: `summaryText` (JSON 직렬화), `keywords` (JSON 직렬화)

---

### 0.2 워드 클라우드 (Word Cloud)

#### 생성 시점
1. **설문 종료 시 (스케줄러)**
   - 1분마다 실행되는 스케줄러에서 종료 시간이 지난 활성화된 설문을 찾아 비활성화 처리
   - 종료된 설문의 모든 주관식 질문에 대해 워드클라우드 비동기 생성

2. **설문 종료 시 (응답 등록 후)**
   - 응답 저장 후 설문 종료 여부 확인
   - 설문이 종료되었으면 워드클라우드 비동기 생성

3. **진행 중인 설문 실시간 업데이트 (스케줄러)** 
   - 1분마다 실행되는 스케줄러에서 진행 중인 설문 처리
   - 진행 중인 설문 중 응답이 1개 이상인 설문 대상
   - 배치 처리로 성능 최적화
   - 워드클라우드 비동기 업데이트

4. **API 조회 시 (실시간 생성 없음)**
   - `GET /survey/{surveyId}/questions/{questionId}/wordcloud` API 호출 시
   - DB에 저장된 워드클라우드만 조회, 없으면 빈 데이터 반환 (실시간 생성 안 함)

#### 수정 시점
- **설문 종료 시마다 업데이트**
  - 기존 워드클라우드가 있으면 업데이트, 없으면 생성
- **진행 중인 설문 실시간 업데이트** 
  - 1분마다 진행 중인 설문에 대해 자동 업데이트
  - 새로운 응답이 추가되면 최신 데이터로 워드클라우드 갱신

#### 저장 위치
- **테이블**: `WORD_CLOUDS`
- **컬럼**: `wordCloudData` (JSON 직렬화된 WordCloudResponseDto)

---

### 0.3 인사이트 (Insight)

#### 생성 시점
1. **설문 종료 시 (스케줄러)**
   - 1분마다 실행되는 스케줄러에서 종료 시간이 지난 활성화된 설문을 찾아 비활성화 처리
   - 종료된 설문의 모든 객관식/주관식 질문에 대해 인사이트 비동기 생성
   - ⭐ **향후**: LLM 기반 동적 분석으로 전환 예정 (질문 유형에 따라 자동 분석)

2. **설문 종료 시 (응답 등록 후)**
   - 응답 저장 후 설문 종료 여부 확인
   - 설문이 종료되었으면 인사이트 비동기 생성

3. **진행 중인 설문 실시간 업데이트 (스케줄러)** 
   - 1분마다 실행되는 스케줄러에서 진행 중인 설문 처리
   - 진행 중인 설문 중 응답이 1개 이상인 설문 대상
   - 배치 처리로 성능 최적화
   - 인사이트 비동기 업데이트

4. **API 조회 시 (실시간 생성 없음)**
   - `GET /survey/{surveyId}/questions/{questionId}/insight` API 호출 시
   - DB에 저장된 인사이트만 조회, 없으면 null 반환 (실시간 생성 안 함)

#### 수정 시점
- **설문 종료 시마다 업데이트**
  - 기존 인사이트가 있으면 업데이트, 없으면 생성
- **진행 중인 설문 실시간 업데이트** 
  - 1분마다 진행 중인 설문에 대해 자동 업데이트
  - 새로운 응답이 추가되면 최신 데이터로 인사이트 갱신

#### 저장 위치
- **테이블**: `QUESTION_INSIGHTS`
- **컬럼**: `insightText` (String)
- ⭐ **향후**: LLM 기반 동적 분석 결과 저장 (질문 유형에 따라 자동으로 적절한 인사이트 생성)

---

### 0.4 통계 (Statistics)

#### 생성 시점
- **API 조회 시마다 실시간 계산**
  - `GET /survey/{surveyId}/questions/{questionId}/statistics` API 호출 시
  - DB 저장 없이 매번 실시간 계산

#### 수정 시점
- **없음** (항상 실시간 계산)

#### 저장 위치
- **DB 저장 안 함** (실시간 계산만 수행)

---

### 0.5 시점 요약

| 결과 타입 | 생성 시점 | 수정 시점 | DB 저장 | 실시간 생성 |
|---------|---------|---------|--------|-----------|
| **요약 리포트** | 1. 설문 종료 시 (스케줄러)<br>2. 설문 종료 시 (응답 등록 후)<br>3. **진행 중인 설문 실시간 업데이트 (스케줄러)** ⭐<br>4. API 조회 시 (빈 데이터 반환) | 설문 종료 시마다 업데이트<br>**진행 중인 설문 1분마다 자동 업데이트** ⭐ | ✅ `INSIGHT_REPORTS` | ❌ |
| **워드 클라우드** | 1. 설문 종료 시 (스케줄러)<br>2. 설문 종료 시 (응답 등록 후)<br>3. **진행 중인 설문 실시간 업데이트 (스케줄러)** ⭐<br>4. API 조회 시 (빈 데이터 반환) | 설문 종료 시마다 업데이트<br>**진행 중인 설문 1분마다 자동 업데이트** ⭐ | ✅ `WORD_CLOUDS` | ❌ |
| **인사이트** | 1. 설문 종료 시 (스케줄러)<br>2. 설문 종료 시 (응답 등록 후)<br>3. **진행 중인 설문 실시간 업데이트 (스케줄러)** ⭐<br>4. API 조회 시 (null 반환) | 설문 종료 시마다 업데이트<br>**진행 중인 설문 1분마다 자동 업데이트** ⭐ | ✅ `QUESTION_INSIGHTS` | ❌ |
| **통계** | API 조회 시마다 | 없음 | ❌ | ✅ |

---

### 0.6 주요 메서드 및 파일 위치

#### 스케줄러
- **파일**: `src/main/java/com/example/thinkfast/scheduler/SurveySchedule.java`
- **메서드**: `updateExpiredSurvey()` - 1분마다 실행, 종료된 설문 처리 및 리포트 생성
- **메서드**: `updateActiveSurveyReports()`  - 1분마다 실행, 진행 중인 설문의 리포트 실시간 업데이트
  - 배치 처리로 성능 최적화 (여러 설문의 질문/응답 수를 한 번에 조회)
  - 진행 중인 설문 (isActive = true, isDeleted = false, endTime > 현재 시간) 중 응답이 1개 이상인 설문만 처리

#### 응답 등록 후 설문 종료 체크
- **파일**: `src/main/java/com/example/thinkfast/service/survey/ResponseService.java`
- **메서드**: `createResponse()` - 응답 저장 후 `checkAndUpdateExpiredSurveyReports()` 호출

#### 설문 종료 감지 및 리포트 업데이트
- **파일**: `src/main/java/com/example/thinkfast/service/survey/SurveyService.java`
- **메서드**: `checkAndUpdateExpiredSurveyReports()` - 설문 종료 여부 확인
- **메서드**: `updateSurveyReports()` - 리포트 업데이트 (Summary, WordCloud, Insight)

#### 리포트 생성 서비스
- **요약 리포트**: `src/main/java/com/example/thinkfast/service/ai/SummaryService.java`
- **워드 클라우드**: `src/main/java/com/example/thinkfast/service/ai/WordCloudService.java`
- **인사이트**: `src/main/java/com/example/thinkfast/service/ai/InsightService.java`
- **통계**: `src/main/java/com/example/thinkfast/service/ai/SurveyStatisticsService.java`

#### 배치 처리 최적화 Repository 메서드 
- **SurveyRepository**: `findActiveSurveysByEndTimeAfter()` - 진행 중인 설문 배치 조회
- **QuestionRepository**: `findBySurveyIdIn()` - 여러 설문의 질문을 한 번에 조회
- **ResponseRepository**: `countDistinctResponseSessionsBySurveyIds()` - 여러 설문의 응답 수를 한 번에 조회

---

## 1. 현재 기능 개요

### 1.1 설문 요약 리포트 기능

#### 1.1.1 주요 기능
- **API 엔드포인트**: `GET /survey/{id}/summary`
- **권한**: 설문 소유자만 조회 가능 (공개/비공개 설정에 따라 차등 적용)
- **조회 방식**: DB에 저장된 리포트 우선 조회, 없으면 실시간 생성

#### 1.1.2 리포트 구성 요소
1. **mainPosition** (String | null)
   - 첫 번째 객관식 질문에서 비율이 가장 높은 옵션
   - `SurveyStatisticsService.getFirstQuestionTopOption()` 사용

2. **mainPositionPercent** (Double | null)
   - mainPosition 옵션의 선택 비율 (%)

3. **insights** (List<String>) ⭐ **변경 예정**
   - 주관식 질문에서 동적으로 추출한 인사이트 리스트 (최대 5개)
   - LLM 기반 문맥 이해 및 자연어 생성으로 동적 분석
   - 질문의 의도와 응답 내용을 종합적으로 분석하여 생성
   - ~~`ImprovementExtractionService.extractImprovements()` 사용~~ (템플릿 기반, 제거 예정)

#### 1.1.3 리포트 저장 구조
- **저장 위치**: `INSIGHT_REPORTS` 테이블
- **저장 방식**: JSON으로 직렬화하여 `summaryText` 컬럼에 저장
- **비동기 저장**: `@Async` 어노테이션을 통한 백그라운드 저장 지원

#### 1.1.4 응답 분석 및 인사이트 추출 프로세스 ⭐ **변경 예정**

**현재 프로세스 (템플릿 기반 - 제거 예정):**
1. 주관식 질문 응답 수집
2. 텍스트 결합 및 키워드 추출 (`TextAnalysisService`)
   - 모든 키워드 추출 (불용어 제거 후)
3. 빈도수 계산 및 정렬
4. 템플릿 기반 문장 생성 (빈도수 2 이상만)

**현재 한계점:**
- 템플릿 기반 문장 생성으로 인한 단순함
- 문맥 이해 부족 (키워드 중심 분석)
- 질문의 의도와 응답 내용 간의 연관성 파악 어려움
- 고정된 키워드("개선사항") 의존

**향후 개선 방향 (LLM 기반 동적 분석):**
- LLM(예: GPT-4, Claude)을 활용한 질문 내용에 따른 응답에 대한 문맥 이해 기반 동적 분석
- 템플릿 기반이 아닌 자연어 생성 기반으로 전환
- 질문의 의도와 응답 내용을 종합적으로 분석하여 동적으로 인사이트 생성
- 키워드 기반이 아닌 문맥 기반 분석으로 전환
- 질문 유형에 따라 자동으로 적절한 분석 방식 선택

### 1.2 설문 응답 기능

#### 1.2.1 주요 기능
- **API 엔드포인트**: `POST /survey/{surveyId}/responses`
- **참여 방식**: 비회원, 무인증 참여 가능
- **응답 저장**: 각 질문별 응답을 `Response` 테이블에 저장
- **응답 세션 관리**: `responseSessionId` (UUID)로 한 설문의 응답들을 그룹화

#### 1.2.2 중복 응답 방지
- **체크 방식**: DeviceId + IP Address 조합
- **해시 처리**: SHA256으로 해시화하여 저장
- **이력 저장**: `SURVEY_RESPONSE_HISTORY` 테이블에 응답 이력 저장
- **체크 로직**:
  - DeviceId + IP 모두 존재: 조합으로 중복 체크
  - DeviceId만 존재: DeviceId로만 중복 체크
  - IP만 존재: IP로만 중복 체크
  - 둘 다 없음: 중복 체크 불가 (허용)

#### 1.2.3 응답 저장 프로세스
1. 설문 활성화 여부 확인
2. 중복 응답 체크
3. 각 질문별 응답 저장 (`Response` 테이블)
   - 객관식: `optionId` 저장
   - 주관식: `subjectiveContent` 저장
4. 응답 이력 저장 (`SurveyResponseHistory`)
5. Redis를 통한 실시간 알람 발송

#### 1.2.4 응답 조회 기능
- **API 엔드포인트**: `GET /survey/{surveyId}/questions/{questionId}/responses`
- **페이징 지원**: page, size 파라미터 (기본값: page=1, size=10, 최대 size=100)
- **정렬**: 생성일시 내림차순 (최신순)
- **권한**: 공개 설문은 인증 없이 접근 가능, 비공개 설문은 소유자만 접근 가능

---

## 2. 현재 구조의 한계점 및 개선 필요 사항

### 2.1 설문 요약 리포트 기능

#### 2.1.1 기능적 한계
1. **제한적인 리포트 내용**
   - 첫 번째 객관식 질문만 분석 (mainPosition)
   - 다른 객관식 질문의 통계 정보 부재
   - 척도형 질문 미지원

2. **응답 분석 및 인사이트 추출의 정확도** ⭐ **변경 예정**
   - 템플릿 기반 문장 생성으로 인한 단순함
   - 문맥 이해 부족 (키워드 중심)
   - 질문의 의도와 응답 내용 간의 연관성 파악 어려움
   - 최소 빈도수 2 이상만 반영 (낮은 응답수 설문에서 제한적)
   - ~~하드코딩된 키워드 사전 의존~~ (제거됨 - 현재 모든 키워드 추출)
   - **개선 계획**: LLM 기반 문맥 이해 및 동적 분석으로 전환 예정
   - **변경 사항**: "개선사항" 키워드 제거, 질문 유형에 따른 동적 분석으로 전환

3. **실시간 생성 성능**
   - DB에 리포트가 없을 때 실시간 생성으로 인한 응답 지연
   - 대량 응답 설문의 경우 생성 시간 증가
   - ✅ **개선됨**: 배치 처리로 성능 최적화 완료

4. **리포트 업데이트**
   - ✅ **개선됨**: 진행 중인 설문에 대해 1분마다 자동 업데이트 스케줄러 구현 완료
   - 리포트 버전 관리 부재

#### 2.1.2 기술적 한계
1. **캐싱 전략 부재**
   - 리포트 조회 시 캐싱 미적용
   - 반복 조회 시 DB/계산 오버헤드

2. **비동기 처리**
   - ✅ **개선됨**: 리포트 생성은 `@Async` 지원하며, 트리거 시점이 명확함
   - ✅ **개선됨**: 설문 종료 시 자동 리포트 생성 스케줄러 구현 완료
   - ✅ **개선됨**: 진행 중인 설문 실시간 업데이트 스케줄러 구현 완료

3. **에러 처리**
   - 리포트 생성 실패 시 사용자에게 명확한 피드백 부족
   - 부분 실패(예: 인사이트 추출만 실패) 시 처리 방식 미정의

### 2.2 설문 응답 기능

#### 2.2.1 기능적 한계
1. **중복 응답 방지의 한계**
   - DeviceId/IP가 없는 경우 중복 체크 불가
   - VPN/프록시 환경에서 IP 기반 체크 불안정
   - 쿠키/로컬스토리지 기반 추가 식별자 부재
   - 시간 기반 제한 없음 (예: 설문 종료 후 응답 불가)

2. **응답 데이터 검증**
   - 필수 질문 체크 부재
   - 응답 형식 검증 (예: 주관식 최대 길이)
   - 악의적 응답 필터링 부재

3. **응답자 정보**
   - 응답자 식별 정보 부족 (비회원 응답)
   - 응답 시간대, 디바이스 정보 등 메타데이터 부족
   - 응답 패턴 분석 불가

#### 2.2.2 기술적 한계
1. **성능 이슈**
   - 대량 응답 시 순차적 저장으로 인한 성능 저하
   - Bulk Insert 미적용

2. **트랜잭션 처리**
   - 응답 저장 중 실패 시 부분 저장 가능성
   - 롤백 전략 명확하지 않음

3. **알람 처리**
   - Redis 알람만 발송, DB 저장 없음
   - 알람 읽음/안읽음 상태 관리 부재
   - 알람 이력 조회 불가

4. **응답 조회 성능**
   - 대량 응답 조회 시 성능 이슈 가능성
   - 인덱스 최적화 필요 여부 확인

---

## 3. 개선 방안

### 3.1 설문 요약 리포트 기능 개선

#### 3.1.1 리포트 내용 확장
- [ ] **다중 질문 분석 지원**
  - 모든 객관식 질문의 통계 포함
  - 질문별 주요 선택지 및 비율 제공
  - 질문 간 상관관계 분석

- [ ] **척도형 질문 지원** (현재 기능 미제공.)
  - 평균값, 중앙값, 분포 통계 제공
  - 척도형 질문 결과 시각화 데이터 포함

- [ ] **응답 통계 정보 추가**
  - 총 응답 수
  - 응답률 (목표 대비)
  - 응답 시간대 분포
  - 응답 소요 시간 분석

#### 3.1.2 LLM 기반 동적 분석 및 인사이트 추출 ⭐ **우선순위 높음**
- [ ] **LLM 기반 문맥 이해 및 동적 자연어 생성** ⭐ **우선순위 높음**
  - **목표**: 질문 내용에 따른 응답에 대한 문맥 이해 기반 동적 분석
  - **핵심 원칙**: 
    - ❌ "개선사항" 같은 고정된 키워드 사용 안 함
    - ✅ 질문 유형과 응답 내용에 따라 동적으로 분석
    - ✅ 템플릿 기반이 아닌 자연어 생성
  - **기술 스택**: LLM API (OpenAI GPT-4, Anthropic Claude 등)
  - **구현 방안**:
    1. 질문 내용과 응답 내용을 함께 LLM에 전달
    2. LLM이 질문의 의도와 응답의 맥락을 종합적으로 분석
    3. 질문 유형에 따라 자동으로 적절한 분석 방식 선택
       - 피드백 요청 질문 → 주요 의견 및 제안 사항 추출
       - 경험 조사 질문 → 주요 경험 및 인사이트 추출
       - 의견 조사 질문 → 주요 의견 및 트렌드 추출
       - 기타 질문 → 질문 의도에 맞는 동적 분석
    4. 템플릿 기반이 아닌 자연어 생성으로 인사이트 도출
    5. 질문별 특성에 맞는 맞춤형 인사이트 생성
  - **예상 효과**:
    - 키워드 중심 분석의 한계 극복
    - 질문과 응답 간의 연관성 정확도 향상
    - 더 의미 있고 구체적인 인사이트 추출
    - 자연스러운 문장 생성으로 가독성 향상
    - 질문 유형에 따른 맞춤형 분석
  - **구현 위치**: `ImprovementExtractionService` 리팩토링 또는 새로운 `LLMInsightExtractionService` 생성
  - **고려사항**:
    - LLM API 호출 비용 관리
    - 응답 지연 시간 최적화 (비동기 처리)
    - API 실패 시 폴백 전략 (기존 템플릿 방식으로 대체)
    - 프롬프트 엔지니어링 최적화 (질문 유형별 프롬프트)

- [ ] **감성 분석 통합**
  - 응답의 긍정/부정 감성 분석
  - 감성에 따른 인사이트 우선순위 조정
  - 긴급한 이슈 자동 식별

- [ ] **동적 카테고리 분류**
  - 질문 유형에 따른 자동 카테고리 분류
  - 우선순위 제안 (빈도, 긴급도 등)

#### 3.1.3 성능 최적화
- [ ] **캐싱 전략 도입**
  - Redis를 활용한 리포트 캐싱
  - 캐시 TTL 설정 (예: 1시간)
  - 캐시 무효화 전략 (새 응답 추가 시)

- [x] **리포트 생성 스케줄링** ✅ **완료**
  - ✅ 설문 종료 시 자동 리포트 생성 (구현 완료)
  - ✅ 주기적 리포트 업데이트 (진행 중인 설문 1분마다 자동 업데이트)
  - [ ] 증분 업데이트 지원 (향후 개선)

- [x] **배치 처리 최적화** ✅ **완료**
  - ✅ 여러 설문의 질문을 한 번에 조회 (`findBySurveyIdIn`)
  - ✅ 여러 설문의 응답 수를 한 번에 조회 (`countDistinctResponseSessionsBySurveyIds`)
  - ✅ 진행 중인 설문 배치 조회 (`findActiveSurveysByEndTimeAfter`)

- [ ] **비동기 처리 강화**
  - ✅ 리포트 생성은 `@Async` 지원 (구현 완료)
  - [ ] 리포트 생성 작업 큐 도입 (예: RabbitMQ, Kafka) - 향후 개선
  - [ ] 작업 상태 추적 (진행 중, 완료, 실패) - 향후 개선
  - [ ] 사용자에게 생성 상태 알림 - 향후 개선

#### 3.1.4 LLM 기반 동적 분석 및 인사이트 추출 구현 세부사항 

##### 3.1.4.1 구현 목표
- 질문 내용과 응답 내용을 함께 분석하여 문맥을 이해
- 템플릿 기반이 아닌 자연어 생성으로 동적 인사이트 도출
- 질문의 의도에 맞는 맞춤형 인사이트 생성
- ❌ "개선사항" 같은 고정된 키워드 사용 안 함
- ✅ 질문 유형에 따라 동적으로 분석 방식 결정

##### 3.1.4.2 구현 방안

**1. 프롬프트 설계 (동적 분석)**
```
질문: "{questionContent}"
질문 유형: {questionType} (예: 피드백 요청, 경험 조사, 의견 조사 등)
응답들:
{response1}
{response2}
...

위 질문과 응답들을 종합적으로 분석하여, 가장 중요한 인사이트 5가지를 자연스러운 문장으로 요약해주세요.
- 질문의 의도와 응답 내용을 종합적으로 고려하여 작성
- 질문 유형에 맞는 적절한 관점에서 분석
- 고정된 키워드나 템플릿을 사용하지 말고, 응답 내용에 기반하여 동적으로 생성
- 각 인사이트는 구체적이고 의미 있게 작성
```

**2. LLM API 통합**
- **서비스 클래스**: `LLMInsightExtractionService` (신규) 또는 `InsightExtractionService` (리팩토링)
- **기존 서비스**: `ImprovementExtractionService` → 제거 또는 리팩토링 예정
- **LLM 선택**: OpenAI GPT-4, GPT-3.5-turbo 또는 Anthropic Claude
- **비동기 처리**: `@Async`를 통한 백그라운드 처리
- **에러 처리**: LLM API 실패 시 기존 템플릿 방식으로 폴백 (점진적 전환)

**3. 성능 최적화**
- **배치 처리**: 여러 질문의 응답을 한 번에 LLM에 전달 (토큰 제한 고려)
- **캐싱**: 동일한 질문-응답 조합에 대한 결과 캐싱
- **비용 관리**: 토큰 사용량 모니터링 및 최적화

**4. 구현 위치**
- **파일**: `src/main/java/com/example/thinkfast/service/ai/LLMInsightExtractionService.java` (신규)
- **또는**: `src/main/java/com/example/thinkfast/service/ai/InsightExtractionService.java` (리팩토링)
- **기존 파일**: `ImprovementExtractionService.java` → 제거 또는 리팩토링 예정
- **호출 위치**: `SummaryService.generateSummaryReport()` 또는 `SummaryService.saveSummaryReportAsync()`

**5. 설정 관리**
- **프로퍼티**: `application.yml`에 LLM API 키 및 설정 추가
- **환경 변수**: 프로덕션 환경에서 API 키 관리
- **스위치**: LLM 사용 여부를 설정으로 제어 (기능 토글)

**6. 테스트 계획**
- **단위 테스트**: LLM API 모킹을 통한 로직 테스트
- **통합 테스트**: 실제 LLM API 호출 테스트 (제한적)
- **성능 테스트**: 응답 시간 및 비용 측정

##### 3.1.4.3 예상 효과
- **정확도 향상**: 키워드 중심 분석의 한계 극복
- **가독성 향상**: 자연스러운 문장 생성
- **맞춤형 분석**: 질문별 특성에 맞는 동적 인사이트 도출
- **유연성**: 질문 유형에 따라 자동으로 적절한 분석 방식 선택
- **사용자 만족도**: 더 의미 있고 구체적인 리포트 제공
- **확장성**: 새로운 질문 유형 추가 시 자동으로 적응

##### 3.1.4.4 고려사항
- **비용**: LLM API 호출 비용 관리 필요
- **지연 시간**: API 응답 시간 최적화 (비동기 처리 필수)
- **안정성**: API 실패 시 폴백 전략 필수
- **프라이버시**: 응답 데이터가 LLM API로 전송되는 것에 대한 사용자 동의 필요 여부 검토

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
- [ ] **다중 식별자 조합**
  - DeviceId + IP + User-Agent 해시 조합
  - 쿠키 기반 세션 ID 추가
  - Fingerprinting 기술 도입 (선택적)

- [ ] **시간 기반 제한**
  - 설문 종료 시간 이후 응답 불가
  - 최소 응답 간격 설정 (예: 같은 설문에 24시간 내 재응답 불가)
  - 응답 가능 시간대 설정

- [ ] **응답자 인증 옵션**
  - 선택적 이메일 인증
  - SMS 인증 (옵션)
  - 소셜 로그인 연동 (옵션)

#### 3.2.2 응답 데이터 검증 강화
- [ ] **필수 질문 검증**
  - 질문별 필수 여부 설정
  - 필수 질문 미응답 시 에러 반환

- [ ] **응답 형식 검증**
  - 주관식 최대/최소 길이 제한
  - 특수 문자 필터링
  - 악의적 콘텐츠 탐지 (욕설, 스팸 등)

- [ ] **응답 품질 검증**
  - 응답 시간 기반 이상치 탐지 (너무 빠른 응답)
  - 패턴 기반 이상치 탐지 (모든 질문 동일 응답 등)

#### 3.2.3 응답자 정보 확장
- [ ] **응답 메타데이터 수집**
  - 응답 시간대
  - 디바이스 정보 (OS, 브라우저 등)
  - 응답 소요 시간
  - 응답 시작 시간

- [ ] **응답자 프로필 (선택적)**
  - 연령대, 성별 등 (선택 사항)
  - 지역 정보 (IP 기반, 선택적)
  - 응답 동기 (선택적)

#### 3.2.4 성능 최적화
- [ ] **Bulk Insert 적용**
  - 여러 질문 응답을 한 번에 저장
  - 배치 처리로 성능 향상

- [ ] **비동기 처리**
  - 응답 저장 후 즉시 반환
  - 알람 발송 등 부가 작업은 비동기 처리

- [ ] **데이터베이스 최적화**
  - 인덱스 추가 (surveyId, questionId, createdAt 등)
  - 파티셔닝 고려 (대량 데이터 시)

#### 3.2.5 알람 시스템 개선
- [ ] **알람 DB 저장**
  - 알람 이력 DB 저장
  - 읽음/안읽음 상태 관리
  - 알람 타입별 분류

- [ ] **알람 조회 API**
  - 사용자별 알람 목록 조회
  - 알람 읽음 처리 API
  - 알람 삭제 API

- [ ] **알람 설정**
  - 알람 수신 설정 (이메일, 푸시 등)
  - 알람 빈도 설정 (즉시, 요약 등)

---

## 4. 우선순위별 구현 계획

### 4.1 Phase 1: 핵심 개선 (1-2주)

#### 설문 응답 기능
1. **필수 질문 검증 추가**
   - Question 엔티티에 `isRequired` 필드 추가
   - 응답 생성 시 필수 질문 검증 로직 구현

2. **응답 데이터 검증 강화**
   - 주관식 최대 길이 제한
   - 기본적인 응답 형식 검증

3. **알람 DB 저장 및 조회 기능**
   - 알람 엔티티 생성
   - 알람 저장 및 조회 API 구현

#### 설문 요약 리포트 기능
1. **캐싱 전략 도입**
   - Redis를 활용한 리포트 캐싱
   - 캐시 무효화 로직 구현

2. **리포트 생성 스케줄링** ✅ **완료**
   - ✅ 설문 종료 시 자동 리포트 생성 (구현 완료)
   - ✅ 진행 중인 설문 실시간 업데이트 스케줄러 구현 완료
   - ✅ 배치 처리 최적화 완료

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
   - **LLM 기반 문맥 이해 및 동적 자연어 생성**
     - 질문 내용에 따른 응답에 대한 문맥 이해 기반 동적 분석
     - 템플릿 기반이 아닌 자연어 생성 기반으로 전환
     - 질문의 의도와 응답 내용을 종합적으로 분석
     - ❌ "개선사항" 같은 고정된 키워드 제거
     - ✅ 질문 유형에 따라 동적으로 분석 방식 결정
   - 동적 카테고리 분류 추가

### 4.3 Phase 3: 고급 기능 (3-4주)

#### 설문 응답 기능
1. **응답자 인증 옵션**
   - 선택적 이메일 인증
   - 응답 인증 API 구현

2. **응답 품질 검증**
   - 이상치 탐지 로직
   - 악의적 콘텐츠 탐지

#### 설문 요약 리포트 기능
1. **리포트 버전 관리**
   - 버전 추적 기능
   - 비교 기능

2. **고급 분석 기능**
   - 질문 간 상관관계 분석
   - 시계열 분석
   - 예측 분석

---

## 5. 기술 스택 제안

### 5.1 캐싱
- **Redis**: 리포트 캐싱, 세션 관리

### 5.2 메시지 큐
- **RabbitMQ** 또는 **Apache Kafka**: 비동기 작업 처리

### 5.3 AI/ML 서비스
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

## 12. 참고 자료 및 관련 문서

### 현재 구현 파일
- `src/main/java/com/example/thinkfast/controller/SurveyController.java`
- `src/main/java/com/example/thinkfast/service/ai/SummaryService.java`
- `src/main/java/com/example/thinkfast/service/ai/ImprovementExtractionService.java` - 현재 템플릿 기반 (제거 예정)
- `src/main/java/com/example/thinkfast/service/survey/ResponseService.java`
- `src/main/java/com/example/thinkfast/scheduler/SurveySchedule.java` ⭐ **실시간 업데이트 스케줄러**
- `src/main/java/com/example/thinkfast/repository/survey/SurveyRepository.java` ⭐ **배치 처리 메서드**
- `src/main/java/com/example/thinkfast/repository/survey/QuestionRepository.java` ⭐ **배치 처리 메서드**
- `src/main/java/com/example/thinkfast/repository/survey/ResponseRepository.java` ⭐ **배치 처리 메서드**

### 향후 구현 예정 파일
- `src/main/java/com/example/thinkfast/service/ai/LLMInsightExtractionService.java` ⭐ **LLM 기반 동적 인사이트 추출 (예정)**
  - ❌ "개선사항" 키워드 제거
  - ✅ 질문 유형에 따른 동적 분석

### 문서
- `tasklist/요약 리포트 조회 API 명세서.md`

---

## 13. 최근 변경사항 요약 (업데이트: 2025-12-10)

### ✅ 완료된 개선사항

#### 1. 실시간 통계 업데이트 스케줄러 구현
- **기능**: 진행 중인 설문에 대해 1분마다 요약 리포트, 워드클라우드, 인사이트 자동 업데이트
- **대상**: 진행 중인 설문 (isActive = true, isDeleted = false, endTime > 현재 시간) 중 응답이 1개 이상인 설문
- **위치**: `SurveySchedule.updateActiveSurveyReports()`
- **효과**: 새로운 응답이 추가되면 최신 데이터로 리포트가 자동 갱신됨

#### 2. 배치 처리 성능 최적화
- **Repository 메서드 추가**:
  - `SurveyRepository.findActiveSurveysByEndTimeAfter()`: 진행 중인 설문 배치 조회
  - `QuestionRepository.findBySurveyIdIn()`: 여러 설문의 질문을 한 번에 조회
  - `ResponseRepository.countDistinctResponseSessionsBySurveyIds()`: 여러 설문의 응답 수를 한 번에 조회
- **효과**: DB 쿼리 수 대폭 감소, 네트워크 라운드트립 최소화

#### 3. 스케줄러 구조 개선
- **기존**: 설문 종료 시에만 리포트 생성
- **개선**: 설문 종료 시 + 진행 중인 설문 실시간 업데이트 (1분마다)
- **효과**: 사용자가 실시간으로 최신 통계를 확인할 수 있음

### 📝 향후 개선 계획

1. **LLM 기반 문맥 이해 및 동적 자연어 생성** ⭐ **우선순위 높음**
   - 질문 내용에 따른 응답에 대한 문맥 이해 기반 동적 분석
   - 템플릿 기반이 아닌 자연어 생성 기반으로 전환
   - 질문의 의도와 응답 내용을 종합적으로 분석하여 동적으로 인사이트 생성
   - ❌ "개선사항" 같은 고정된 키워드 제거
   - ✅ 질문 유형에 따라 동적으로 분석 방식 결정
   - 기술 스택: OpenAI GPT-4 또는 Anthropic Claude API

2. **캐싱 전략 도입**: Redis를 활용한 리포트 캐싱

3. **리포트 버전 관리**: 변경 이력 추적 및 비교 기능

4. **작업 큐 도입**: RabbitMQ/Kafka를 통한 비동기 작업 처리 강화

5. **증분 업데이트**: 전체 재생성 대신 변경된 부분만 업데이트

