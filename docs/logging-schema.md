# THINKFAST 로깅 시스템 스키마 문서

## 개요

이 문서는 THINKFAST WAS 로깅 시스템의 로그 스키마를 정의합니다. 모든 로그는 JSON 형식으로 출력되며, ELK 스택(Elasticsearch, Logstash, Kibana)과 호환됩니다.

## 공통 필드

모든 로그에 공통으로 포함되는 필드입니다.

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| @timestamp | date | 로그 발생 시간 (ISO 8601) | 2024-01-01T12:00:00.000Z |
| level | keyword | 로그 레벨 | INFO, ERROR, WARN, DEBUG |
| service | keyword | 서비스명 | thinkfast |
| env | keyword | 환경 정보 | prod, local |
| host | keyword | 서버 호스트명 | server-hostname |
| log_type | keyword | 로그 타입 | http_request, exception, external_api, scheduler, async, db, performance |
| message | text | 사람이 읽을 수 있는 요약 메시지 | HTTP request received: POST /api/surveys |
| request_id | keyword | 단일 요청 추적 ID | req-abc123 |
| trace_id | keyword | 분산 추적 ID | trace-xyz789 |
| user_id | keyword | 사용자 ID (인증된 경우) | user-123 |

## HTTP 요청 로그 (log_type: http_request)

HTTP 요청/응답에 대한 로그입니다.

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| http.method | keyword | HTTP 메서드 | GET, POST, PUT, DELETE |
| http.uri | keyword | 요청 URI | /api/surveys/123 |
| http.status | number | HTTP 상태 코드 | 200, 404, 500 |
| http.duration_ms | number | 응답 시간 (밀리초) | 145 |
| http.client_ip | keyword | 클라이언트 IP 주소 | 192.168.1.100 |
| http.user_agent | text | User-Agent 헤더 | Mozilla/5.0... |
| http.content_type | keyword | Content-Type | application/json |
| http.request_size | number | 요청 크기 (바이트) | 1024 |
| http.response_size | number | 응답 크기 (바이트) | 2048 |
| http.slow_request | boolean | 느린 요청 여부 (3초 이상) | true, false |

## 예외 로그 (log_type: exception)

예외 및 에러에 대한 로그입니다.

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| error.type | keyword | 예외 클래스명 | com.example.thinkfast.exception.AiServiceException |
| error.message | text | 예외 메시지 | Gemini API 호출 실패 |
| error.stack_trace | text | 전체 스택 트레이스 | java.lang.Exception: ... |
| error.severity | keyword | 심각도 | error, critical, warn |
| error.category | keyword | 예외 카테고리 | business, system |
| error.cause.type | keyword | 원인 예외 타입 | org.springframework.web.reactive... |
| error.cause.message | text | 원인 예외 메시지 | 500 Internal Server Error |
| error.root_cause.type | keyword | 최상위 원인 예외 타입 | java.net.SocketTimeoutException |
| error.root_cause.message | text | 최상위 원인 예외 메시지 | Connection timeout |
| http.method | keyword | HTTP 메서드 (HTTP 요청 중 발생한 경우) | POST |
| http.uri | keyword | HTTP URI (HTTP 요청 중 발생한 경우) | /api/surveys |
| http.query_string | text | 쿼리 문자열 (민감 정보 마스킹됨) | id=123&name=test |
| thread.name | keyword | 스레드 이름 (비동기 작업의 경우) | async-1 |
| thread.id | number | 스레드 ID (비동기 작업의 경우) | 42 |
| thread.state | keyword | 스레드 상태 (비동기 작업의 경우) | RUNNABLE |

## 외부 API 로그 (log_type: external_api)

외부 시스템 연동에 대한 로그입니다.

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| external_api.system | keyword | 외부 시스템명 | gemini, qdrant, redis, mariadb |
| external_api.operation | keyword | 작업 타입 | generateContent, embedContent, search, pubsub_send |
| external_api.request_url | text | 요청 URL (민감 정보 마스킹됨) | https://generativelanguage.googleapis.com/... |
| external_api.request_method | keyword | HTTP 메서드 | POST, GET |
| external_api.duration_ms | number | 실행 시간 (밀리초) | 1234 |
| external_api.status | keyword | 상태 | success, failure, timeout |
| external_api.response_status | number | HTTP 응답 상태 코드 | 200, 500 |
| external_api.retry_count | number | 재시도 횟수 | 2 |
| external_api.error_message | text | 에러 메시지 | Connection timeout |
| external_api.channel | keyword | Redis 채널명 (Redis의 경우) | alarm-channel |
| external_api.message_size | number | 메시지 크기 (바이트, Redis의 경우) | 1024 |
| external_api.recipient_id | keyword | 수신자 ID (Redis의 경우) | user-123 |

