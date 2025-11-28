# 요약 리포트 조회 API 명세서

## 개요
설문의 요약 리포트를 조회하는 API입니다. 설문 종료 후 자동으로 생성된 리포트를 DB에서 조회하거나, 리포트가 없는 경우 실시간으로 생성하여 반환합니다.

---

## API 엔드포인트

### 요청
```
GET /survey/{id}/summary
```

### 인증
- **필수**: JWT 토큰 (Bearer Token)
- **권한**: CREATOR 역할 필요 (`@PreAuthorize("hasRole('CREATOR')")`)
- **설문 소유자 확인**: 설문의 `userId`와 현재 사용자 ID가 일치해야 함

### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `id` | Long | 필수 | 설문 ID |

### Headers
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

---

## 응답

### 성공 응답 (200 OK)

#### 응답 본문
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "mainPosition": "정글",
    "mainPositionPercent": 28.57,
    "improvements": [
      "매칭 시스템에 대한 개선이 필요합니다",
      "클라이언트 관련 기능의 향상이 요청되었습니다",
      "버그 부분의 수정이 필요합니다"
    ]
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

#### 응답 필드 설명
| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | Boolean | 요청 성공 여부 |
| `message` | String | 응답 메시지 |
| `data` | Object | 요약 리포트 데이터 |
| `data.mainPosition` | String \| null | 가장 많이 선택된 객관식 옵션 (첫 번째 객관식 질문 기준) |
| `data.mainPositionPercent` | Double \| null | 해당 옵션의 비율 (%) |
| `data.improvements` | Array<String> | 개선 사항 리스트 (최대 5개, 주관식 질문에서 추출) |
| `timestamp` | String | 응답 생성 시간 (ISO 8601 형식) |

---

### 에러 응답

#### 1. 설문을 찾을 수 없음 (404 Not Found)
```json
{
  "success": false,
  "message": "존재하지 않는 설문입니다.",
  "data": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

**발생 조건:**
- 설문 ID가 존재하지 않음
- 설문이 삭제됨 (`isDeleted = true`)

---

#### 2. 인증 실패 (401 Unauthorized)
```json
{
  "success": false,
  "message": "인증이 필요합니다.",
  "data": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

**발생 조건:**
- JWT 토큰이 없음
- JWT 토큰이 만료됨
- JWT 토큰이 유효하지 않음

---

#### 3. 권한 없음 (403 Forbidden)
```json
{
  "success": false,
  "message": "인증이 필요합니다.",
  "data": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

**발생 조건:**
- 설문 소유자가 아님 (`Survey.userId` ≠ 현재 사용자 ID)
- CREATOR 역할이 아님

---

#### 4. 서버 오류 (500 Internal Server Error)
```json
{
  "success": false,
  "message": "서버 내부 오류가 발생했습니다.",
  "data": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

**발생 조건:**
- 리포트 역직렬화 실패
- 데이터베이스 오류
- 기타 서버 내부 오류

---

## 동작 흐름

### 1. 요청 처리 흐름
```
1. JWT 토큰 검증
   ↓
2. CREATOR 역할 확인
   ↓
3. 설문 존재 여부 확인
   ↓
4. 설문 소유자 확인 (Survey.userId == 현재 사용자 ID)
   ↓
5. 요약 리포트 조회
   - DB에서 기존 리포트 조회 (InsightReport)
   - 있으면: 역직렬화하여 반환
   - 없으면: 실시간 생성하여 반환
   ↓
6. 응답 반환
```

### 2. 리포트 조회 로직
```
SummaryService.getSummaryReport(surveyId)
   ↓
1. InsightReportRepository.findBySurveyId(surveyId)
   ↓
2. 리포트가 있으면?
   → JSON 역직렬화 → SummaryReportDto 반환
   ↓
3. 리포트가 없으면?
   → generateSummaryReport() 호출
   → 실시간 통계 집계 및 키워드 추출
   → SummaryReportDto 반환
```

---

## 비즈니스 로직

### mainPosition 추출
- 설문의 첫 번째 객관식 질문 (`MULTIPLE_CHOICE`)에서 추출
- `orderIndex`가 가장 작은 객관식 질문 선택
- 해당 질문의 옵션별 응답 수 집계
- 비율이 가장 높은 옵션을 `mainPosition`으로 설정
- 비율을 `mainPositionPercent`로 설정

### improvements 추출
- 설문의 모든 주관식 질문 (`SUBJECTIVE`)에서 추출
- 모든 주관식 응답 텍스트 수집
- `TextAnalysisService`를 통한 키워드 추출
- 개선 관련 키워드 필터링 (IMPROVEMENT_KEYWORDS 사전 활용)
- 템플릿 기반 문장 생성
- 최대 5개 개선 사항 반환

---

## 예시

### 요청 예시
```bash
curl -X GET "http://localhost:8080/survey/1/summary" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

### 성공 응답 예시
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "mainPosition": "정글",
    "mainPositionPercent": 28.57,
    "improvements": [
      "매칭 시스템에 대한 개선이 필요합니다",
      "클라이언트 관련 기능의 향상이 요청되었습니다",
      "버그 부분의 수정이 필요합니다",
      "성능 최적화가 필요합니다",
      "안정성 향상이 요청되었습니다"
    ]
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

### 리포트가 없는 경우 (실시간 생성)
- DB에 리포트가 없어도 실시간으로 생성하여 반환
- 설문 종료 후 스케줄러가 비동기로 리포트를 생성하므로, 이후 요청에서는 DB에서 조회됨

---

## 주의사항

1. **성능 고려사항**
   - 리포트가 DB에 저장되어 있으면 빠르게 조회 가능
   - 리포트가 없으면 실시간 계산이 필요하므로 응답 시간이 길어질 수 있음
   - 설문 종료 후 스케줄러가 자동으로 리포트를 생성하므로, 대부분의 경우 DB에서 조회됨

2. **데이터 일관성**
   - 리포트는 설문 종료 시점의 데이터를 기준으로 생성됨
   - 설문 종료 후에도 새로운 응답이 추가되면 리포트가 갱신되지 않음
   - 최신 데이터가 필요한 경우 리포트를 재생성해야 함

3. **권한 확인**
   - 설문 소유자만 조회 가능
   - 다른 사용자의 설문 리포트는 조회할 수 없음

---

## 관련 API

- `GET /survey/{id}` - 설문 상세 조회
- `GET /survey/{id}/questions` - 설문 질문 목록 조회
- `GET /survey/{id}/questions/{questionId}/statistics` - 질문별 통계 조회 (향후 구현 예정)

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2024-01-15 | 초기 API 명세서 작성 |

