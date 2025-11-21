# 로깅 시스템 구조 및 ElasticSearch + Kibana 전환 작업

## 📋 현재 로깅 시스템 구조

### 1. 로깅 프레임워크

- **SLF4J**: 로깅 추상화 계층
- **Logback**: Spring Boot 2.5.8 기본 제공 (별도 의존성 불필요)
- **사용 방식**:
  - `@Slf4j` (Lombok): `SurveySchedule`, `JwtTokenProvider`
  - `LoggerFactory.getLogger()`: 다른 컴포넌트들

### 2. 로깅 컴포넌트 구조

#### 2.1 필터 레벨 - ExceptionLoggingFilter
- **위치**: `com.example.thinkfast.common.logger.ExceptionLoggingFilter`
- **역할**: 서블릿 필터 체인에서 예외 감지 및 로깅
- **동작**:
  - `doFilterInternal`에서 예외 캐치 (`Exception` 타입만)
  - 401, 403, 500+ 상태 코드 로깅
  - 중복 로깅 방지 (`exceptionLogged` 속성 사용)
- **특징**: `@Component`로 자동 등록, `OncePerRequestFilter` 상속
- **현재 로그 포맷**: 
  ```
  [EXCEPTION] {method} {uri} => Exception: {message}
  [EXCEPTION] {method} {uri} => Status: {status}
  ```

#### 2.2 인터셉터 레벨 - LoggingInterceptor
- **위치**: `com.example.thinkfast.common.logger.LoggingInterceptor`
- **등록**: `WebConfig`에서 인터셉터로 등록
- **역할**: HTTP 요청/응답 로깅
- **로깅 내용**:
  - `preHandle`: 요청 시작 시간 기록, `[REQUEST]` 로깅
  - `afterCompletion`: 응답 시간 측정, `[RESPONSE]` 로깅, 예외 발생 시 `[EXCEPTION]` 로깅
- **현재 로그 포맷**:
  ```
  [REQUEST] {method} {uri} from {remoteAddr}
  [RESPONSE] {method} {uri} => {status} ({duration}ms)
  [EXCEPTION] {method} {uri} => Exception: {message}
  ```

#### 2.3 예외 처리 레벨 - GlobalExceptionHandler
- **위치**: `com.example.thinkfast.common.aop.GlobalExceptionHandler`
- **역할**: 전역 예외 처리 및 로깅
- **특징**: `@RestControllerAdvice`로 모든 컨트롤러 예외 처리
- **현재 로그 포맷**:
  ```
  [EXCEPTION] {method} {uri}
  ```

### 3. 애플리케이션별 로깅 사용 현황

#### 3.1 JWT 토큰 (JwtTokenProvider)
- **로그 타입**: `log.warn()`
- **내용**: 만료된 토큰, 잘못된 토큰 경고
- **현재 포맷**: 
  ```
  만료된 JWT 토큰입니다. 만료 시간: {expiration}, 현재 시간: {now}
  잘못된 JWT 토큰입니다.
  ```

#### 3.2 스케줄러 (SurveySchedule)
- **로그 타입**: `log.info()`, `log.error()`
- **내용**: 스케줄 실행, 설문 업데이트, 예외 처리
- **현재 포맷**:
  ```
  [SCHEDULER] SURVEY CHECK
  [UPDATE] survey update unactive - ID: {id}, 제목: {title}, 종료일: {date}
  스케줄러 실행 중 오류 발생
  ```

#### 3.3 WebSocket (AlarmHandler)
- **로그 타입**: `log.info()`
- **내용**: WebSocket 연결/해제 이벤트
- **현재 포맷**:
  ```
  [WEBSOCKET] CONNECTED {username} => sessionId: {sessionId}
  [WEBSOCKET] DISCONNECTED {username} => sessionId: {sessionId}, reason: {reason}
  ```
- **⚠️ 문제점**: Logger 클래스 참조 오류 (`LoggingInterceptor.class` 사용 중)

### 4. 로깅 설정

