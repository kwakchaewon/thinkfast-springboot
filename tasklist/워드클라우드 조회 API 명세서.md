# 워드클라우드 조회 API 명세서

## 개요
주관식 질문의 응답을 분석하여 워드클라우드 데이터를 조회하는 API입니다. 설문 종료 후 자동으로 생성된 워드클라우드를 DB에서 조회하거나, 워드클라우드가 없는 경우 실시간으로 생성하여 반환합니다.

---

## API 엔드포인트

### 요청
```
GET /survey/{surveyId}/questions/{questionId}/wordcloud
```

### 인증
- **필수**: JWT 토큰 (Bearer Token)
- **권한**: CREATOR 역할 필요 (`@PreAuthorize("hasRole('CREATOR')")`)
- **설문 소유자 확인**: 설문의 `userId`와 현재 사용자 ID가 일치해야 함

### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `surveyId` | Long | 필수 | 설문 ID |
| `questionId` | Long | 필수 | 질문 ID (주관식 질문만 가능) |

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
    "questionId": 2,
    "wordCloud": [
      {
        "word": "매칭",
        "count": 12
      },
      {
        "word": "클라이언트",
        "count": 8
      },
      {
        "word": "버그",
        "count": 7
      },
      {
        "word": "최적화",
        "count": 6
      },
      {
        "word": "AFK",
        "count": 5
      }
    ],
    "totalResponses": 38
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

#### 응답 필드 설명
| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | Boolean | 요청 성공 여부 |
| `message` | String | 응답 메시지 |
| `data` | Object | 워드클라우드 데이터 |
| `data.questionId` | Long | 질문 ID |
| `data.wordCloud` | Array<Object> | 워드클라우드 단어 리스트 (빈도수 내림차순 정렬, 최대 50개) |
| `data.wordCloud[].word` | String | 키워드 |
| `data.wordCloud[].count` | Integer | 키워드 빈도수 |
| `data.totalResponses` | Long | 전체 응답 수 (중복 제거된 세션 수) |
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
- 질문 ID가 존재하지 않음

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

#### 4. 잘못된 요청 (400 Bad Request)
```json
{
  "success": false,
  "message": "잘못된 요청입니다.",
  "data": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

**발생 조건:**
- 주관식 질문이 아님 (워드클라우드는 주관식 질문에만 적용 가능)
- 질문이 해당 설문에 속하지 않음

---

#### 5. 서버 오류 (500 Internal Server Error)
```json
{
  "success": false,
  "message": "서버 내부 오류가 발생했습니다.",
  "data": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

**발생 조건:**
- 워드클라우드 역직렬화 실패
- 데이터베이스 오류
- 텍스트 분석 중 오류
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
5. 질문 존재 여부 확인
   ↓
6. 질문 타입 확인 (SUBJECTIVE만 허용)
   ↓
7. 워드클라우드 조회
   - DB에서 기존 워드클라우드 조회 (WordCloud)
   - 있으면: 역직렬화하여 반환
   - 없으면: 실시간 생성하여 반환
   ↓
8. 응답 반환
```

### 2. 워드클라우드 생성 로직
```
WordCloudService.getWordCloud(questionId)
   ↓
1. WordCloudRepository.findByQuestionId(questionId)
   ↓
2. 워드클라우드가 있으면?
   → JSON 역직렬화 → WordCloudResponseDto 반환
   ↓
3. 워드클라우드가 없으면?
   → generateWordCloud() 호출
   → 주관식 응답 텍스트 수집
   → TextAnalysisService를 통한 키워드 추출
   → 빈도수 계산 및 정렬
   → 상위 50개 키워드 선택
   → WordCloudResponseDto 반환
```

---

## 비즈니스 로직

### 키워드 추출 프로세스
1. **응답 데이터 수집**
   - 질문별 모든 주관식 응답 텍스트 조회
   - 빈 응답 제외

2. **텍스트 전처리**
   - 특수문자, 이모지 제거
   - 구어체 정규화 (축약형, 줄임말 처리)
   - 반복 문자 정규화 (예: "너무너무너무" → "너무")
   - 비속어 필터링
   - 인터넷 축약어 필터링 (초성 축약어 처리)

3. **키워드 추출**
   - 정규식 기반 한글 단어 추출 (`[가-힣]+`)
   - 최소 2글자 이상인 단어만 추출
   - 불용어 제거 (조사, 대명사, 접속사, 부사 등)

4. **빈도수 계산 및 정렬**
   - 키워드별 빈도수 계산
   - 빈도수 내림차순 정렬
   - 상위 50개 키워드 선택

5. **워드클라우드 데이터 생성**
   - `WordCloudDto` 리스트 생성 (word, count)
   - `WordCloudResponseDto` 생성 (questionId, wordCloud, totalResponses)

---

## 예시

### 요청 예시
```bash
curl -X GET "http://localhost:8080/survey/1/questions/2/wordcloud" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

### 성공 응답 예시
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "questionId": 2,
    "wordCloud": [
      {
        "word": "매칭",
        "count": 12
      },
      {
        "word": "클라이언트",
        "count": 8
      },
      {
        "word": "버그",
        "count": 7
      },
      {
        "word": "최적화",
        "count": 6
      },
      {
        "word": "성능",
        "count": 5
      },
      {
        "word": "안정성",
        "count": 4
      }
    ],
    "totalResponses": 38
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

### 응답이 없는 경우
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "questionId": 2,
    "wordCloud": [],
    "totalResponses": 0
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

**발생 조건:**
- 주관식 질문에 응답이 없음
- 모든 응답이 빈 텍스트

---

## 주의사항

1. **성능 고려사항**
   - 워드클라우드가 DB에 저장되어 있으면 빠르게 조회 가능
   - 워드클라우드가 없으면 실시간 계산이 필요하므로 응답 시간이 길어질 수 있음
   - 설문 종료 후 스케줄러가 자동으로 워드클라우드를 생성하므로, 대부분의 경우 DB에서 조회됨
   - 대량의 응답 데이터가 있는 경우 키워드 추출 및 빈도수 계산에 시간이 소요될 수 있음

2. **데이터 일관성**
   - 워드클라우드는 설문 종료 시점의 데이터를 기준으로 생성됨
   - 설문 종료 후에도 새로운 응답이 추가되면 워드클라우드가 갱신되지 않음
   - 최신 데이터가 필요한 경우 워드클라우드를 재생성해야 함

3. **권한 확인**
   - 설문 소유자만 조회 가능
   - 다른 사용자의 설문 워드클라우드는 조회할 수 없음

4. **질문 타입 제한**
   - 주관식 질문 (`SUBJECTIVE`)에만 적용 가능
   - 객관식 질문 (`MULTIPLE_CHOICE`)이나 척도형 질문 (`SCALE`)에서는 사용 불가

5. **키워드 추출 제한**
   - 최소 2글자 이상인 한글 단어만 추출
   - 불용어, 비속어, 인터넷 축약어는 자동으로 필터링됨
   - 상위 50개 키워드만 반환 (빈도수 기준)

---

## 관련 API

- `GET /survey/{id}` - 설문 상세 조회
- `GET /survey/{id}/questions` - 설문 질문 목록 조회
- `GET /survey/{id}/summary` - 요약 리포트 조회
- `GET /survey/{id}/questions/{questionId}/statistics` - 질문별 통계 조회 (향후 구현 예정)

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2024-01-15 | 초기 API 명세서 작성 |

