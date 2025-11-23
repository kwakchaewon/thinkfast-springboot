# 설문 결과 페이지 AI 기능 전략 수립 및 Todo 리스트

## 작업 목표
설문 결과 페이지의 AI 기반 기능(요약 리포트, 워드클라우드, 인사이트 생성)을 구현하기 위한 인프라 및 API 개발 작업


## AI 기능 개요

### 1. 요약 리포트 생성
- 설문 전체 데이터를 분석하여 질문별 주요 인사이트 추출
- AI 설문 템플릿은 아래 템플릿으로 적절한 것을 선택하여 추출
  - 주요 인사이트 요약 (응답자 수, 긍정/부정 비율, 핵심 키워드 등)
  - 설문 핵심 발견점
  - 행동 추천
  - AI 생성형 요약 (응답자들은 주로 ~했으며 ~로 나타났습니다.)
  - 핵심 주제 분석

### 2. 워드클라우드 데이터 집계
- 주관식 질문의 모든 응답 텍스트 분석
- 키워드 추출 및 빈도수 집계
- 불용어 제거 및 의미있는 단어 선별

### 3. 인사이트 텍스트 자동 생성
- 통계 데이터를 기반으로 인사이트 문장 자동 생성
- 객관식: 옵션별 분포를 설명
- 주관식: 주요 키워드와 트렌드를 설명하는 텍스트

---

## ToDo 리스트

### AI 인프라 구축
- [x] AI 서비스 제공 방식 결정
  - [x] **Java 단일 구현 채택** - 모든 기능을 Java 백엔드에서 구현 (기본 전략)
  - [x] **외부 AI API 사용** - Java에서 직접 호출하여 응답 저장 (Gemini 무료 API 사용)
  - [x] **Python 서버 구축 안함** - 우선 구축하지 않기로 결정 (형태소 분석 등 고급 기능은 Java 템플릿으로 대체)
  - [x] 하이브리드 방식 (기본은 Java 자체, 고급은 무료 AI API) - **채택됨 (무료 중심, Java 우선)**
- [ ] AI API 통신 모듈 개발 (Phase 2 선택사항)
  - [ ] HTTP 클라이언트 설정 (WebClient) - Java에서 직접 구현
  - [ ] **Gemini API 클라이언트 구현** - Java에서 직접 호출
    - [ ] Gemini API 엔드포인트: `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent`
    - [ ] 요청 헤더: `Content-Type: application/json`, `X-goog-api-key: {API_KEY}`
    - [ ] 요청 본문 구조: `{"contents": [{"parts": [{"text": "..."}]}]}`
    - [ ] 응답 파싱 로직 구현
  - [ ] AI API 응답 저장 로직 구현 (분석 결과를 DB 또는 Redis에 저장)
  - [ ] API 키 관리 및 보안 처리 (application.yml 환경변수 활용)
    - [ ] `gemini.api-key` 설정 추가 (application-local.yml, application-prod.yml)
  - [ ] 요청/응답 타임아웃 및 재시도 로직
  - [ ] 에러 핸들링 및 폴백 처리 (무료 티어 한도 초과 시 Java 템플릿으로 폴백)
  - [ ] 무료 티어 한도 모니터링
- [ ] AI 서비스 비용 및 사용량 모니터링 시스템 구축
  - [ ] API 호출 횟수 추적
  - [ ] 비용 계산 로직 (외부 API 사용 시)

### 텍스트 분석 모듈 개발 (Java 자체 구현)
- [ ] 한국어 텍스트 전처리 모듈
  - [ ] 특수문자, 이모지 제거 로직
  - [ ] 기본 텍스트 정제 작업
  - [ ] 불용어(stop words) 사전 구축 (하드코딩된 리스트)
  - [ ] 형태소 분석 없이 정규식 기반 처리 (Python 서버 구축 안함)
- [ ] 키워드 추출 모듈
  - [ ] 정규식 기반 단어 추출 (`Pattern.compile("[가-힣]+")`)
  - [ ] 키워드 빈도수 계산
  - [ ] 키워드 필터링 로직 (최소 빈도수, 최대 개수 등)
  - [ ] 상위 N개 키워드 선별
  - [ ] 유사 키워드 그룹화 (정규식 기반으로 간단히 구현)