#### 4.1 application-local.yml / application-prod.yml
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```
- **Hibernate SQL 로깅**: DEBUG 레벨
- **SQL 파라미터 바인딩**: TRACE 레벨
- **로그 파일 출력 설정**: 없음 (콘솔 출력만)
- **JSON 포맷 설정**: 없음

### 5. 현재 시스템의 문제점

#### 5.1 구조화되지 않은 로그 포맷
- ❌ 단순 텍스트 문자열로 로깅
- ❌ 검색 가능한 필드 구조 없음
- ❌ ElasticSearch 인덱싱 최적화 불가

#### 5.2 예외 처리 범위 제한
- ❌ `Exception` 타입만 캐치 (Error 타입 누락)
- ❌ 비동기 작업 예외 처리 없음
- ❌ UncaughtExceptionHandler 없음

#### 5.3 메타데이터 부족
- ❌ 서비스명, 환경 정보 없음
- ❌ 호스트명/IP 정보 없음
- ❌ 트레이스 ID, Request ID 없음
- ❌ 사용자 ID 정보 없음

#### 5.4 로그 파일 출력 설정 없음
- ❌ Filebeat 연동 불가
- ❌ 로그 파일 경로 설정 없음

#### 5.5 코드 레벨 문제
- ❌ `LoggingInterceptor.afterCompletion`에서 `startTime` null 체크 없음
- ❌ `AlarmHandler`에서 잘못된 Logger 클래스 참조

---

## 🎯 Kibana + ElasticSearch 전환을 위한 ToDo 리스트

### Phase 1: 기반 설정 및 인프라 구축 (우선순위: 높음)

#### 1.1 Logback JSON Encoder 설정
- [ ] `build.gradle`에 `net.logstash.logback:logstash-logback-encoder` 의존성 추가
- [ ] `src/main/resources/logback-spring.xml` 파일 생성
- [ ] JSON 포맷 Encoder 설정
- [ ] 콘솔 및 파일 Appender에 JSON Encoder 적용
- **예상 작업 시간**: 2시간
- **담당자**: 백엔드 팀

#### 1.2 로그 파일 출력 설정
- [ ] `application.yml`에 로그 파일 경로 설정 추가
  ```yaml
  logging:
    file:
      name: /var/log/thinkfast/application.log
      max-size: 100MB
      max-history: 30
  ```
- [ ] 로그 파일 디렉토리 생성 스크립트 작성
- [ ] Filebeat가 읽을 수 있는 권한 설정
- **예상 작업 시간**: 1시간
- **담당자**: DevOps 팀

#### 1.3 공통 메타데이터 설정
- [ ] `application.yml`에 서비스 메타데이터 추가
  ```yaml
  logging:
    service:
      name: thinkfast
      version: ${project.version}
      environment: ${spring.profiles.active}
  ```
- [ ] Logback 설정에 MDC 기본값 설정
- [ ] 호스트명/IP 자동 수집 설정
- **예상 작업 시간**: 1시간
- **담당자**: 백엔드 팀

### Phase 2: 핵심 로그 구조화 (우선순위: 높음)

#### 2.1 HTTP 요청/응답 로그 구조화
- [ ] `LoggingInterceptor`에 MDC 기반 구조화 로깅 적용
- [ ] Request ID 생성 및 MDC 설정
- [ ] Trace ID 생성 및 MDC 설정
- [ ] HTTP 메타데이터를 구조화된 필드로 분리
  - `http.method`, `http.uri`, `http.status`, `http.duration_ms`, `http.client_ip`
- [ ] `afterCompletion`에서 null 체크 추가
- **예상 작업 시간**: 4시간
- **담당자**: 백엔드 팀
- **파일**: `LoggingInterceptor.java`

#### 2.2 예외 로그 구조화
- [ ] `ExceptionLoggingFilter`에 Error/Throwable 타입 캐치 추가
- [ ] `GlobalExceptionHandler`에 구조화된 로깅 적용
- [ ] 예외 정보를 구조화된 필드로 분리
  - `error.type`, `error.message`, `error.stack_trace`, `error.severity`
- [ ] 중복 로깅 방지 로직 개선
- **예상 작업 시간**: 4시간
- **담당자**: 백엔드 팀
- **파일**: `ExceptionLoggingFilter.java`, `GlobalExceptionHandler.java`

#### 2.3 UncaughtExceptionHandler 구현
- [ ] `GlobalUncaughtExceptionHandler` 클래스 생성
- [ ] Thread 기본 UncaughtExceptionHandler 설정
- [ ] Error 타입 (OutOfMemoryError 등) 구조화된 로깅
- [ ] 스레드 정보 포함
- **예상 작업 시간**: 3시간
- **담당자**: 백엔드 팀
- **파일**: `GlobalUncaughtExceptionHandler.java` (신규)

### Phase 3: 확장 및 개선 (우선순위: 중간)

#### 3.1 스케줄러 로그 구조화
- [ ] `SurveySchedule`에 MDC 기반 구조화 로깅 적용
- [ ] Job ID 생성 및 추적
- [ ] 실행 시간, 처리 건수 등 메트릭 추가
- [ ] Error 타입 예외 처리 추가
- **예상 작업 시간**: 2시간
- **담당자**: 백엔드 팀
- **파일**: `SurveySchedule.java`

#### 3.2 WebSocket 로그 구조화
- [ ] `AlarmHandler` Logger 클래스 참조 수정 (`AlarmHandler.class`)
- [ ] WebSocket 이벤트 구조화된 로깅 적용
- [ ] 세션 정보, 사용자 정보 구조화
- **예상 작업 시간**: 2시간
- **담당자**: 백엔드 팀
- **파일**: `AlarmHandler.java`

#### 3.3 비동기 작업 예외 처리
- [ ] `AsyncConfig` 생성 (비동기 설정)
- [ ] `AsyncUncaughtExceptionHandler` 구현
- [ ] MDC 전파를 위한 `TaskDecorator` 구현
- [ ] 비동기 작업 예외 구조화된 로깅
- **예상 작업 시간**: 3시간
- **담당자**: 백엔드 팀
- **파일**: `AsyncConfig.java` (신규)

#### 3.4 JWT 토큰 로그 구조화
- [ ] `JwtTokenProvider`에 구조화된 로깅 적용
- [ ] 토큰 만료/유효성 검사 로그 구조화
- **예상 작업 시간**: 1시간
- **담당자**: 백엔드 팀
- **파일**: `JwtTokenProvider.java`

### Phase 4: ElasticSearch + Kibana 통합 (우선순위: 중간)

#### 4.1 Filebeat 설정
- [ ] Filebeat 설치 및 설정
- [ ] 로그 파일 경로 설정
- [ ] ElasticSearch 출력 설정
- [ ] 인덱스 템플릿 설정
- **예상 작업 시간**: 4시간
- **담당자**: DevOps 팀

#### 4.2 ElasticSearch 인덱스 템플릿 생성
- [ ] 로그 타입별 인덱스 템플릿 정의
  - `thinkfast-logs-*` (일반 로그)
  - `thinkfast-errors-*` (에러 로그)
- [ ] 필드 매핑 정의 (타입, 분석기 설정)
- [ ] 인덱스 라이프사이클 정책 설정
- **예상 작업 시간**: 3시간
- **담당자**: DevOps 팀

#### 4.3 Kibana 대시보드 구성
- [ ] 로그 탐색 (Discover) 뷰 최적화
- [ ] 대시보드 생성
  - HTTP 요청/응답 모니터링
  - 에러 추이 분석
  - 성능 메트릭 (응답 시간, 처리량)
  - 스케줄러 실행 현황
  - WebSocket 연결 현황
- [ ] 시각화 차트 생성
- **예상 작업 시간**: 6시간
- **담당자**: DevOps 팀

### Phase 5: 최적화 및 모니터링 (우선순위: 낮음)

#### 5.1 로그 샘플링 설정
- [ ] 고빈도 로그에 대한 샘플링 전략 수립
- [ ] Logback 샘플링 설정
- **예상 작업 시간**: 2시간
- **담당자**: 백엔드 팀

#### 5.2 로그 레벨 동적 변경
- [ ] Actuator 엔드포인트를 통한 로그 레벨 동적 변경
- [ ] 프로덕션 환경 로그 레벨 최적화
- **예상 작업 시간**: 2시간
- **담당자**: 백엔드 팀

#### 5.3 알림 설정
- [ ] Kibana Alert 설정
  - 에러율 임계값 초과 시 알림
  - 응답 시간 임계값 초과 시 알림
  - 특정 에러 타입 발생 시 알림
- [ ] Slack/Email 연동
- **예상 작업 시간**: 4시간
- **담당자**: DevOps 팀

#### 5.4 성능 모니터링
- [ ] 로그 수집 성능 모니터링
- [ ] ElasticSearch 인덱싱 성능 최적화
- [ ] 인덱스 크기 관리
- **예상 작업 시간**: 3시간
- **담당자**: DevOps 팀

---

## 📊 예상 로그 스키마

### 공통 필드
```json
{
  "@timestamp": "ISO 8601 형식",
  "level": "INFO|ERROR|WARN|DEBUG",
  "service": "thinkfast",
  "env": "local|prod|staging",
  "host": "서버 호스트명",
  "logType": "http_request|exception|scheduler|websocket",
  "message": "사람이 읽을 수 있는 요약 메시지",
  "request_id": "단일 요청 추적 ID",
  "trace_id": "분산 추적 ID"
}
```

### HTTP 로그 필드
```json
{
  "http": {
    "method": "GET|POST|PUT|DELETE",
    "uri": "/api/surveys/123",
    "status": 200,
    "duration_ms": 45,
    "client_ip": "192.168.1.100",
    "user_agent": "Mozilla/5.0...",
    "content_type": "application/json"
  }
}
```

### 예외 로그 필드
```json
{
  "error": {
    "type": "java.lang.NullPointerException",
    "message": "Cannot invoke method on null object",
    "stack_trace": "전체 스택 트레이스",
    "severity": "error|critical",
    "cause": "원인 예외 정보"
  }
}
```

### 스케줄러 로그 필드
```json
{
  "scheduler": {
    "job_name": "updateExpiredSurvey",
    "job_id": "job-abc123",
    "execution_time_ms": 250,
    "processed_count": 3,
    "surveys": [...]
  }
}
```

### WebSocket 로그 필드
```json
{
  "websocket": {
    "event": "connection_established|connection_closed",
    "session_id": "session-abc123",
    "username": "user@example.com",
    "user_id": 456,
    "endpoint": "/alarm/user@example.com"
  }
}
```

---

## 📝 체크리스트 요약

### 필수 작업 (Phase 1-2)
- [ ] Logback JSON Encoder 설정
- [ ] 로그 파일 출력 설정
- [ ] HTTP 요청/응답 로그 구조화
- [ ] 예외 로그 구조화
- [ ] UncaughtExceptionHandler 구현

### 권장 작업 (Phase 3)
- [ ] 스케줄러 로그 구조화
- [ ] WebSocket 로그 구조화
- [ ] 비동기 작업 예외 처리
- [ ] JWT 토큰 로그 구조화

### 선택 작업 (Phase 4-5)
- [ ] Filebeat 설정
- [ ] ElasticSearch 인덱스 템플릿
- [ ] Kibana 대시보드
- [ ] 알림 설정
- [ ] 성능 최적화

---

## 🔗 참고 자료

- [Logstash Logback Encoder 문서](https://github.com/logfellow/logstash-logback-encoder)
- [ElasticSearch Common Schema](https://www.elastic.co/guide/en/ecs/current/index.html)
- [Spring Boot Logging 가이드](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging)

---

**작성일**: 2024-01-15  
**최종 수정일**: 2024-01-15  
**버전**: 1.0

