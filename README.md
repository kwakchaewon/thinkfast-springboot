# 🚀 Think Fast (띵패스트)

> AI 기반 인사이트를 제공하는 실시간 설문조사 플랫폼

Think Fast는 실시간 설문 조사, AI 기반 인사이트 분석, 실시간 알림 등의 서비스를 제공하는 웹 애플리케이션입니다.

---

## 📋 목차

- [프로젝트 개요](#-프로젝트-개요)
- [기술 스택 및 선택 이유](#-기술-스택-및-선택-이유)
- [시스템 아키텍처](#-시스템-아키텍처)
- [핵심 기능 및 기술적 구현](#-핵심-기능-및-기술적-구현)
- [성능 최적화 전략](#-성능-최적화-전략)
- [주요 기술적 의사결정](#-주요-기술적-의사결정)

---

## 🎯 프로젝트 개요

### 주요 기능
- **실시간 설문 조사**: 비회원 참여 가능, 중복 응답 방지
- **AI 기반 인사이트 분석**: Gemini API를 활용한 요약 리포트, 워드클라우드, 질문별 인사이트 자동 생성
- **실시간 알림**: WebSocket + Redis Pub/Sub 기반 실시간 알림 시스템
- **자동화된 통계 업데이트**: 스케줄러 기반 실시간 통계 업데이트

### 개발 기간
- 2024년 (개인 프로젝트)

### Frontend Repository
- [thinkfast-vue3](https://github.com/kwakchaewon/thinkfast-vue3)

---

## 🛠 기술 스택 및 선택 이유

### Backend
| 기술 | 버전 | 선택 이유 |
|------|------|----------|
| **Spring Boot** | 2.5.8 | 빠른 개발, 풍부한 생태계, 엔터프라이즈급 안정성 |
| **Java** | 8 | 안정성과 성숙도, 넓은 커뮤니티 지원 |
| **Spring Security + JWT** | - | Stateless 인증, 확장 가능한 보안 아키텍처 |
| **MariaDB** | 10.x | MySQL 호환성, 오픈소스, 안정적인 성능 |
| **Redis** | 6.x | Pub/Sub 메시징, 캐싱, 빠른 I/O 성능 |
| **Flyway** | 7.1.1 | 버전 관리된 DB 마이그레이션, 자동화된 스키마 관리 |
| **Spring WebFlux** | - | 비동기 AI API 호출, 논블로킹 I/O |

### AI/LLM
| 기술 | 선택 이유 |
|------|----------|
| **Google Gemini 2.0 Flash** | 무료 티어 제공, 빠른 응답 속도, Java에서 직접 호출 가능 |
| **Java 단일 구현** | Python 서버 없이 Java 백엔드에서 직접 AI API 호출하여 인프라 단순화 |

### Infrastructure
- **Docker**: 컨테이너화를 통한 배포 표준화
- **AWS EC2**: 클라우드 인프라
- **Jenkins**: CI/CD 파이프라인

---

## 🏗 시스템 아키텍처

### 전체 구조
```
┌─────────────┐
│   Client    │ (Vue 3 + TypeScript)
└──────┬──────┘
       │ HTTP/WebSocket
┌──────▼──────────────────────────────────────┐
│         Spring Boot Application             │
│  ┌──────────────────────────────────────┐   │
│  │  Controller Layer                   │   │
│  │  - AuthController                    │   │
│  │  - SurveyController                  │   │
│  └──────────┬───────────────────────────┘   │
│  ┌──────────▼───────────────────────────┐   │
│  │  Service Layer                       │   │
│  │  - SurveyService                     │   │
│  │  - AI Services (Summary, WordCloud)  │   │
│  └──────────┬───────────────────────────┘   │
│  ┌──────────▼───────────────────────────┐   │
│  │  Repository Layer                    │   │
│  └──────────┬───────────────────────────┘   │
└──────┬──────────────────────────────────────┘
       │
   ┌───┴────┬──────────┬──────────┐
   │        │          │          │
┌──▼──┐ ┌──▼──┐  ┌───▼───┐  ┌───▼───┐
│Maria│ │Redis│  │Gemini │  │WebSocket│
│ DB  │ │Pub/ │  │ API   │  │        │
└─────┘ │Sub  │  └───────┘  └────────┘
        └─────┘
```

### 실시간 알림 아키텍처
```
응답 생성 → RedisPublisher → Redis Pub/Sub Channel
                                    ↓
                            RedisSubscriber
                                    ↓
                            WebSocket Handler
                                    ↓
                            연결된 모든 클라이언트에게 브로드캐스트
```

---

## 💡 핵심 기능

### 1. 실시간 알림 시스템 (WebSocket + Redis Pub/Sub)

- **WebSocket 단독 사용의 한계**: 단일 서버 인스턴스에서만 동작, 수평 확장 불가
- **Redis Pub/Sub 도입**: 여러 서버 인스턴스 간 메시지 브로드캐스트 가능

```java
// RedisPublisher: 이벤트 발생 시 Redis 채널에 메시지 발행
public void sendAlarm(Long surveyId, String type) {
    AlarmMessage alarmMessage = new AlarmMessage(...);
    String json = objectMapper.writeValueAsString(alarmMessage);
    redisTemplate.convertAndSend("alarm-channel", json);
}

// RedisSubscriber: Redis 채널 구독 및 WebSocket으로 전달
public void onMessage(String message, Pattern pattern) {
    // WebSocket 세션에 메시지 전달
    webSocketHandler.sendToAll(message);
}
```

---

### 2. 중복 응답 방지 전략 수립 (DeviceId + IP 해시화)

- 중복 응답 방지를 위해 응답 저장 시점에서  DeviceId + IP 해시값 비교
- **SHA256 해시화**: 원본 데이터 보호하면서 중복 체크 가능
- **유연한 체크 로직**: DeviceId와 IP 중 하나만 있어도 체크 가능

```java
// SHA256 해시화
String deviceIdHash = HashUtil.encodeSha256(deviceId);
String ipAddressHash = HashUtil.encodeSha256(ipAddress);

// 중복 체크: DeviceId + IP 조합 또는 개별 체크
if (deviceId != null && ipAddress != null) {
    // 조합 해시로 체크
} else if (deviceId != null) {
    // DeviceId만으로 체크
} else if (ipAddress != null) {
    // IP만으로 체크
}
```



---

### 3. 배치 처리 최적화 (스케줄러)

**문제점:**
- 초기 구현: 설문별로 개별 쿼리 실행 → N+1 문제
- 100개 설문 처리 시 100번 이상의 DB 쿼리 발생

**최적화 전략:**
```java
// Before: N+1 문제
for (Survey survey : surveys) {
    List<Question> questions = questionRepository.findBySurveyId(survey.getId());
    Long count = responseRepository.countBySurveyId(survey.getId());
}

// After: 배치 처리
List<Long> surveyIds = surveys.stream().map(Survey::getId).collect(toList());
List<Question> allQuestions = questionRepository.findBySurveyIdIn(surveyIds); // 한 번에 조회
List<Object[]> responseCounts = responseRepository.countDistinctResponseSessionsBySurveyIds(surveyIds); // 한 번에 조회
```

**성능 개선:**
- 쿼리 수: O(N) → O(1)
- 100개 설문 처리 시: 200+ 쿼리 → 2-3 쿼리로 감소

---

### 4. 비동기 AI 리포트 생성
- **동기 처리의 문제**: AI API 호출 시 응답 시간 5-30초, 사용자 대기 시간 증가
- **@Async 비동기 처리**: 리포트 생성은 백그라운드에서 처리, API는 즉시 응답
- API 응답 시간 감소: 5-30초 → < 100ms

```java
@Async("taskExecutor")
@Transactional
public void saveSummaryReportAsync(Long surveyId) {
    // 1. 리포트 생성 (시간 소요)
    SummaryReportDto summary = generateSummaryReport(surveyId);
    
    // 2. JSON 직렬화
    String summaryText = objectMapper.writeValueAsString(summary);
    
    // 3. DB 저장
    insightReportRepository.save(report);
}

// API 호출 시: DB에 저장된 리포트만 조회 (빠른 응답)
public SummaryReportDto getSummaryReport(Long surveyId) {
    return insightReportRepository.findBySurveyId(surveyId)
        .map(report -> deserialize(report.getSummaryText()))
        .orElse(emptyReport());
}
```

---

### 5. 실시간 통계 업데이트 스케줄러
- **진행 중인 설문**: 설문 종료 전에도 실시간으로 통계 업데이트 필요
- **1분 간격 스케줄링**: 실시간성과 서버 부하의 균형

**구현 방식:**
```java
@Scheduled(fixedRate = 60000) // 1분마다 실행
public void updateActiveSurveyReports() {
    // 1. 진행 중인 설문 조회 (배치)
    List<Survey> activeSurveys = surveyRepository.findActiveSurveysByEndTimeAfter(now);
    
    // 2. 응답 수 배치 조회
    List<Object[]> responseCounts = responseRepository
        .countDistinctResponseSessionsBySurveyIds(surveyIds);
    
    // 3. 각 설문에 대해 비동기로 리포트 업데이트
    targetSurveyIds.forEach(surveyId -> {
        summaryService.saveSummaryReportAsync(surveyId);
        wordCloudService.saveWordCloudsForSurveyAsync(surveyId);
        insightService.saveInsightsForSurveyAsync(surveyId);
    });
}
```

**효과:**
- 진행 중인 설문도 실시간으로 통계 확인 가능
- 배치 처리로 서버 부하 최소화

---

### 6. AI 서비스 통합 (Gemini API)

**기술적 의사결정:**
- **Python 서버 vs Java 직접 호출**: Java 단일 구현 선택
  - 이유: 인프라 단순화, 배포 복잡도 감소, 유지보수 용이
- **WebFlux 사용**: 비동기 논블로킹 I/O로 AI API 호출
- **타임아웃 설정**: 30초 타임아웃으로 무한 대기 방지

**구현 방식:**
```java
@Bean
public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .responseTimeout(Duration.ofSeconds(30))
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000);
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}

// Gemini API 호출
public String generateInsight(String prompt) {
    return webClient.post()
        .uri(geminiApiUrl)
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(String.class)
        .block();
}
```

**효과:**
- 단일 언어 스택으로 개발/운영 단순화
- 비동기 처리로 성능 최적화

---

## ⚡ 성능 최적화 전략

### 1. 배치 처리
- **문제**: N+1 쿼리 문제
- **해결**: `IN` 절을 활용한 배치 조회
- **효과**: 쿼리 수 90% 이상 감소

### 2. 비동기 처리
- **@Async**: AI 리포트 생성, 통계 업데이트
- **효과**: API 응답 시간 대폭 단축

### 3. DB 인덱싱
- 설문 ID, 질문 ID, 응답 세션 ID 등에 인덱스 적용
- 복합 인덱스로 조회 성능 향상

### 4. 스케줄러 최적화
- 1분 간격으로 배치 처리
- 진행 중인 설문만 필터링하여 불필요한 처리 방지

---

## 🎯 주요 의사결정 내역

### 1. Redis Pub/Sub vs 메시지 큐
**선택: Redis Pub/Sub**
- **이유**: 
  - 실시간 알림은 일회성 이벤트 (큐 저장 불필요)
  - 단순한 구조로 구현 용이
  - 이미 Redis를 캐싱용으로 사용 중
- **대안 고려**: RabbitMQ, Kafka (복잡도 대비 이점 적음)

### 2. JWT Stateless 인증
**선택: JWT 토큰 기반 인증**
- **이유**:
  - 서버 확장성 (세션 저장소 불필요)
  - 마이크로서비스 아키텍처에 적합
  - Refresh Token으로 보안 강화

### 3. Flyway DB 마이그레이션
**선택: Flyway**
- **이유**:
  - 버전 관리된 스키마 변경
  - 자동화된 마이그레이션
  - 롤백 지원

### 4. Java 단일 구현 (AI 서비스)
**선택: Python 서버 없이 Java에서 직접 AI API 호출**
- **이유**:
  - 인프라 단순화
  - 배포 복잡도 감소
  - 유지보수 용이
- **트레이드오프**: 형태소 분석 등 고급 NLP 기능 제한 (템플릿 기반으로 대체)

### 5. 비동기 리포트 생성
**선택: @Async + DB 저장 방식**
- **이유**:
  - API 응답 시간 단축
  - 사용자 경험 개선
  - AI API 호출 실패 시에도 기존 리포트 제공 가능

---

## 📊 프로젝트 구조

```
thinkfast/
├── src/main/java/com/example/thinkfast/
│   ├── controller/          # REST API 엔드포인트
│   ├── service/             # 비즈니스 로직
│   │   ├── ai/              # AI 서비스 (Summary, WordCloud, Insight)
│   │   ├── auth/            # 인증 서비스
│   │   └── survey/          # 설문 서비스
│   ├── repository/          # 데이터 접근 계층
│   ├── domain/              # 엔티티
│   ├── dto/                 # 데이터 전송 객체
│   ├── security/            # 보안 설정 (JWT, Spring Security)
│   ├── realtime/            # 실시간 알림 (WebSocket + Redis)
│   └── scheduler/           # 스케줄러 (통계 업데이트)
└── src/main/resources/
    └── db/migration/        # Flyway 마이그레이션 스크립트
```

---

## 🚀 배포

### Docker 빌드
```bash
docker build -t thinkfast:latest .
docker run -d -p 8080:8080 thinkfast:latest
```

### 환경 변수
- `SPRING_PROFILES_ACTIVE`: prod
- `SPRING_DATASOURCE_URL`: MariaDB 연결 정보
- `GEMINI_API_KEY`: Gemini API 키

---

## 📝 향후 개선 계획

### Phase 1: 핵심 개선
- [ ] Redis 캐싱 전략 도입
- [ ] 필수 질문 검증 기능
- [ ] 알람 DB 저장 및 조회 API

### Phase 2: 기능 확장
- [ ] LLM 기반 동적 분석 (질문 유형별 맞춤 분석)
- [ ] 중복 응답 방지 강화 (쿠키 기반 세션)
- [ ] Bulk Insert 적용

### Phase 3: 고급 기능
- [ ] 리포트 버전 관리
- [ ] 질문 간 상관관계 분석
- [ ] 응답 품질 검증 (이상치 탐지)

---

## 📄 라이선스

이 프로젝트는 개인 프로젝트입니다.

---

**Think Fast** - 빠르고 스마트한 설문조사 플랫폼 🚀