- [ ] 키워드 랭킹 알고리즘
  - [ ] 빈도수 기반 정렬 (기본 구현)
  - [ ] TF-IDF 기반 중요도 계산 (선택사항 - Java 라이브러리 활용 가능)

### 요약 리포트 생성 로직
- [ ] 설문 데이터 집계 및 분석
  - [ ] 객관식 질문별 주요 응답 옵션 추출
  - [ ] 비율이 가장 높은 옵션 식별
  - [ ] 트렌드 패턴 분석 (선택사항)
- [ ] 개선 사항 추출 로직
  - [ ] 주관식 질문에서 개선 관련 키워드 추출 (Java - 정규식 기반)
  - [ ] 템플릿 기반 개선 사항 문장 생성 (Java - 기본 구현)
  - [ ] AI 기반 요약 생성 (Phase 2 선택사항 - Java에서 무료 AI API 직접 호출)
- [ ] 요약 리포트 응답 포맷 정의
  - [ ] API 응답 구조 설계
  - [ ] 데이터 타입 정의

### 워드클라우드 집계 API
- [ ] 주관식 질문 응답 데이터 수집
  - [ ] 질문별 모든 응답 텍스트 조회
  - [ ] 데이터 전처리
- [ ] 키워드 분석 및 집계
  - [ ] 텍스트 분석 모듈 호출
  - [ ] 키워드 빈도수 집계
  - [ ] 워드클라우드 데이터 포맷 생성
- [ ] 워드클라우드 API 엔드포인트 구현
  - [ ] `GET /survey/:id/questions/:questionId/wordcloud` API 개발
  - [ ] 응답 데이터 구조 정의
  - [ ] 캐싱 전략 구현 (선택사항 - 계산 비용 절감)

### 인사이트 텍스트 생성 로직
- [ ] 객관식 인사이트 생성
  - [ ] 옵션별 분포 분석 (Java)
  - [ ] 패턴 인식 로직 (예: 특정 옵션이 과반수, 비슷한 비율 등) (Java)
  - [ ] 템플릿 기반 문장 생성 (Java - 기본 구현)
  - [ ] AI 기반 생성 (Phase 2 선택사항 - Java에서 무료 AI API 직접 호출)
- [ ] 주관식 인사이트 생성
  - [ ] 주요 키워드 기반 문장 생성 (Java - 템플릿 기반)
  - [ ] 트렌드 설명 텍스트 생성 (Java)
  - [ ] AI 기반 자연어 생성 (Phase 2 선택사항 - Java에서 무료 AI API 직접 호출)
- [ ] 인사이트 텍스트 저장 및 관리
  - [ ] 인사이트 텍스트 캐싱 (동일한 통계에 대해 재계산 방지)
  - [ ] 수동 수정 기능 (선택사항)

### Backend API 개발
- [ ] 요약 리포트 조회 API (`GET /survey/:id/summary`)
  - [ ] 설문 소유자 확인 로직 구현 (userId 기반 권한 검증)
  - [ ] 설문 데이터 분석 수행
  - [ ] 요약 리포트 생성 로직 호출
  - [ ] 응답 데이터 반환
  - [ ] 캐싱 처리 (Redis 활용)
- [ ] 질문별 응답 통계 API 구현 및 인사이트 추가
  - [ ] `GET /survey/:id/questions/:questionId/statistics` API 구현 (현재 미구현 상태)
  - [ ] 설문 소유자 확인 로직 구현
  - [ ] 객관식/주관식/척도형 질문별 통계 집계 로직 구현
  - [ ] 인사이트 텍스트 생성 로직 통합
  - [ ] 응답에 `insight` 필드 추가
  - [ ] 페이징 처리 (대용량 응답 데이터 고려)
- [ ] 워드클라우드 조회 API (`GET /survey/:id/questions/:questionId/wordcloud`)
  - [ ] 설문 소유자 확인 로직 구현
  - [ ] 주관식 질문 검증 (워드클라우드는 주관식 질문에만 적용)
  - [ ] 키워드 분석 및 집계 로직 호출
  - [ ] 워드클라우드 데이터 반환
  - [ ] 캐싱 처리 (Redis 활용, 데이터 변경 시에만 재계산)

