## :bar_chart: Think Fast 서비스 소개
AI 기반 인사이트 제공. 실시간 설문조사 플랫폼 **띵패스트**
<br/>
실시간 설문 조사, 알림, AI 및 LangChain/RAG 기반 인사이트 등의 서비스를 제공합니다.
<br/>
<br/>

## 🛠  사용기술

### **Frontend**
- Vue3, Typescript
- [thinkfast-vue3 리포지토리로 이동하기](https://github.com/kwakchaewon/thinkfast-vue3)

### **Backend**
- Spring boot, Spring Security, Redis, Web Socket

### **Database**
- MariaDB + Redis

### **AI / LLM**
- Python 기반 LangChain, Hugging Face Transformers
- Vector DB (FAISS)

### **Infra / Deployment**
- AWS EC2 & RDS
- Docker & Jenkins 기반 CI/CD
- HTTPS only Cookie
- Redis Pub/Sub
<br/>

### ✅ 서비스 아키텍쳐


### ✅ ERD 


### ✅ 서비스 플로우 
<br/>
<br/>


## 💻 상세 개발 내역
### ✅ 로그인/회원가입
✔️ **Spring Securiyt + JWT 기반 인증 인가**
- 회원가입 시 각 필드별 유효성 검사.
- 로그인 시 `AccessToken` 과 `RefreshToken` 발급.

|회원 가입 화면|로그인 화면|
|---|---|
|![image](https://github.com/AIVLE-School-Third-Big-Project/Team11-Project/assets/76936390/ea45c4c8-2a2a-4c51-bb27-4bf21f1f8c64)|![image](https://github.com/AIVLE-School-Third-Big-Project/Team11-Project/assets/76936390/2899f85b-1dea-4f25-b664-033b187f4f4f)|
<br/>

### ✅ 설문 CRUD
✔️ **설문 생성**
- 설문 생성 시, 질문 유형(객관식/주관식) 및 응답 종료 시간 설정 가정 가능.

|설문 생성 화면|설문 생성 화면|
|---|---|
|![image](https://github.com/AIVLE-School-Third-Big-Project/Team11-Project/assets/76936390/ea45c4c8-2a2a-4c51-bb27-4bf21f1f8c64)|![image](https://github.com/AIVLE-School-Third-Big-Project/Team11-Project/assets/76936390/2899f85b-1dea-4f25-b664-033b187f4f4f)|
<br/>

✔️ **설문 조회**
- 전체 설문 조회 및 설문 상세 기능 제공.
- 삭제 설문 조회 시, 예외 처리.

|설문 상세|삭제 설문 조회 화면|
|---|---|
|![image](https://github.com/AIVLE-School-Third-Big-Project/Team11-Project/assets/76936390/ea45c4c8-2a2a-4c51-bb27-4bf21f1f8c64)|![image](https://github.com/AIVLE-School-Third-Big-Project/Team11-Project/assets/76936390/2899f85b-1dea-4f25-b664-033b187f4f4f)|
<br/>


### ✅ 설문 참여
✔️ **설문 응답**
- 모바일 환경에 최적화된 UI/UX
- QR 코드를 통한 쉬운 응답 참여. 번거로운 회원가입 절차 없이 응답 가능.
- 중복 응답 방지를 위한 설문별 쿠키 + 스토리지 + IP 제한.
- 삭제, 만료 설문, 중복 참여 예외 처리

|응답 참여 화면|삭제 설문 조회 화면|
|---|---|
|![image](https://github.com/AIVLE-School-Third-Big-Project/Team11-Project/assets/76936390/ea45c4c8-2a2a-4c51-bb27-4bf21f1f8c64)|![image](https://github.com/AIVLE-School-Third-Big-Project/Team11-Project/assets/76936390/2899f85b-1dea-4f25-b664-033b187f4f4f)|
<br/>


**✔ Hospital 광고 API**
- 메인 화면에 답변 작성이 우수한 병원 광고.<br/>
- Rank 알고리즘이 없어 ChatGPT를 제외한 병원 리스트 랜덤하게 표기 <br/><br/>


### ✅ 알림 및 로깅 시스템
- (~ing) <br/>

### ✅ AI 요약 및 인사이트
- Python 서버(LangChain + Hugging Face) 연동 <br/>
- 주관식 응답 요약 및 감정 분석 API 개발<br/>
- 설문 응답 기반 요약 리포트 자동 생성<br/>

**✔ 비동기적 gpt 답변 등록**


### ✅ RAG 기반 Q&A 기능
- FAISS 또는 Chroma를 사용한 벡터 DB 구축<br/>
- 주관식 응답 벡터화 및 저장 <br/>
- LangChain RetrievalQAChain 기반 질의응답 API 개발<br/>

### ✅ 배포 및 취약점 개선 
- HttpOnly Cookie 저장 방식 적용
- Redis Pub/Sub
- DB 이중화
- 중복 로그인 방지
<br/><br/>
