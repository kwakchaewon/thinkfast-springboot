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

### 텍스트 분석 모듈 개발 (Java 자체 구현)
- [x] 한국어 텍스트 전처리 모듈
  - [x] 특수문자, 이모지 제거 로직
  - [x] 기본 텍스트 정제 작업
  - [x] 불용어(stop words) 사전 구축 (하드코딩된 리스트)
  - [x] 형태소 분석 없이 정규식 기반 처리 (Python 서버 구축 안함)
  - [x] `TextAnalysisService` 클래스 생성 (`service/ai/TextAnalysisService.java`)
  - [x] 구어체 정규화 (축약형, 줄임말 처리)
  - [x] 반복 문자 정규화 (예: "너무너무너무" -> "너무")
  - [x] 비속어 필터링 (비속어 사전 구축 및 필터링 옵션)
  - [x] 인터넷 축약어 필터링 (초성 축약어 처리: ㅈㄴ, ㅈㄹ, ㅅㅂ 등)

### 요약 리포트 생성 로직
- [x] 설문 데이터 집계 및 분석
  - [x] 객관식 질문별 주요 응답 옵션 추출
  - [x] 비율이 가장 높은 옵션 식별
  - [x] `SurveyStatisticsService` 클래스 생성 (`service/ai/SurveyStatisticsService.java`)
  - [x] 통계 집계 DTO 클래스 생성 (`OptionStatisticsDto`, `QuestionStatisticsDto`)
  - [x] `ResponseRepository`에 통계 집계 쿼리 메서드 추가

### 워드클라우드 집계 API
- [x] 주관식 질문 응답 데이터 수집
  - [x] 질문별 모든 응답 텍스트 조회
  - [x] 데이터 전처리
- [x] 키워드 분석 및 집계
  - [x] 텍스트 분석 모듈 호출
  - [x] 키워드 빈도수 집계
  - [x] 워드클라우드 데이터 포맷 생성
- [x] 워드클라우드 API 엔드포인트 구현
  - [x] `GET /survey/:id/questions/:questionId/wordcloud` API 개발
  - [x] 응답 데이터 구조 정의
  - [x] `WordCloudService` 클래스 생성
  - [x] `WordCloudDto`, `WordCloudResponseDto` 클래스 생성

### 인사이트 텍스트 생성 로직
- [x] 객관식 인사이트 생성
  - [x] 옵션별 분포 분석 (Java)
  - [x] 패턴 인식 로직 (예: 특정 옵션이 과반수, 비슷한 비율 등) (Java)
  - [x] 템플릿 기반 문장 생성 (Java - 기본 구현)
  - [x] `InsightService` 클래스 생성 (`service/ai/InsightService.java`)
- [x] 주관식 인사이트 생성
  - [x] 주요 키워드 기반 문장 생성 (Java - 템플릿 기반)
  - [x] 트렌드 설명 텍스트 생성 (Java)
- [x] 인사이트 텍스트 DB 저장 로직 구현
  - [x] Flyway 마이그레이션 스크립트 생성 (`V8__create_question_insights_table.sql`)
  - [x] 설문 종료 시 시점에서 인사이트 질문별 텍스트 생성 (`SurveySchedule` 통합)
  - [x] API 구현 및 호출 시 API 조회 로직을 통해 질문별 인사이트 조회 (`InsightService.getInsight()`)
  - [x] 인사이트 텍스트 조회 API 구현 (`GET /survey/{surveyId}/questions/{questionId}/insight`)

### Backend API 개발
- [x] 요약 리포트 조회 API (`GET /survey/:id/summary`)
  - [x] 설문 소유자 확인 로직 구현 (userId 기반 권한 검증)
  - [x] `SummaryService.getSummaryReport()` 호출 (DB 조회 우선)
  - [x] 응답 데이터 반환
  - [x] DB 저장 로직 구현 완료 (설문 종료 시 비동기 생성)
  - [x] API 명세서 작성 (`API_SPEC_SUMMARY_REPORT.md`)
- [x] 인사이트 텍스트 조회 API (`GET /survey/:id/questions/:questionId/insight`)
  - [x] 설문 소유자 확인 로직 구현
  - [x] 질문 존재 여부 및 소유권 확인
  - [x] `InsightService.getInsight()` 호출 (DB 조회 우선)
  - [x] 응답 데이터 반환
  - [x] API 명세서 작성 (`API_SPEC_INSIGHT.md`)
- [x] 워드클라우드 조회 API (`GET /survey/:id/questions/:questionId/wordcloud`)
  - [x] 설문 소유자 확인 로직 구현
  - [x] 주관식 질문 검증 (워드클라우드는 주관식 질문에만 적용)
  - [x] 키워드 분석 및 집계 로직 호출
  - [x] 워드클라우드 데이터 반환
  - [x] DB 저장 로직 구현 완료 (설문 종료 시 비동기 생성)
  - [x] API 명세서 작성 (`API_SPEC_WORD_CLOUD.md`)

---

## 참고사항

### 기술 스택
- **AI API**: Google Gemini 2.0 Flash (무료 티어)
- **텍스트 분석**: Java 정규식 기반 (형태소 분석 없이 구현)
- **비동기 처리**: Spring @Async
- **데이터베이스**: MariaDB (Flyway 마이그레이션)

### 주요 구현 포인트
1. **Java 단일 구현**: Python 서버 없이 Java에서 모든 기능 구현
2. **비동기 처리**: AI 리포트 생성은 백그라운드에서 처리하여 API 응답 시간 단축
3. **DB 저장 우선**: 생성된 리포트는 DB에 저장하여 재사용
4. **템플릿 기반 폴백**: AI API 실패 시 Java 템플릿 기반 인사이트 생성

---

**작성일**: 2024-XX-XX  
**최종 수정일**: 2024-XX-XX