### 데이터베이스 스키마
- [ ] 기존 테이블 확인 및 활용
  - [ ] `INSIGHT_REPORTS` 테이블 확인 (이미 존재 - V2 마이그레이션)
  - [ ] 기존 스키마와의 호환성 검토
  - [ ] 필요 시 추가 컬럼 마이그레이션 작성
- [ ] AI 분석 결과 캐싱 테이블 설계 (선택사항 - Redis 우선 고려)
  - [ ] 요약 리포트 캐시 테이블 (Redis 우선, DB는 장기 보관용)
  - [ ] 워드클라우드 캐시 테이블
  - [ ] 인사이트 텍스트 캐시 테이블
  - [ ] 캐시 무효화 로직 (새 응답 추가 시)
  - [ ] AI 분석 이력 테이블 (선택사항)
  - [ ] 분석 수행 이력 기록
  - [ ] 사용량 추적
  - [ ] AI API 호출 이력 (디버깅 및 모니터링용, Phase 2 선택사항)
  - [ ] AI API 응답 저장 (분석 결과를 DB에 저장하여 재사용)
- [ ] 데이터베이스 인덱스 최적화
  - [ ] RESPONSES 테이블 인덱스 (QUESTION_ID, OPTION_ID)
  - [ ] 통계 집계 쿼리 성능 향상을 위한 복합 인덱스
  - [ ] Flyway 마이그레이션 파일 작성

### 성능 최적화
- [ ] 비동기 처리
  - [ ] `@EnableAsync` 설정 추가 (ThinkfastApplication에 추가)
  - [ ] `@Async` 어노테이션을 활용한 비동기 메서드 구현
  - [ ] ThreadPoolTaskExecutor 설정 (비동기 작업 스레드 풀)
  - [ ] 대용량 텍스트 분석 시 백그라운드 작업 처리
  - [ ] 작업 큐 시스템 구축 (선택사항 - Redis Queue 활용 고려)
- [ ] 캐싱 전략
  - [ ] **Redis 캐싱 구현** (프로젝트에 Redis 이미 사용 중)
  - [ ] RedisTemplate 또는 RedisCacheManager 설정
  - [ ] AI 분석 결과 캐싱 (동일 입력에 대해 재계산 방지)
  - [ ] 캐시 키 명명 규칙 정의 (`survey:{surveyId}:summary` 등)
  - [ ] 캐시 TTL 설정 (기본값 1시간)
  - [ ] 캐시 무효화 전략 (설문 응답 추가 시 이벤트 기반 무효화)
- [ ] 데이터베이스 쿼리 최적화
  - [ ] 통계 집계 쿼리 인덱싱 (QUESTION_ID, OPTION_ID 등)
  - [ ] 대용량 응답 데이터 조회 시 페이징 처리
  - [ ] N+1 쿼리 문제 해결 (JOIN FETCH 활용)
  - [ ] 쿼리 성능 모니터링 및 최적화
- [ ] 배치 처리
  - [ ] `@Scheduled` 어노테이션 활용 (이미 @EnableScheduling 사용 중)
  - [ ] 주기적으로 인기 설문의 분석 결과 사전 계산
  - [ ] 크론 작업 설정 (선택사항)

### 에러 핸들링 및 폴백
- [ ] GlobalExceptionHandler 통합
  - [ ] 기존 `GlobalExceptionHandler`에 AI 관련 예외 추가
  - [ ] 커스텀 예외 클래스 정의 (AiServiceException, TextAnalysisException 등)
  - [ ] 예외별 적절한 HTTP 상태 코드 및 메시지 반환
- [ ] AI 서비스 장애 시 폴백 로직
  - [ ] 외부 AI API 연결 실패 시 Java 템플릿 기반 인사이트로 폴백
  - [ ] 이전에 생성된 캐시 데이터 사용 (Redis 또는 DB)
  - [ ] Circuit Breaker 패턴 적용 (선택사항 - Resilience4j)
  - [ ] 에러 메시지 사용자 친화적으로 표시
  - [ ] 로깅 및 모니터링 (기존 로깅 시스템 활용)
