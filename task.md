# 메뉴얼 챗봇 RAG 구현 작업 리스트

## 개요
RAG 기반 메뉴얼 챗봇 구현: Qdrant Vector DB를 활용한 메뉴얼 검색, Gemini API를 통한 답변 생성, 건의사항 MariaDB 저장 기능을 추가합니다.

---

## 인프라

### 1. Qdrant Docker 컨테이너 설정
- [x] 운영 docker-compose.yml Qdrant 서비스 추가 (포트 6333/6334, 볼륨 `qdrant_data`)
- [x] 로컬 실행 확인  
  `docker run -d --name thinkfast-qdrant --restart unless-stopped -e TZ=Asia/Seoul -e QDRANT__SERVICE__GRPC_PORT=6334 -e QDRANT__SERVICE__HTTP_PORT=6333 -p 6333:6333 -p 6334:6334 -v qdrant_data:/qdrant/storage qdrant/qdrant:v1.7.0`
- [x] build.gradle에 Qdrant Java 클라이언트 의존성 추가 (io.qdrant:client:1.7.0)
- [x] build.gradle에 파일 파싱 라이브러리 추가 (Apache Tika for PDF/DOCX)

### 2. 데이터베이스 스키마
- [ ] V13__create_manuals_tables.sql - MANUALS 테이블 생성
- [ ] V14__create_manual_chunks_table.sql - MANUAL_CHUNKS 테이블 생성
- [ ] V15__create_suggestions_table.sql - SUGGESTIONS 테이블 생성
- [ ] V16__create_chat_history_table.sql - CHAT_HISTORY 테이블 생성

### 3. 환경 변수 설정
- [ ] application-local.yml에 Qdrant 설정 추가
- [ ] application-prod.yml에 Qdrant 설정 추가
- [ ] Gemini Embedding API URL 설정 추가

---

## 백엔드 - Vector DB 통합

### 4. Qdrant 클라이언트 구현
- [ ] QdrantConfig.java 생성 - Qdrant 연결 설정
- [ ] QdrantClient.java 생성 - Vector DB CRUD 작업 (Collection/Point 관리, Vector 검색)

### 5. 임베딩 서비스 구현
- [ ] EmbeddingService.java 생성 - Gemini Embedding API 연동
- [ ] GeminiApiService.java에 Embedding 메서드 추가

---

## 백엔드 - 도메인 모델 및 Repository

### 6. 도메인 엔티티 구현
- [ ] Manual.java 생성 - 메뉴얼 엔티티
- [ ] ManualChunk.java 생성 - 청크 엔티티
- [ ] Suggestion.java 생성 - 건의사항 엔티티
- [ ] ChatHistory.java 생성 - 챗봇 대화 이력 엔티티 (선택사항)

### 7. Repository 구현
- [ ] ManualRepository.java 생성
- [ ] ManualChunkRepository.java 생성
- [ ] SuggestionRepository.java 생성
- [ ] ChatHistoryRepository.java 생성 (선택사항)

---

## 백엔드 - 파일 처리

### 8. 파일 파싱 서비스 구현
- [ ] FileProcessingService.java 생성
  - PDF/DOCX/TXT 파일 파싱
  - 텍스트 청킹 (500-1000자 단위, 오버랩 100자)
  - 인코딩 처리 (UTF-8)
  - 파일 저장 로직

---

## 백엔드 - 서비스 계층

### 9. 메뉴얼 서비스 구현
- [ ] ManualService.java 생성
  - 메뉴얼 등록 (텍스트/파일)
  - 청크 생성 및 임베딩
  - Qdrant에 벡터 저장
  - 메뉴얼 CRUD
  - 비동기 처리 (@Async)

### 10. RAG 서비스 구현
- [ ] RAGService.java 생성
  - 사용자 질문 임베딩
  - Qdrant에서 유사도 검색 (Cosine Similarity)
  - Top-K 청크 추출 (K=3~5)
  - 컨텍스트 구성

### 11. 챗봇 서비스 구현
- [ ] ChatService.java 생성
  - RAG 컨텍스트 + 사용자 질문을 Gemini에 전달
  - 프롬프트 엔지니어링
  - 답변 생성 및 참조 메뉴얼 ID 포함

### 12. 건의사항 서비스 구현
- [ ] SuggestionService.java 생성
  - 건의사항 저장 및 상태 관리

---

## 백엔드 - 컨트롤러 계층

### 13. 메뉴얼 관리 API 구현
- [ ] ManualController.java 생성
  - POST /api/manuals - 메뉴얼 등록 (텍스트)
  - POST /api/manuals/upload - 파일 업로드
  - GET /api/manuals - 메뉴얼 목록
  - GET /api/manuals/{id} - 메뉴얼 상세
  - PUT /api/manuals/{id} - 메뉴얼 수정
  - DELETE /api/manuals/{id} - 메뉴얼 삭제