## 스케줄러 로그 (log_type: scheduler)

배치 및 스케줄 작업에 대한 로그입니다.

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| scheduler.job_name | keyword | 작업 이름 | updateExpiredSurvey, updateActiveSurveyReports |
| scheduler.job_id | keyword | 작업 ID | job-20240101-120000-expired |
| scheduler.execution_time_ms | number | 실행 시간 (밀리초) | 2500 |
| scheduler.processed_count | number | 처리된 건수 | 5 |
| scheduler.failed_count | number | 실패한 건수 | 0 |
| scheduler.status | keyword | 작업 상태 | success, failure, partial |
| scheduler.error_message | text | 에러 메시지 (실패한 경우) | Database connection failed |

## 비동기 작업 로그 (log_type: async)

@Async 작업에 대한 로그입니다.

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| async.method | keyword | 메서드명 | SummaryService.saveSummaryReportAsync |
| async.thread | keyword | 스레드 이름 | async-1 |
| async.parameters | text | 메서드 파라미터 (요약) | surveyId=123, ... |

## 데이터베이스 로그 (log_type: db)

데이터베이스 쿼리에 대한 로그입니다.

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| db.operation | keyword | 작업 타입 | select, insert, update, delete |
| db.query | text | SQL 쿼리 (정규화됨) | SELECT * FROM survey WHERE id = ? |
| db.duration_ms | number | 실행 시간 (밀리초) | 45 |
| db.slow_query | boolean | 느린 쿼리 여부 (1초 이상) | true, false |
| db.query_count | number | 동일 쿼리 실행 횟수 | 10 |
| db.avg_duration_ms | number | 평균 실행 시간 (밀리초) | 35 |

## 성능 로그 (log_type: performance)

성능 측정에 대한 로그입니다.

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| performance.class | keyword | 클래스명 | SurveyService |
| performance.method | keyword | 메서드명 | getSurveys |
| performance.duration_ms | number | 실행 시간 (밀리초) | 123 |
| performance.slow | boolean | 느린 실행 여부 | true, false |
| performance.parameters | text | 메서드 파라미터 (요약) | userId=123, ... |
| performance.exception | keyword | 예외 타입 (발생한 경우) | NullPointerException |
| performance.success | boolean | 성공 여부 | true, false |

## Elasticsearch 인덱스 템플릿

Elasticsearch에 로그를 저장할 때 사용할 인덱스 템플릿 예시입니다.

```json
{
  "index_patterns": ["thinkfast-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 1
    },
    "mappings": {
      "properties": {
        "@timestamp": { "type": "date" },
        "level": { "type": "keyword" },
        "service": { "type": "keyword" },
        "env": { "type": "keyword" },
        "host": { "type": "keyword" },
        "log_type": { "type": "keyword" },
        "message": { "type": "text" },
        "request_id": { "type": "keyword" },
        "trace_id": { "type": "keyword" },
        "user_id": { "type": "keyword" },
        "http": {
          "properties": {
            "method": { "type": "keyword" },
            "uri": { "type": "keyword" },
            "status": { "type": "integer" },
            "duration_ms": { "type": "long" },
            "client_ip": { "type": "ip" },
            "user_agent": { "type": "text" },
            "slow_request": { "type": "boolean" }
          }
        },
        "error": {
          "properties": {
            "type": { "type": "keyword" },
            "message": { "type": "text" },
            "severity": { "type": "keyword" },
            "stack_trace": { "type": "text" }
          }
        },
        "external_api": {
          "properties": {
            "system": { "type": "keyword" },
            "operation": { "type": "keyword" },
            "duration_ms": { "type": "long" },
            "status": { "type": "keyword" }
          }
        },
        "scheduler": {
          "properties": {
            "job_name": { "type": "keyword" },
            "job_id": { "type": "keyword" },
            "execution_time_ms": { "type": "long" },
            "processed_count": { "type": "integer" },
            "status": { "type": "keyword" }
          }
        }
      }
    }
  }
}
```

## 로그 샘플링

성능 최적화를 위해 다음 샘플링 전략이 적용됩니다:

- **성공 요청**: 10%만 로깅
- **에러/경고**: 100% 로깅
- **느린 요청** (3초 이상): 100% 로깅

## 민감 정보 마스킹

다음 정보는 자동으로 마스킹됩니다:

- API 키, 토큰, 비밀번호
- 이메일 주소 (사용자명 부분만)
- 전화번호, 주민등록번호, 신용카드 번호