- [ ] 분석 실패 케이스 처리
  - [ ] 빈 응답 데이터 처리 (응답 없음 메시지 반환)
  - [ ] 분석 불가능한 텍스트 처리 (특수문자만 있는 경우 등)
  - [ ] 부분 실패 시 부분 결과 제공
  - [ ] 타임아웃 처리 (외부 AI API 응답 지연 시)

### 테스트
- [ ] 단위 테스트
  - [ ] 텍스트 전처리 모듈 테스트
  - [ ] 키워드 추출 모듈 테스트
  - [ ] 인사이트 생성 로직 테스트
- [ ] 통합 테스트
  - [ ] AI API 통신 테스트
  - [ ] 워드클라우드 집계 API 테스트
  - [ ] 요약 리포트 생성 API 테스트
- [ ] 성능 테스트
  - [ ] 대용량 응답 데이터 처리 테스트
  - [ ] 응답 시간 측정 및 최적화
- [ ] 정확도 테스트
  - [ ] 키워드 추출 정확도 검증
  - [ ] 인사이트 텍스트 품질 검증

### 문서화
- [ ] AI 기능 API 문서 작성
  - [ ] 요청/응답 스펙
  - [ ] 사용 예시
  - [ ] 제한사항 및 주의사항
- [ ] 개발 가이드 작성
  - [ ] 새로운 분석 로직 추가 방법
  - [ ] AI 서비스 변경 시 마이그레이션 가이드
- [ ] 운영 문서 작성
  - [ ] 모니터링 방법
  - [ ] 트러블슈팅 가이드
  - [ ] 비용 관리 방법

---

## 작업 진행 상황

### 2024-XX-XX
- [x] AI 기능 선행 작업 ToDo 리스트 작성
- [x] AI 인프라 구축 시작
  - [x] AI 서비스 제공 방식 결정 (하이브리드 방식 채택 - 무료 중심)
  - [x] **Java 단일 구현 전략 채택** - 모든 기능을 Java 백엔드에서 구현 (기본)
  - [x] **Python 서버 구축 안함 결정** - 우선 구축하지 않기로 결정
  - [x] **외부 AI API 직접 호출 방식 채택** - Java에서 외부 AI API 호출하여 응답 저장
  - [x] Phase별 구현 전략 수립 (Phase 1: Java 자체, Phase 2: 무료 AI API Java에서 직접 호출)
  - [x] 결정 사항 문서화 (`AI_SERVICE_DECISION.md` 생성)

---

## 서버 개발 시 필요한 부가 정보

### API 응답 포맷
프로젝트에서 사용하는 표준 API 응답 구조:
- **BaseResponseBody AOP 사용**: `@BaseResponseBody` 어노테이션이 자동으로 응답 래핑
- **BaseResponse 클래스**: `success`, `message`, `data`, `timestamp` 필드 포함
- **실제 응답 구조:**
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    // 실제 응답 데이터
  },
  "timestamp": "2024-01-01T00:00:00"
}
```

**에러 응답 예시:**
```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null,
  "timestamp": "2024-01-01T00:00:00"
}
```

**참고:** 
- `BaseResponse.success(data)` 또는 `BaseResponse.fail(message)` 메서드 사용
- `@BaseResponseBody` 어노테이션이 있는 컨트롤러는 자동으로 래핑됨

### 인증 및 인가
- **인증 방식:** Bearer Token (JWT)
- **헤더 형식:** `Authorization: Bearer {accessToken}`
- **토큰 저장 위치:** 클라이언트의 `localStorage`에 `accessToken` 저장
- **토큰 갱신:** `/auth/refresh` 엔드포인트 사용
- **인가 필요 API:** 설문 결과 조회 API는 인증이 필요함 (설문 소유자 또는 관리자만 접근 가능)
- **권한 확인 로직:**
  - `@PreAuthorize("hasRole('CREATOR')")` 사용 (현재 설문 API에 적용됨)
  - **설문 소유자 확인 필수**: `Survey.userId`와 현재 사용자 ID 비교
  - 설문 소유자가 아닌 경우 403 Forbidden 반환
  - **구현 필요**: 현재 SurveyController에 "본인 권한 확인 필요" 주석 존재

### Base URL 및 포트
- **개발 환경:** `http://localhost:8080`
- **프로덕션 환경:** `/api` (nginx 프록시 사용)
- **요청 타임아웃:** 5초