### 14. 챗봇 API 구현
- [ ] ChatbotController.java 생성
  - POST /api/chatbot/chat - 챗봇 질문/답변
  - GET /api/chatbot/history - 대화 이력 조회 (선택사항)

### 15. 건의사항 API 구현
- [ ] SuggestionController.java 생성
  - POST /api/suggestions - 건의사항 등록
  - GET /api/suggestions - 건의사항 목록 (관리자)
  - PUT /api/suggestions/{id}/status - 상태 변경

---

## 백엔드 - DTO

### 16. DTO 클래스 구현
- [ ] ChatRequest.java - 챗봇 요청 DTO
- [ ] ChatResponse.java - 챗봇 응답 DTO
- [ ] CreateManualRequest.java - 메뉴얼 생성 요청 DTO
- [ ] ManualResponse.java - 메뉴얼 응답 DTO
- [ ] CreateSuggestionRequest.java - 건의사항 생성 요청 DTO
- [ ] SuggestionResponse.java - 건의사항 응답 DTO

---

## 백엔드 - 보안 및 최적화

### 17. 보안 설정
- [ ] SecurityConfig.java 수정
  - 챗봇 API 경로 추가
  - 메뉴얼 관리 API 경로 추가
  - 건의사항 API 경로 추가

### 18. 에러 처리 및 폴백
- [ ] GlobalExceptionHandler.java에 Vector DB 예외 처리 추가
- [ ] RAG 서비스에 폴백 로직 구현 (Vector DB 실패 시 키워드 검색)

### 19. 캐싱 전략
- [ ] Redis에 자주 묻는 질문-답변 캐싱
- [ ] 메뉴얼 목록 캐싱
- [ ] 캐시 무효화 로직

---

## 프론트엔드 (Vue3)

### 20. 챗봇 UI 컴포넌트
- [ ] 챗봇 인터페이스 컴포넌트 생성
  - 메시지 입력창
  - 대화 내역 표시
  - 로딩 상태 표시
  - 참조 메뉴얼 링크 표시

### 21. 메뉴얼 관리 UI 컴포넌트
- [ ] 메뉴얼 목록 페이지
- [ ] 메뉴얼 등록 페이지 (텍스트 입력)
- [ ] 메뉴얼 파일 업로드 페이지
- [ ] 메뉴얼 상세/수정 페이지

### 22. 건의사항 UI 컴포넌트
- [ ] 건의사항 작성 폼
- [ ] 건의사항 목록 (관리자용)
- [ ] 건의사항 상태 관리 (관리자용)

---

## 테스트 및 문서화

### 23. 테스트 작성
- [ ] 단위 테스트 (EmbeddingService, RAGService, ManualService, FileProcessingService)
- [ ] 통합 테스트 (챗봇 API, 메뉴얼 등록/조회)

### 24. API 문서화
- [ ] Swagger 어노테이션 추가
- [ ] API 명세서 작성

---

## 배포 및 운영

### 25. Docker 설정
- [ ] docker-compose.yml 최종 확인
- [ ] Qdrant 볼륨 설정 확인
- [ ] 환경 변수 설정 확인

### 26. 모니터링 및 로깅
- [ ] 챗봇 사용량 로깅
- [ ] Vector DB 검색 성능 모니터링
- [ ] 에러 로깅 강화

---

## 향후 확장 계획

- [ ] 이메일 발송 기능 (SMTP 연동)
- [ ] 챗봇 대화 이력 저장
- [ ] 메뉴얼 버전 관리
- [ ] 관리자 대시보드 (건의사항 관리)
- [ ] 챗봇 성능 최적화
- [ ] 다국어 지원

---

## 참고 사항

### 기술 스택
- **Vector DB**: Qdrant (Docker 컨테이너)
- **Embedding**: Gemini Embedding API (text-embedding-004)
- **LLM**: Gemini 2.0 Flash (기존)
- **파일 파싱**: Apache Tika
- **청킹 전략**: 고정 크기 청킹 (500-1000자) + 오버랩 (100자)

### 주요 구현 포인트
1. **청킹 전략**: 문장 단위로 분할, 오버랩으로 컨텍스트 유지
2. **검색 최적화**: Qdrant 필터링으로 활성 메뉴얼만 검색
3. **프롬프트 엔지니어링**: System Prompt에 "메뉴얼 기반으로만 답변" 명시
4. **에러 처리**: Vector DB 실패 시 키워드 기반 검색으로 폴백
5. **성능**: 메뉴얼 등록은 비동기, 챗봇 응답은 동기 (실시간성)
