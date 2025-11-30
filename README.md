# 🚀 Think Fast (띵패스트)

> AI 기반 인사이트 제공.  실시간 설문조사 플랫폼

Think Fast는 실시간 설문 조사, 알림, AI 기반 인사이트 분석 등의 서비스를 제공하는 웹 애플리케이션입니다.

---

## 📋 목차

- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [프로젝트 구조](#-프로젝트-구조)
- [시작하기](#-시작하기)
- [API 문서](#-api-문서)
- [데이터베이스](#-데이터베이스)
- [배포](#-배포)
- [개발 가이드](#-개발-가이드)

---

<br></br>
## ✨ 주요 기능

### 🔐 인증/인가
- **JWT 기반 인증**: Access Token 및 Refresh Token 발급
- **Spring Security**: 역할 기반 접근 제어 (CREATOR, RESPONDER)
- **회원가입/로그인**: 필드별 유효성 검사 및 중복 체크
- **프로필 관리**: 사용자 정보 조회 및 수정
<br></br>

### 📊 설문 관리
- **설문 CRUD**: 설문 생성, 조회, 삭제
- **질문 유형 지원**:
  - 객관식 (MULTIPLE_CHOICE)
  - 주관식 (SUBJECTIVE)
  - 척도형 (SCALE)
- **설문 설정**:
  - 시작/종료 시간 설정
  - 공개/비공개 설정
  - 결과 공개 여부 설정
- **공개 설문 목록**: 인증 없이 조회 가능, 검색 및 정렬 지원

### 📝 설문 응답
- **비회원 참여**: 회원가입 없이 설문 응답 가능
- **중복 응답 방지**: DeviceId + IP Address 기반 중복 체크 (SHA256 해시화)
- **응답 세션 관리**: UUID 기반 응답 그룹화
- **응답 조회**: 페이징 지원 (기본 10개, 최대 100개)

### 📈 AI 기반 인사이트 분석
- **요약 리포트**: 설문 전체 요약 및 주요 인사이트
  - 주요 선택지 및 비율 분석
  - 주관식 응답 기반 인사이트 추출
- **워드클라우드**: 주관식 질문의 키워드 시각화
- **질문별 인사이트**: 객관식/주관식 질문별 맞춤 분석
- **실시간 통계**: 질문별 응답 통계 (객관식: 선택지별 비율, 주관식: 응답 수)

### 🔔 실시간 알림
- **WebSocket + Redis Pub/Sub**: 실시간 알림 전송
- **알림 유형**:
  - 설문 응답 생성 알림
  - 설문 종료 알림
- **멀티 쓰레드 환경 지원**: 동시 접속자에게 실시간 알림 전달

### ⏰ 스케줄링
- **설문 종료 자동 처리**: 1분마다 종료된 설문 비활성화
- **실시간 통계 업데이트**: 진행 중인 설문 1분마다 자동 업데이트
  - 요약 리포트 업데이트
  - 워드클라우드 업데이트
  - 인사이트 텍스트 업데이트
- **배치 처리 최적화**: 여러 설문의 질문/응답 수를 한 번에 조회

---

## 🛠 기술 스택

### Backend
- **Framework**: Spring Boot 2.5.8
- **Language**: Java 8
- **Build Tool**: Gradle 7.6.3
- **Security**: Spring Security + JWT (jjwt 0.11.5)
- **Database**: 
  - MariaDB (메인 데이터베이스)
  - Redis (캐싱 및 Pub/Sub)
- **Migration**: Flyway 7.1.1
- **WebSocket**: Spring WebSocket
- **Reactive**: Spring WebFlux (AI 서비스 통신)

### AI/LLM
- **Gemini API**: Google Gemini 2.0 Flash 모델 사용
- **기능**: 
  - 요약 리포트 생성
  - 인사이트 추출
  - 워드클라우드 데이터 생성

### Frontend
- **Framework**: Vue 3 + TypeScript
- **Repository**: [thinkfast-vue3](https://github.com/kwakchaewon/thinkfast-vue3)

### Infrastructure
- **Containerization**: Docker
- **CI/CD**: Github Actions & Jenkins
- **Cloud**: AWS EC2

---

## 📁 프로젝트 구조

```
thinkfast/
├── src/
│   ├── main/
│   │   ├── java/com/example/thinkfast/
│   │   │   ├── common/          # 공통 유틸리티 및 설정
│   │   │   │   ├── aop/         # AOP (응답 래핑 등)
│   │   │   │   ├── config/      # 설정 클래스
│   │   │   │   ├── exception/   # 예외 처리
│   │   │   │   ├── logger/      # 로깅 유틸리티
│   │   │   │   └── utils/       # 유틸리티 클래스
│   │   │   ├── controller/      # REST API 컨트롤러
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── SurveyController.java
│   │   │   │   ├── NotificationController.java
│   │   │   │   └── TestController.java
│   │   │   ├── domain/          # 엔티티
│   │   │   │   ├── ai/          # AI 관련 엔티티
│   │   │   │   ├── auth/        # 인증 관련 엔티티
│   │   │   │   └── survey/      # 설문 관련 엔티티
│   │   │   ├── dto/             # 데이터 전송 객체
│   │   │   │   ├── ai/
│   │   │   │   ├── auth/
│   │   │   │   └── survey/
│   │   │   ├── repository/      # 데이터 접근 계층
│   │   │   │   ├── ai/
│   │   │   │   ├── auth/
│   │   │   │   └── survey/
│   │   │   ├── service/         # 비즈니스 로직
│   │   │   │   ├── ai/          # AI 서비스
│   │   │   │   ├── auth/        # 인증 서비스
│   │   │   │   └── survey/     # 설문 서비스
│   │   │   ├── security/       # 보안 설정
│   │   │   ├── realtime/       # 실시간 알림 (WebSocket + Redis)
│   │   │   ├── scheduler/      # 스케줄러 (설문 종료 처리 등)
│   │   │   └── ThinkfastApplication.java
│   │   └── resources/
│   │       ├── application.yml          # 기본 설정
│   │       ├── application-local.yml    # 로컬 환경 설정
│   │       ├── application-prod.yml     # 프로덕션 환경 설정
│   │       └── db/migration/            # Flyway 마이그레이션 스크립트
│   └── test/                    # 테스트 코드
├── build.gradle                 # Gradle 빌드 설정
├── settings.gradle              # Gradle 프로젝트 설정
├── Dockerfile                   # Docker 이미지 빌드 설정
├── gradlew                      # Gradle Wrapper (Unix)
├── gradlew.bat                  # Gradle Wrapper (Windows)
└── README.md                    # 프로젝트 문서
```

---

## 🚀 시작하기

### 사전 요구사항
- Java 8 이상
- Gradle 7.6.3 이상 (또는 Gradle Wrapper 사용)
- MariaDB 10.x 이상
- Redis 6.x 이상

### 로컬 환경 설정

1. **저장소 클론**
```bash
git clone <repository-url>
cd thinkfast
```

2. **데이터베이스 설정**
   - MariaDB 데이터베이스 생성:
   ```sql
   CREATE DATABASE thinkfast CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
   - Redis 서버 실행

3. **환경 변수 설정**
   - `src/main/resources/application-local.yml` 파일 수정:
   ```yaml
   spring:
     datasource:
       url: jdbc:mariadb://localhost:3306/thinkfast
       username: your_username
       password: your_password
     flyway:
       url: jdbc:mariadb://localhost:3306/thinkfast?characterEncoding=UTF-8&serverTimezone=UTC
       user: your_username
       password: your_password
   
   jwt:
     secret: your-secret-key-should-be-very-long-and-secure-at-least-256-bits
   
   gemini:
     api-key: your-gemini-api-key
   ```

4. **애플리케이션 실행**
```bash
# Windows
gradlew.bat bootRun

# Unix/Mac
./gradlew bootRun
```

5. **애플리케이션 접속**
   - 서버 주소: `http://localhost:8080`

### Docker를 사용한 실행

1. **Docker 이미지 빌드**
```bash
docker build -t thinkfast:latest .
```

2. **Docker 컨테이너 실행**
```bash
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:mariadb://host.docker.internal:3306/thinkfast \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  thinkfast:latest
```

---

## 📚 API 문서

### 인증 API

#### 회원가입
```
POST /auth/signup
Content-Type: application/json

{
  "username": "string",
  "password": "string",
  "email": "string",
  "realUsername": "string"
}
```

#### 로그인
```
POST /auth/login
Content-Type: application/json

{
  "username": "string",
  "password": "string"
}

Response:
{
  "accessToken": "string",
  "refreshToken": "string"
}
```

#### 토큰 갱신
```
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "string"
}
```

#### 로그아웃
```
POST /auth/logout
Authorization: Bearer {accessToken}
```

#### 프로필 조회
```
GET /auth/me
Authorization: Bearer {accessToken}
```

#### 프로필 수정
```
PUT /auth/profile
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "email": "string",
  "realUsername": "string"
}
```

### 설문 API

#### 설문 생성
```
POST /survey
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "title": "string",
  "description": "string",
  "startTime": "2024-01-01T00:00:00",
  "endTime": "2024-01-31T23:59:59",
  "showResults": true,
  "questions": [
    {
      "type": "MULTIPLE_CHOICE",
      "content": "string",
      "orderIndex": 1,
      "options": [
        {
          "content": "string"
        }
      ]
    }
  ]
}
```

#### 설문 목록 조회
```
GET /survey
Authorization: Bearer {accessToken}
```

#### 공개 설문 목록 조회
```
GET /survey/public?page=1&size=10&sort=newest&search=keyword
```

#### 설문 상세 조회
```
GET /survey/{id}
Authorization: Bearer {accessToken}
```

#### 설문 삭제
```
DELETE /survey/{id}
Authorization: Bearer {accessToken}
```

#### 설문 질문 목록 조회
```
GET /survey/{surveyId}/questions
```

### 응답 API

#### 설문 응답 제출
```
POST /survey/{surveyId}/responses
Content-Type: application/json

{
  "clientInfo": {
    "deviceId": "string"
  },
  "answers": [
    {
      "questionId": 1,
      "type": "MULTIPLE_CHOICE",
      "optionId": 2
    },
    {
      "questionId": 2,
      "type": "SUBJECTIVE",
      "content": "응답 내용"
    }
  ]
}
```

#### 질문별 응답 조회
```
GET /survey/{surveyId}/questions/{questionId}/responses?page=1&size=10
```

### AI 인사이트 API

#### 요약 리포트 조회
```
GET /survey/{id}/summary
Authorization: Bearer {accessToken}  # 비공개 설문의 경우 필수
```

#### 워드클라우드 조회
```
GET /survey/{surveyId}/questions/{questionId}/wordcloud
Authorization: Bearer {accessToken}  # 비공개 설문의 경우 필수
```

#### 인사이트 조회
```
GET /survey/{surveyId}/questions/{questionId}/insight
Authorization: Bearer {accessToken}  # 비공개 설문의 경우 필수
```

#### 질문별 통계 조회
```
GET /survey/{surveyId}/questions/{questionId}/statistics
Authorization: Bearer {accessToken}  # 비공개 설문의 경우 필수
```

---

## 🗄 데이터베이스

### 주요 테이블

- **USERS**: 사용자 정보
- **SURVEYS**: 설문 정보
- **QUESTIONS**: 질문 정보
- **OPTIONS**: 객관식 선택지
- **RESPONSES**: 응답 데이터
- **SURVEY_RESPONSE_HISTORY**: 응답 이력 (중복 방지)
- **INSIGHT_REPORTS**: 요약 리포트
- **WORD_CLOUDS**: 워드클라우드 데이터
- **QUESTION_INSIGHTS**: 질문별 인사이트
- **NOTIFICATIONS**: 알림 정보

### 마이그레이션

Flyway를 사용하여 데이터베이스 스키마를 관리합니다.

- 마이그레이션 스크립트 위치: `src/main/resources/db/migration/`
- 마이그레이션 파일 명명 규칙: `V{version}__{description}.sql`
- 예: `V1__create_user_tables.sql`

애플리케이션 실행 시 자동으로 마이그레이션이 수행됩니다.

---

## 🚢 배포

### 빌드

```bash
# JAR 파일 빌드
./gradlew clean bootJar

# 빌드된 JAR 위치
# build/libs/thinkfast-0.0.1-SNAPSHOT.jar
```

### Docker 배포

```bash
# Docker 이미지 빌드
docker build -t thinkfast:latest .

# Docker 컨테이너 실행
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:mariadb://{db-host}:3306/thinkfast \
  -e SPRING_DATASOURCE_USERNAME={username} \
  -e SPRING_DATASOURCE_PASSWORD={password} \
  thinkfast:latest
```

### 프로덕션 환경 설정

`application-prod.yml` 파일에 프로덕션 환경 설정을 추가하세요:

```yaml
spring:
  datasource:
    url: jdbc:mariadb://{prod-db-host}:3306/thinkfast
    username: {prod-username}
    password: {prod-password}
  
  redis:
    host: {redis-host}
    port: 6379

jwt:
  secret: {prod-secret-key}

gemini:
  api-key: {prod-api-key}
```

---

## 💻 개발 가이드

### 코드 스타일
- Java 8 문법 준수
- Lombok 사용 (Getter, Setter, Builder 등)
- 명확한 변수명 및 메서드명 사용

### 로깅
- SLF4J + Logback 사용
- 로그 레벨: DEBUG (개발), INFO (운영)
- 구조화된 로그 메시지 작성

### 예외 처리
- 커스텀 예외 클래스 사용 (`AiServiceException`, `NoResponseException` 등)
- `@ControllerAdvice`를 통한 전역 예외 처리
- 일관된 에러 응답 형식 (`BaseResponse`)

### 비동기 처리
- `@Async` 어노테이션 사용
- AI 서비스 호출, 리포트 생성 등 시간이 오래 걸리는 작업에 적용

### 보안
- JWT 토큰 기반 인증
- HttpOnly Cookie 사용 (HTTPS only)
- IP 주소 및 DeviceId 해시화 (SHA256)
- 역할 기반 접근 제어 (`@PreAuthorize`)

### 성능 최적화
- 배치 처리: 여러 설문의 질문/응답 수를 한 번에 조회
- 비동기 처리: 리포트 생성, AI 서비스 호출
- 스케줄러: 1분마다 자동 업데이트

---

## 📝 향후 개선 계획

### Phase 1: 핵심 개선
- [ ] 필수 질문 검증 기능 추가
- [ ] 응답 데이터 검증 강화
- [ ] 알람 DB 저장 및 조회 API 구현
- [ ] Redis 캐싱 전략 도입

### Phase 2: 기능 확장
- [ ] LLM 기반 동적 분석 (질문 유형별 맞춤 분석)
- [ ] 중복 응답 방지 강화 (쿠키 기반 세션 ID)
- [ ] 응답 메타데이터 수집
- [ ] Bulk Insert 적용

### Phase 3: 고급 기능
- [ ] 리포트 버전 관리
- [ ] 질문 간 상관관계 분석
- [ ] 응답자 인증 옵션
- [ ] 응답 품질 검증 (이상치 탐지)

자세한 내용은 [task.md](./task.md) 파일을 참고하세요.

---

## 📄 라이선스

이 프로젝트는 개인 프로젝트입니다.

---

## 👥 기여자

- 프로젝트 관리자: [GitHub 프로필]

---

## 📞 문의

프로젝트 관련 문의사항이 있으시면 이슈를 등록해주세요.

---

**Think Fast** - 빠르고 스마트한 설문조사 플랫폼 🚀