### 데이터베이스 스키마 (실제 구현 기준)
**실제 엔티티 구조 (V2 마이그레이션 기준):**

**설문 (Survey)**
- `id`: Long (PK)
- `userId`: Long (FK, 설문 소유자) - **권한 확인에 사용**
- `title`: String
- `description`: String
- `startTime`: LocalDateTime
- `endTime`: LocalDateTime
- `isActive`: Boolean
- `isDeleted`: Boolean
- `createdAt`: LocalDateTime
- `updatedAt`: LocalDateTime

**질문 (Question)**
- `id`: Long (PK)
- `surveyId`: Long (FK)
- `type`: Enum ('MULTIPLE_CHOICE', 'SUBJECTIVE', 'SCALE')
- `content`: String
- `orderIndex`: Integer

**옵션 (Option)**
- `id`: Long (PK)
- `questionId`: Long (FK)
- `content`: String

**응답 (Response)** - 실제 구조
- `id`: Long (PK)
- `responseSessionId`: String (UUID, 동일 세션의 응답들을 그룹화)
- `questionId`: Long (FK)
- `questionType`: String
- `optionId`: Long | null (객관식인 경우)
- `subjectiveContent`: String | null (주관식인 경우)
- `scaleValue`: Integer | null (척도형인 경우)

**참고:**
- `RESPONSES` 테이블은 질문별로 개별 레코드로 저장됨
- `responseSessionId`로 동일 설문 응답을 그룹화
- 통계 집계 시 `questionId`와 `optionId` 또는 `subjectiveContent`를 활용

### 기존 API 엔드포인트 참고
```
GET  /survey                          - 설문 목록 조회
GET  /survey/recent                   - 최근 설문 목록 조회
GET  /survey/:id                      - 설문 상세 조회
GET  /survey/:id/questions            - 설문 질문 목록 조회
POST /survey                          - 설문 생성
POST /survey/:id/responses            - 설문 응답 제출
DELETE /survey/:id                    - 설문 삭제
```

### 설문 응답 데이터 구조
**요청 포맷 (`POST /survey/:id/responses`):**
```json
{
  "clientInfo": {
    "deviceId": "string"  // 중복 응답 방지용
  },
  "answers": [
    {
      "questionId": 1,
      "type": "MULTIPLE_CHOICE",
      "optionId": 2,        // 객관식: 선택된 옵션 ID
      "content": null
    },
    {
      "questionId": 2,
      "type": "SUBJECTIVE",
      "optionId": null,
      "content": "응답 텍스트"  // 주관식: 응답 내용
    }
  ]
}
```

### 질문 타입
- **MULTIPLE_CHOICE**: 객관식 (optionId 사용)
- **SUBJECTIVE**: 주관식 (content 사용)
- **SCALE**: 척도형 (content에 숫자 문자열, 현재 기능 제외)

### 인사이트 텍스트 예시
**객관식 인사이트 예시:**
- "정글과 미드가 응답자의 절반 이상을 차지하며, 원딜, 탑, 서포터는 동일한 비율로 나타났습니다."
- "A 옵션이 전체 응답의 60%를 차지하며 압도적으로 선호되고 있습니다."

**주관식 인사이트 예시:**
- "응답자들은 '매칭 시스템'에 대한 개선 요청이 가장 많았으며, 그 뒤를 '클라이언트 안정성'과 '버그 수정'에 대한 의견이 차지했습니다."
- "주요 키워드로는 '성능', '최적화', '안정성'이 자주 언급되었습니다."

### 요약 리포트 데이터 구조
**요청:** `GET /survey/:id/summary`

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "mainPosition": "정글",
    "mainPositionPercent": 45.0,
    "improvements": [
      "매칭 시스템의 실력 차이 완화 필요",
      "초반 AFK 플레이어 패널티 강화",
      "클라이언트 성능 최적화 필요"
    ]
  }
}
```

**참고:** 
- `mainPosition`은 가장 많이 선택된 객관식 옵션 (첫 번째 객관식 질문 기준 또는 특정 질문 ID 지정)
- `improvements`는 주관식 질문에서 추출한 개선 사항 (AI 분석 또는 키워드 기반)

### 질문별 통계 API 응답 구조
**요청:** `GET /survey/:id/questions/:questionId/statistics`

**객관식 응답:**
```json
{
  "success": true,
  "data": {
    "questionId": 1,
    "type": "MULTIPLE_CHOICE",
    "statistics": {
      "options": [
        {
          "optionId": 1,
          "optionContent": "정글",
          "count": 20,
          "percent": 28.5
        }
      ],
      "totalResponses": 70
    },
    "insight": "정글과 미드가 응답자의 절반 이상을 차지하며..."
  }
}
```

**주관식 응답:**
```json
{
  "success": true,
  "data": {
    "questionId": 2,
    "type": "SUBJECTIVE",
    "statistics": {
      "totalResponses": 38
    },
    "insight": "응답자들은 '매칭 시스템'에 대한 개선 요청이 가장 많았으며..."
  }
}
```

### 워드클라우드 API 응답 구조
**요청:** `GET /survey/:id/questions/:questionId/wordcloud`

**응답:**
```json
{
  "success": true,
  "data": {
    "questionId": 2,
    "wordCloud": [
      { "word": "매칭", "count": 12 },
      { "word": "클라이언트", "count": 8 },
      { "word": "버그", "count": 7 },
      { "word": "최적화", "count": 6 },
      { "word": "AFK", "count": 5 }
    ],
    "totalResponses": 38
  }
}
```

### 질문별 전체 응답 목록 API
**요청:** `GET /survey/:id/questions/:questionId/responses?page=1&limit=50`

**응답 (객관식):**
```json
{
  "success": true,
  "data": {
    "questionId": 1,
    "responses": [
      { "id": 1, "content": "정글" },
      { "id": 2, "content": "미드" },
      { "id": 3, "content": "정글" }
    ],
    "pagination": {
      "total": 70,
      "page": 1,
      "limit": 50,
      "totalPages": 2
    }
  }
}
```

**응답 (주관식):**
```json
{
  "success": true,
  "data": {
    "questionId": 2,
    "responses": [
      { "id": 1, "content": "매칭 시스템이 너무 불공정해요." },
      { "id": 2, "content": "클라이언트가 자주 튕깁니다." },
      { "id": 3, "content": "버그가 많아요." }
    ],
    "pagination": {
      "total": 38,
      "page": 1,
      "limit": 50,
      "totalPages": 1
    }
  }
}
```

### 에러 처리
- **400 Bad Request**: 잘못된 요청 (필수 파라미터 누락, 잘못된 데이터 타입 등)
- **401 Unauthorized**: 인증 실패 (토큰 없음, 만료됨)
- **403 Forbidden**: 권한 없음 (설문 소유자가 아닌 경우)
- **404 Not Found**: 리소스 없음 (설문 ID 또는 질문 ID 없음)
- **500 Internal Server Error**: 서버 내부 오류

**에러 응답 형식:**
```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null
}
```

### 캐싱 전략
- 설문 응답이 추가되기 전까지는 분석 결과 캐싱하여 재사용
- 새 응답이 추가되면 해당 설문의 모든 캐시 무효화
- 캐시 키: `survey:{surveyId}:summary`, `survey:{surveyId}:question:{questionId}:statistics` 등
- 캐시 TTL: 설정 가능 (기본값 1시간 권장)

### 성능 고려사항
- 대용량 설문(수천 건 응답) 처리 시 배치 처리 또는 비동기 작업 고려
- 워드클라우드 생성은 CPU 집약적이므로 백그라운드 작업으로 처리 권장
- 분석 결과는 데이터베이스나 캐시에 저장하여 재계산 방지

---

## 백엔드 개발자 검토 의견

### ✅ 잘 작성된 부분
1. 프론트엔드와의 협업을 위한 API 스펙이 명확하게 정의되어 있음
2. 점진적 개발 접근 방식이 잘 계획되어 있음
3. 에러 처리 및 폴백 전략이 고려되어 있음

### ⚠️ 수정/추가 필요 사항

#### 1. **질문별 통계 API 상태 명확화**
- 현재 `GET /survey/:id/questions/:questionId/statistics` API가 **아직 구현되지 않음**
- TODO에서 "수정"이 아닌 **"구현"**으로 명시 필요 (수정 완료)

#### 2. **Python AI 서버 연동 명시**
- 프로젝트에 Python AI 서버가 별도로 존재함 (LangChain, Hugging Face)
- HTTP 클라이언트 설정 및 통신 방식 명시 필요 (수정 완료)
- Python 서버 URL, 포트, 엔드포인트 정의 필요

#### 3. **설문 소유자 확인 로직**
- 모든 결과 조회 API에서 설문 소유자 확인 필수
- `Survey.userId`와 현재 사용자 비교 로직 구현 필요
- 현재 SurveyController에 "본인 권한 확인 필요" 주석 존재 (수정 완료)

#### 4. **Redis 캐싱 전략 명시**
- 프로젝트에 Redis가 이미 사용 중이므로 Redis 캐싱 전략 명시 필요 (수정 완료)
- RedisTemplate 또는 RedisCacheManager 활용 방법 제시

#### 5. **비동기 처리 설정**
- `@EnableAsync` 설정 및 `@Async` 활용 방법 명시 필요 (수정 완료)
- ThreadPoolTaskExecutor 설정 고려

#### 6. **데이터베이스 스키마 정확성**
- 실제 엔티티 구조와 일치하도록 스키마 정보 수정 (수정 완료)
- `INSIGHT_REPORTS` 테이블이 이미 존재함을 명시
- `RESPONSES` 테이블 구조가 프론트엔드 추정과 다름 (responseSessionId 사용)

#### 7. **API 응답 구조 정확성**
- `BaseResponseBody` AOP 사용으로 자동 래핑됨을 명시 (수정 완료)
- `timestamp` 필드 포함됨을 명시

#### 8. **에러 처리 통합**
- 기존 `GlobalExceptionHandler`와 통합 필요 (수정 완료)
- 커스텀 예외 클래스 정의 필요

#### 9. **데이터베이스 쿼리 최적화**
- 통계 집계 쿼리 인덱싱 필요
- N+1 쿼리 문제 해결 방안 명시 (수정 완료)

#### 10. **추가 고려사항**
- **질문별 응답 목록 API**: `GET /survey/:id/questions/:questionId/responses` 구현 필요 여부 확인
- **워드클라우드 API**: 주관식 질문에만 적용되므로 질문 타입 검증 필요 (수정 완료)
- **캐시 무효화**: 설문 응답 추가 시 이벤트 기반 캐시 삭제 로직 구현 필요
- **로깅**: 기존 로깅 시스템 활용 방법 명시 (수정 완료)

---

## Gemini API 연동 정보

### API 엔드포인트
- **URL**: `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent`
- **Method**: `POST`
- **Content-Type**: `application/json`

### 인증
- **헤더**: `X-goog-api-key: {API_KEY}`
- **API 키 설정**: `application.yml` 또는 환경변수로 관리
  ```yaml
  gemini:
    api-key: ${GEMINI_API_KEY:your-api-key-here}
  ```

### 요청 형식
```json
{
  "contents": [
    {
      "parts": [
        {
          "text": "프롬프트 텍스트"
        }
      ]
    }
  ]
}
```

### 응답 형식
Gemini API는 JSON 형식으로 응답하며, 생성된 텍스트는 `candidates[0].content.parts[0].text` 경로에 포함됩니다.

### 구현 시 고려사항
1. **WebClient 의존성 추가**: `build.gradle`에 `spring-boot-starter-webflux` 추가 필요
   ```gradle
   implementation 'org.springframework.boot:spring-boot-starter-webflux'
   ```
2. **타임아웃 설정**: 외부 API 호출이므로 적절한 타임아웃 설정 (기본 5초)
3. **재시도 로직**: 네트워크 오류 시 재시도 메커니즘 구현
4. **에러 처리**: API 키 오류, 할당량 초과, 서버 오류 등 다양한 에러 케이스 처리
5. **폴백 전략**: API 호출 실패 시 Java 템플릿 기반 인사이트로 폴백
6. **API 키 보안**: API 키는 환경변수로 관리하고, 코드에 하드코딩하지 않음

### 무료 티어 제한사항
- Gemini API 무료 티어의 요청 제한 확인 필요
- 할당량 초과 시 폴백 로직으로 Java 템플릿 기반 인사이트 사용

---

## 참고사항

1. **AI 서비스 선택 기준:**
   - **비용: 무료 우선** (포트폴리오 프로젝트 특성상 완전 무료 방식 채택)
   - **기본 전략: Java 단일 구현** - 모든 기능을 Java 백엔드에서 구현 (완전 무료, 안정적)
   - **Phase 1**: Java 자체 구현 (정규식 기반 키워드 추출, 템플릿 기반 인사이트)
   - **Phase 2**: 무료 AI API (**Gemini API** - Java에서 직접 호출, 응답 저장, 무료 티어 활용)
   - **Python 서버: 구축 안함** - 우선 구축하지 않기로 결정 (형태소 분석 등 고급 기능은 Java 템플릿으로 대체)
   - 성능: 응답 시간 및 처리량
   - 정확도: 한국어 처리 능력 (Phase 1은 정규식 기반, Phase 2에서 AI API로 개선)
   - 데이터 보안: 민감한 설문 데이터 처리 시 주의

2. **텍스트 분석 라이브러리:**
   - **Java 구현**: 정규식 기반 (`Pattern.compile("[가-힣]+")`) - 형태소 분석 없이 구현
   - **Python 서버**: 구축 안함 (형태소 분석 등 고급 기능은 Java 템플릿으로 대체)

3. **워드클라우드 데이터 포맷:**
   ```json
   {
     "wordCloud": [
       { "word": "매칭", "count": 12 },
       { "word": "클라이언트", "count": 8 },
       { "word": "버그", "count": 7 }
     ],
     "totalResponses": 38
   }
   ```

4. **캐싱 전략:**
   - 설문 응답이 추가되기 전까지는 분석 결과를 캐싱하여 재사용
   - 새 응답이 추가되면 해당 설문의 캐시 무효화
   - TTL 설정으로 일정 시간 후 자동 갱신

5. **점진적 개발 접근 (Java 우선):**
   - 1단계: 기본 키워드 추출 및 빈도수 집계 (Java - 정규식 기반)
   - 2단계: 템플릿 기반 인사이트 텍스트 생성 (Java)
   - 3단계: AI 기반 자연어 생성 (Java에서 무료 AI API 직접 호출, 응답 저장, 선택사항)
   - Python 서버 구축 안함 (형태소 분석 등 고급 기능은 Java 템플릿으로 대체)

6. **백엔드 개발 시 주의사항:**
   - **설문 소유자 확인**: 모든 결과 조회 API에서 `Survey.userId`와 현재 사용자 비교 필수
   - **트랜잭션 관리**: 통계 집계 시 `@Transactional(readOnly = true)` 사용
   - **N+1 쿼리 방지**: JPA JOIN FETCH 또는 @EntityGraph 활용
   - **캐시 무효화**: 설문 응답 추가 시 Redis 캐시 삭제 로직 구현
   - **AI API 통신**: Java에서 WebClient 사용하여 **Gemini API** 직접 호출 (Phase 2 선택사항)
     - **Gemini API 엔드포인트**: `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent`
     - **인증 방식**: `X-goog-api-key` 헤더에 API 키 전달
     - **요청 형식**: `{"contents": [{"parts": [{"text": "프롬프트 텍스트"}]}]}`
     - **API 키 설정**: `application.yml`에 `gemini.api-key` 환경변수로 관리
   - **AI API 응답 저장**: 외부 AI API 호출 결과를 DB 또는 Redis에 저장하여 재사용
   - **Python 서버**: 구축 안함 (형태소 분석 등 고급 기능은 Java 템플릿으로 대체)
   - **에러 처리**: 기존 `GlobalExceptionHandler`에 AI 관련 예외 추가
   - **로깅**: 기존 로깅 시스템(`LoggingInterceptor`, `ExceptionLoggingFilter`) 활용
   - **키워드 추출**: 정규식 기반으로 구현 (`Pattern.compile("[가-힣]+")`), 형태소 분석 없이 진행

