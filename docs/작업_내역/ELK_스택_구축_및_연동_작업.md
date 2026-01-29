# ELK 스택 구축 및 연동 작업 리스트

## 개요

THINKFAST WAS의 구조화된 JSON 로그를 Elasticsearch에 저장하고, Kibana를 통해 로그를 시각화 및 분석할 수 있도록 ELK 스택을 구축합니다. 모든 컴포넌트는 Docker 기반으로 구동하며, 추후 docker-compose.yml로 통합 관리합니다.

---

## 현재 상태

### 로그 출력 현황
- **로그 포맷**: JSON (logstash-logback-encoder 7.0 사용)
- **로그 파일 경로**: `/var/log/thinkfast/application.log` (프로덕션 환경)
- **로그 구조**: 구조화된 JSON (MDC 기반 필드 포함)
- **로그 타입**: http_request, exception, external_api, scheduler, async, performance

### 기존 설정 파일
- `src/main/resources/logback-spring.xml`: JSON 로그 출력 설정
- `docs/logstash-pipeline.conf`: Logstash 파이프라인 설정 (참고용)
- `docs/filebeat-config.yml`: Filebeat 설정 (참고용)

---

## Phase 1: Elasticsearch 구축

### 1.1 Elasticsearch Docker 컨테이너 설정

**목표**: Elasticsearch 8.x Docker 컨테이너 실행

**작업 내용**:
- [X] Elasticsearch 이미지 선택 (docker.elastic.co/elasticsearch/elasticsearch:8.11.0 권장)
  - 공식 Elastic 이미지 사용: `docker.elastic.co/elasticsearch/elasticsearch:8.11.0`
  - 이미지 다운로드: `docker pull docker.elastic.co/elasticsearch/elasticsearch:8.11.0`
- [X] 기본 설정 파일 준비
  - 클러스터명: `thinkfast-cluster`
  - 노드명: `thinkfast-node-1`
  - 메모리: 최소 2GB (권장 4GB)
- [X] 볼륨 설정: 데이터 영속성을 위한 볼륨 마운트
- [X] 네트워크 설정: Docker 네트워크 생성 또는 기존 네트워크 사용

**참고 설정**:
```yaml
# docker-compose.yml (예시)
  elasticsearch:
    image: elasticsearch:8.11.0
    container_name: thinkfast-elasticsearch
    environment:
      - discovery.type=single-node # 단일 노드 모드로 실행
      - xpack.security.enabled=true # Elasticsearch 보안 기능 활성화
      - ELASTIC_PASSWORD=test1234  # elastic 사용자 비밀번호 (필수!) id: elastic
      - xpack.security.enrollment.enabled=false # 노드/키바나 자동 등록(enrollment) 기능 비활성화
      - "ES_JAVA_OPTS=-Xms1g -Xmx1g" # JVM Heap 메모리 설정
    deploy:
      resources:
        limits:
          memory: 2g # 컨테이너가 사용할 수 있는 최대 메모리 제한
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
      - "9300:9300"
    networks:
      - thinkfast_backend
    ulimits:
      memlock:
        soft: -1
        hard: -1
```

**비밀번호 설정 방법**:

Elasticsearch 8.x에서 `xpack.security.enabled=true`인 경우, 비밀번호 설정이 **필수**입니다.

**방법 1: 환경변수로 비밀번호 설정 (권장)**
- `ELASTIC_PASSWORD` 환경변수를 추가하여 `elastic` 사용자의 비밀번호를 설정
- 기본 사용자: `elastic`
- 예시: `ELASTIC_PASSWORD=your-elastic-password`
- **주의**: 비밀번호는 영문, 숫자, 특수문자를 포함한 강력한 비밀번호를 사용하세요.

**방법 2: 컨테이너 로그에서 자동 생성된 비밀번호 확인** (ELASTIC_PASSWORD 미설정 시)
- 컨테이너를 처음 실행하면 임시 비밀번호가 자동 생성됨
- 컨테이너 로그에서 확인:
  ```bash
  docker logs thinkfast-elasticsearch | grep "generated password"
  # 또는
  docker logs thinkfast-elasticsearch | grep "Password for the elastic user"
  ```
- **주의**: 이 비밀번호는 한 번만 표시되며, 안전한 곳에 저장해야 합니다.

**로그인 정보**:
- 사용자명: `elastic`
- 비밀번호: `ELASTIC_PASSWORD` 환경변수 값 또는 로그에서 확인한 값

**접속 테스트**:
```bash
# 사용자명과 비밀번호를 포함한 요청
curl -u elastic:your-elastic-password http://localhost:9200

# 또는 브라우저에서 접속 시 인증 팝업에서 입력
# 사용자명: elastic
# 비밀번호: (위에서 설정/확인한 비밀번호)
```

---

### 1.2 Elasticsearch 인덱스 템플릿 생성

**목표**: THINKFAST 로그를 위한 인덱스 템플릿 정의

**작업 내용**:
- [ ] 인덱스 템플릿 JSON 파일 생성 (`elasticsearch/templates/thinkfast-logs-template.json`)
- [ ] 인덱스 패턴: `thinkfast-logs-*`
- [ ] 필드 매핑 정의:
  - `@timestamp`: date
  - `level`: keyword
  - `service`: keyword
  - `env`: keyword
  - `log_type`: keyword
  - `request_id`: keyword
  - `trace_id`: keyword
  - `user_id`: keyword
  - `http.*`: nested object (method, uri, status, duration_ms 등)
  - `error.*`: nested object (type, message, severity 등)
  - `external_api.*`: nested object (system, operation, duration_ms 등)
  - `scheduler.*`: nested object (job_name, job_id, status 등)
- [ ] 인덱스 설정: 샤드 수, 레플리카 수

**파일 위치**:
- `elasticsearch/templates/thinkfast-logs-template.json` (신규)

**템플릿 적용 방법**:
```bash
# 템플릿 적용 (호스트에서 실행, 보안 활성화 시 비밀번호 필요)
curl -u elastic:your-elastic-password -X PUT "localhost:9200/_index_template/thinkfast-logs-template" \
  -H 'Content-Type: application/json' \
  -d @elasticsearch/templates/thinkfast-logs-template.json

# 또는 Docker 컨테이너 내에서 실행
docker exec thinkfast-elasticsearch curl -u elastic:your-elastic-password -X PUT "localhost:9200/_index_template/thinkfast-logs-template" \
  -H 'Content-Type: application/json' \
  -d @/path/to/thinkfast-logs-template.json

# 템플릿 확인 (보안 활성화 시 비밀번호 필요)
curl -u elastic:your-elastic-password "localhost:9200/_index_template/thinkfast-logs-template?pretty"

# 모든 인덱스 템플릿 목록 확인 (보안 활성화 시 비밀번호 필요)
curl -u elastic:your-elastic-password "localhost:9200/_index_template?pretty"
```

**예상 작업 시간**: 3시간  
**담당자**: 백엔드 팀

---

## Phase 2: Kibana 구축

### 2.1 Kibana Docker 컨테이너 설정

**목표**: Kibana 8.x Docker 컨테이너 실행 및 Elasticsearch 연동

**작업 내용**:
- [ ] Kibana 이미지 선택 (kibana:8.11.0 권장, Elasticsearch와 버전 일치)
- [ ] Elasticsearch 연결 설정
- [ ] 포트 설정: 5601
- [ ] 볼륨 설정 (필요 시 설정 파일 영속화)

**참고 설정**:
```yaml
# docker-compose.yml (예시)
kibana:
  image: docker.elastic.co/kibana/kibana:8.11.0
  container_name: thinkfast-kibana
  restart: unless-stopped
  environment:
    - TZ=Asia/Seoul
    - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    - SERVER_NAME=kibana
  ports:
    - "5601:5601"
  depends_on:
    elasticsearch:
      condition: service_healthy
  networks:
    - thinkfast_backend
```

**Docker 명령어**:
```bash
# Kibana 이미지 다운로드 (공식 이미지)
docker pull docker.elastic.co/kibana/kibana:8.11.0

# Kibana 컨테이너 실행 (Elasticsearch 실행 중이어야 함)
docker run -d \
  --name thinkfast-kibana \
  -e ELASTICSEARCH_HOSTS=http://elasticsearch:9200 \
  -e SERVER_NAME=kibana \
  -p 5601:5601 \
  --link thinkfast-elasticsearch:elasticsearch \
  docker.elastic.co/kibana/kibana:8.11.0

# 또는 Docker 네트워크 사용 (권장)
docker network create thinkfast-network
docker run -d \
  --name thinkfast-kibana \
  --network thinkfast-network \
  -e ELASTICSEARCH_HOSTS=http://thinkfast-elasticsearch:9200 \
  -e SERVER_NAME=kibana \
  -p 5601:5601 \
  docker.elastic.co/kibana/kibana:8.11.0

# 컨테이너 상태 확인
docker ps | grep kibana

# 컨테이너 로그 확인
docker logs thinkfast-kibana

# Kibana 상태 확인 (초기 시작 시 몇 분 소요)
curl http://localhost:5601/api/status
```

**검증 방법**:
```bash
# Kibana 접속
# 브라우저에서 http://localhost:5601 접속
# Status: Green 표시 확인

# Kibana API 상태 확인
curl http://localhost:5601/api/status?pretty
```

**예상 작업 시간**: 1시간  
**담당자**: DevOps 팀

---

### 2.2 Kibana 인덱스 패턴 생성

**목표**: THINKFAST 로그 인덱스를 Kibana에서 탐색 가능하도록 설정

**작업 내용**:
- [ ] Kibana UI에서 인덱스 패턴 생성
- [ ] 인덱스 패턴: `thinkfast-logs-*`
- [ ] 타임스탬프 필드: `@timestamp`
- [ ] 필드 형식 확인 및 필요 시 수정

**작업 절차**:
1. Kibana 접속 (http://localhost:5601)
2. Stack Management > Index Patterns > Create index pattern
3. Index pattern name: `thinkfast-logs-*`
4. Time field: `@timestamp`
5. Create index pattern 클릭
6. Discover에서 로그 확인

**예상 작업 시간**: 1시간  
**담당자**: 백엔드 팀

---

## Phase 3: 로그 수집 파이프라인 구성

### 3.1 Filebeat Docker 컨테이너 설정 (옵션 1)

**목표**: WAS 컨테이너의 로그 파일을 읽어 Elasticsearch에 전송

**작업 내용**:
- [ ] Filebeat 이미지 선택 (docker.elastic.co/beats/filebeat:8.11.0 권장)
- [ ] Filebeat 설정 파일 생성 (`filebeat/filebeat.yml`)
- [ ] 로그 파일 경로 설정: `/var/log/thinkfast/application.log`
- [ ] Elasticsearch 직접 전송 또는 Logstash 경유
- [ ] Docker 볼륨 마운트 설정 (로그 파일 공유)

**참고 설정**:
```yaml
# docker-compose.yml (예시)
filebeat:
  image: docker.elastic.co/beats/filebeat:8.11.0
  container_name: thinkfast-filebeat
  user: root
  restart: unless-stopped
  volumes:
    - ./filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
    - thinkfast_logs:/var/log/thinkfast:ro  # WAS 로그 파일 볼륨 공유
  depends_on:
    - elasticsearch
  networks:
    - thinkfast_backend
```

**Filebeat 설정 파일 위치**:
- `filebeat/filebeat.yml` (신규)

**Docker 명령어**:
```bash
# Filebeat 이미지 다운로드
docker pull docker.elastic.co/beats/filebeat:8.11.0

# Filebeat 컨테이너 실행 (테스트용, 파일 경로는 실제 환경에 맞게 수정)
docker run -d \
  --name thinkfast-filebeat \
  --user root \
  -v $(pwd)/filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro \
  -v $(pwd)/logs:/var/log/thinkfast:ro \
  --network thinkfast-network \
  docker.elastic.co/beats/filebeat:8.11.0

# 컨테이너 상태 확인
docker ps | grep filebeat

# 컨테이너 로그 확인
docker logs thinkfast-filebeat

# Filebeat 설정 검증 (컨테이너 내에서)
docker exec thinkfast-filebeat filebeat test config

# Filebeat 출력 연결 테스트
docker exec thinkfast-filebeat filebeat test output
```

**예상 작업 시간**: 3시간  
**담당자**: DevOps 팀

---

### 3.2 Logstash Docker 컨테이너 설정 (옵션 2)

**목표**: Filebeat에서 받은 로그를 가공하여 Elasticsearch에 전송

**작업 내용**:
- [ ] Logstash 이미지 선택 (docker.elastic.co/logstash/logstash:8.11.0 권장)
- [ ] Logstash 파이프라인 설정 파일 생성 (`logstash/pipeline/logstash.conf`)
- [ ] `docs/logstash-pipeline.conf` 기반으로 설정 파일 작성
- [ ] Elasticsearch 인덱스 템플릿 경로 확인 및 수정
- [ ] Docker 볼륨 마운트 설정

**참고 설정**:
```yaml
# docker-compose.yml (예시)
logstash:
  image: docker.elastic.co/logstash/logstash:8.11.0
  container_name: thinkfast-logstash
  restart: unless-stopped
  environment:
    - TZ=Asia/Seoul  # 타임존 설정
    - "LS_JAVA_OPTS=-Xmx512m -Xms512m"  # JVM Heap 메모리 설정
  deploy:
    resources:
      limits:
        memory: 1g  # 컨테이너가 사용할 수 있는 최대 메모리 제한
  volumes:
    - /opt/deploy/logstash-pipeline.conf:/usr/share/logstash/pipeline/logstash.conf:ro  # 파이프라인 설정 파일
    - ./elasticsearch/templates:/etc/logstash/templates:ro  # Elasticsearch 템플릿 파일
  ports:
    - "5044:5044"  # Filebeat 입력 포트
  depends_on:
    - elasticsearch
  networks:
    - thinkfast_backend
```

**Logstash 파이프라인 파일 위치**:
- `/opt/deploy/logstash-pipeline.conf` (신규, `docs/logstash-pipeline.conf` 참고)

**Docker 명령어**:
```bash
# Logstash 이미지 다운로드
docker pull docker.elastic.co/logstash/logstash:8.11.0

# Logstash 컨테이너 실행 (테스트용, docker-compose.yml 사용 권장)
docker run -d \
  --name thinkfast-logstash \
  -e TZ=Asia/Seoul \
  -e "LS_JAVA_OPTS=-Xmx512m -Xms512m" \
  --memory=1g \
  -v $(pwd)/logstash/pipeline:/usr/share/logstash/pipeline:ro \
  -v $(pwd)/elasticsearch/templates:/etc/logstash/templates:ro \
  -p 5044:5044 \
  --network thinkfast-network \
  docker.elastic.co/logstash/logstash:8.11.0

# docker-compose.yml 사용 (Elasticsearch 설정 참고)
docker-compose.yml:
logstash:
  image: docker.elastic.co/logstash/logstash:8.11.0
  container_name: thinkfast-logstash
  restart: unless-stopped
  environment:
    - TZ=Asia/Seoul  # 타임존 설정
    - "LS_JAVA_OPTS=-Xmx512m -Xms512m"  # JVM Heap 메모리 설정
  deploy:
    resources:
      limits:
        memory: 1g  # 컨테이너가 사용할 수 있는 최대 메모리 제한
  volumes:
    - /opt/deploy/logstash-pipeline.conf:/usr/share/logstash/pipeline/logstash.conf:ro  # 파이프라인 설정 파일
    - ./elasticsearch/templates:/etc/logstash/templates:ro  # Elasticsearch 템플릿 파일
  ports:
    - "5044:5044"  # Filebeat 입력 포트
  depends_on:
    - elasticsearch
  networks:
    - thinkfast_backend

# 컨테이너 상태 확인
docker ps | grep logstash

# 컨테이너 로그 확인
docker logs thinkfast-logstash

# Logstash 설정 검증 (컨테이너 내에서)
docker exec thinkfast-logstash logstash --config.test_and_exit --path.settings=/usr/share/logstash/config
```

**예상 작업 시간**: 4시간  
**담당자**: DevOps 팀

**참고**: Filebeat에서 Elasticsearch로 직접 전송하는 경우 이 단계는 생략 가능

---

## Phase 4: Docker Compose 통합

### 4.1 docker-compose.yml 생성

**목표**: 모든 서비스를 하나의 docker-compose.yml로 통합 관리

**작업 내용**:
- [ ] 기존 서비스 확인 (thinkfast-backend, mariadb, redis 등)
- [ ] Elasticsearch 서비스 추가
- [ ] Kibana 서비스 추가
- [ ] Filebeat 서비스 추가 (또는 Logstash)
- [ ] 네트워크 설정 통합
- [ ] 볼륨 설정 통합
- [ ] 의존성 설정 (depends_on)

**파일 위치**:
- `docker-compose.yml` (신규 또는 기존 파일 수정)

**완전한 docker-compose.yml 예시** (기존 서비스 + ELK 스택):
```yaml
version: "3.8"

services:
  # 기존 서비스
  mariadb:
    image: mariadb:11
    container_name: mariadb
    restart: always
    environment:
      - TZ=Asia/Seoul
      - MYSQL_ROOT_PASSWORD=@aa6368601
      - MYSQL_DATABASE=thinkfast
    volumes:
      - thinkfast_mariadb_data:/var/lib/mysql
    ports:
      - "3306:3306"
    networks:
      - thinkfast_backend

  redis:
    image: redis:7-alpine
    container_name: thinkfast-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - thinkfast_backend
    command: redis-server --appendonly yes
    restart: unless-stopped

  qdrant:
    image: qdrant/qdrant:v1.7.0
    container_name: thinkfast-qdrant
    restart: unless-stopped
    environment:
      - TZ=Asia/Seoul
      - QDRANT__SERVICE__GRPC_PORT=6334
      - QDRANT__SERVICE__HTTP_PORT=6333
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - qdrant_data:/qdrant/storage
    networks:
      - thinkfast_backend

  thinkfast-backend:
    image: kwakchaewon/thinkfast_backend:latest
    container_name: thinkfast-app
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mariadb://mariadb:3306/thinkfast?characterEncoding=UTF-8&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: '@aa6368601'
      SPRING_FLYWAY_URL: jdbc:mariadb://mariadb:3306/thinkfast?characterEncoding=UTF-8&serverTimezone=UTC
      SPRING_FLYWAY_USER: root
      SPRING_FLYWAY_PASSWORD: '@aa6368601'
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      JWT_SECRET: your-secret-key-should-be-very-long-and-secure-at-least-256-bits
      JWT_TOKEN_VALIDITY_IN_SECONDS: 3600
      JWT_REFRESH_TOKEN_VALIDITY_IN_SECONDS: 604800
      TZ: Asia/Seoul
      GEMINI_API_KEY: AIzaSyBb4bfYr4gEpvWijCcwaWZXKqHRRlQclJ4
      QDRANT_URL: http://qdrant:6333
    volumes:
      - thinkfast_logs:/var/log/thinkfast  # 로그 파일 볼륨 (Filebeat 접근용)
    depends_on:
      - mariadb
      - redis
      - qdrant
    networks:
      - thinkfast_backend
    restart: unless-stopped

  thinkfast-frontend:
    image: kwakchaewon/thinkfast_frontend:latest
    container_name: thinkfast-frontend
    restart: unless-stopped
    ports:
      - "80:80"
    environment:
      - TZ=Asia/Seoul
      - BACKEND_URL=http://thinkfast-backend:8080
      - NGINX_HOST=localhost
      - NGINX_PORT=80
    depends_on:
      - thinkfast-backend
    networks:
      - thinkfast_backend

  # ELK 스택
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: thinkfast-elasticsearch
    restart: unless-stopped
    environment:
      - TZ=Asia/Seoul
      - discovery.type=single-node
      - xpack.security.enabled=false  # 개발 환경 (운영에서는 true 권장)
      - "ES_JAVA_OPTS=-Xms2g -Xmx2g"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
      - "9300:9300"
    networks:
      - thinkfast_backend
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    container_name: thinkfast-kibana
    restart: unless-stopped
    environment:
      - TZ=Asia/Seoul
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
      - SERVER_NAME=kibana
    ports:
      - "5601:5601"
    depends_on:
      elasticsearch:
        condition: service_healthy
    networks:
      - thinkfast_backend

  filebeat:
    image: docker.elastic.co/beats/filebeat:8.11.0
    container_name: thinkfast-filebeat
    user: root
    restart: unless-stopped
    volumes:
      - ./filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
      - thinkfast_logs:/var/log/thinkfast:ro  # WAS 로그 파일 볼륨 공유
    depends_on:
      - elasticsearch
    networks:
      - thinkfast_backend

volumes:
  thinkfast_mariadb_data:
    external: true
  redis_data:
    external: false
  qdrant_data:
    external: false
  elasticsearch_data:
    external: false
  thinkfast_logs:
    external: false  # WAS와 Filebeat가 공유하는 로그 볼륨

networks:
  thinkfast_backend:
    driver: bridge
```

**주요 변경 사항**:
- 기존 네트워크 `thinkfast_backend` 사용
- 기존 스타일 유지 (restart: unless-stopped, TZ=Asia/Seoul 등)
- Elasticsearch 이미지: `docker.elastic.co/elasticsearch/elasticsearch:8.11.0` (공식 이미지)
- Kibana 이미지: `docker.elastic.co/kibana/kibana:8.11.0` (공식 이미지)
- 로그 볼륨: `thinkfast_logs` (Docker 볼륨으로 공유)
- Elasticsearch healthcheck 추가 (Kibana 시작 전 준비 상태 확인)
- `thinkfast-backend` 서비스에 로그 볼륨 추가: `thinkfast_logs:/var/log/thinkfast`

**참고**: 
- Jenkins 서비스는 기존 docker-compose.yml에 포함되어 있지만 ELK 스택과는 무관하므로 위 예시에서는 생략했습니다. 필요시 기존 docker-compose.yml에 ELK 서비스만 추가하면 됩니다.
- 로그 파일은 Docker 볼륨(`thinkfast_logs`)을 사용하여 WAS와 Filebeat가 공유합니다.
- `thinkfast-backend` 컨테이너 내부에서 `/var/log/thinkfast/application.log` 경로로 로그가 출력되어야 합니다.

**Docker Compose 명령어**:
```bash
# 모든 이미지 다운로드 (선택사항, docker-compose up 시 자동으로 다운로드됨)
docker-compose pull

# 모든 서비스 시작 (백그라운드)
docker-compose up -d

# 특정 서비스만 시작
docker-compose up -d elasticsearch kibana filebeat

# 모든 서비스 상태 확인
docker-compose ps

# 특정 서비스 로그 확인
docker-compose logs -f elasticsearch
docker-compose logs -f kibana
docker-compose logs -f filebeat

# 모든 서비스 로그 확인
docker-compose logs -f

# 서비스 재시작
docker-compose restart elasticsearch
docker-compose restart kibana

# 모든 서비스 중지 (컨테이너는 유지)
docker-compose stop

# 모든 서비스 중지 및 컨테이너 제거
docker-compose down

# 볼륨까지 함께 제거 (주의: 데이터 삭제됨)
docker-compose down -v

# 서비스 재빌드 및 시작 (기존 컨테이너 제거 후 재생성)
docker-compose up -d --force-recreate
```

**예상 작업 시간**: 4시간  
**담당자**: DevOps 팀

---

### 4.2 로그 디렉토리 공유 설정

**목표**: WAS 컨테이너와 Filebeat 컨테이너가 로그 파일을 공유

**작업 내용**:
- [ ] WAS 컨테이너 로그 파일 경로 확인 (`/var/log/thinkfast/application.log`)
- [ ] 호스트 볼륨 또는 Docker 볼륨 생성
- [ ] WAS 컨테이너와 Filebeat 컨테이너에 볼륨 마운트
- [ ] 파일 권한 확인 (Filebeat가 읽을 수 있도록)

**방법 1: 호스트 볼륨 사용**

**호스트 경로 추천**:
- **추천 경로 1: `./logs`** (가장 일반적, 권장)
  - 프로젝트 루트 디렉토리의 `logs` 폴더
  - 장점: 간단하고 직관적, 프로젝트와 함께 관리 가능, 이식성 좋음
  - Windows: `E:\dev\thinkfast\logs`
  - Linux/Mac: `./logs` (프로젝트 루트 기준)

- **추천 경로 2: `./data/logs`**
  - 프로젝트 루트의 `data/logs` 폴더
  - 장점: 데이터 관련 파일들을 한 곳에 모아 관리
  - Windows: `E:\dev\thinkfast\data\logs`

- **기타 옵션:**
  - 절대 경로: `E:/logs/thinkfast` (Windows) 또는 `/var/log/thinkfast` (Linux)
  - 장점: 고정된 위치, 프로젝트와 독립적
  - 단점: 이식성 떨어짐, 권한 설정 필요할 수 있음

```yaml
# docker-compose.yml (호스트 볼륨 사용 예시)
services:
  thinkfast-backend:
    volumes:
      - ./logs:/var/log/thinkfast  # 호스트 경로: 프로젝트 루트/logs

  filebeat:
    volumes:
      - ./logs:/var/log/thinkfast:ro  # 읽기 전용 마운트
```

**방법 2: Docker 볼륨 사용** (권장, 위 예시에서 사용)
```yaml
# docker-compose.yml
volumes:
  thinkfast_logs:
    external: false

services:
  thinkfast-backend:
    volumes:
      - thinkfast_logs:/var/log/thinkfast

  filebeat:
    volumes:
      - thinkfast_logs:/var/log/thinkfast:ro
```

**참고**: 위 구성 예시에서는 Docker 볼륨(`thinkfast_logs`)을 사용하여 WAS와 Filebeat가 로그 파일을 공유합니다. 호스트 볼륨(`./logs`)을 사용하려면 `thinkfast_logs` 대신 `./logs:/var/log/thinkfast` 형태로 변경하면 됩니다.

**로그 디렉토리 생성 및 확인 명령어**:

**Linux/Mac**:
```bash
# 호스트에서 로그 디렉토리 생성 (필요 시)
mkdir -p ./logs
chmod 755 ./logs
```

**Windows (PowerShell)**:
```powershell
# 호스트에서 로그 디렉토리 생성 (필요 시)
New-Item -ItemType Directory -Force -Path .\logs
```

**Windows (CMD)**:
```cmd
# 호스트에서 로그 디렉토리 생성 (필요 시)
mkdir logs
```

**로그 확인 명령어** (모든 OS 공통):
```bash

# WAS 컨테이너에서 로그 파일 확인
docker exec thinkfast-backend ls -la /var/log/thinkfast/

# Filebeat 컨테이너에서 로그 파일 접근 확인
docker exec thinkfast-filebeat ls -la /var/log/thinkfast/

# 로그 파일 권한 확인
docker exec thinkfast-backend stat -c "%a %n" /var/log/thinkfast/application.log

# 실시간 로그 모니터링 (호스트에서)
tail -f ./logs/application.log

# 로그 파일 크기 확인
docker exec thinkfast-backend du -sh /var/log/thinkfast/
```

**예상 작업 시간**: 1시간  
**담당자**: DevOps 팀

---

## Phase 5: Kibana 대시보드 구성

### 5.1 기본 대시보드 생성

**목표**: 주요 로그 타입별 시각화 대시보드 생성

**작업 내용**:
- [ ] HTTP 요청 대시보드
  - 요청 수 (시간별)
  - 응답 시간 분포 (P50, P95, P99)
  - HTTP 상태 코드 분포
  - 느린 요청 (3초 이상) 추이
  - 인기 엔드포인트 Top 10
- [ ] 예외 로그 대시보드
  - 예외 발생 추이
  - 예외 타입별 분포
  - 심각도별 분포 (error, critical)
  - 최근 에러 로그 목록
- [ ] 외부 API 호출 대시보드
  - API 호출 수 (시스템별)
  - 평균 응답 시간 (시스템별)
  - 실패율 (시스템별)
  - 재시도 횟수 분석
- [ ] 스케줄러 작업 대시보드
  - 작업 실행 추이
  - 실행 시간 분포
  - 성공/실패율
  - 실패한 작업 목록

**작업 절차**:
1. Kibana > Visualize Library > Create visualization
2. 각 메트릭별 Visualization 생성
3. Dashboard 생성 및 Visualization 추가
4. Dashboard 저장 및 공유

**예상 작업 시간**: 6시간  
**담당자**: 백엔드 팀 / DevOps 팀

---

### 5.2 알림 규칙 설정 (선택사항)

**목표**: 특정 조건 발생 시 알림

**작업 내용**:
- [ ] Kibana Alerting 기능 사용 (또는 외부 모니터링 도구 연동)
- [ ] 알림 규칙 정의:
  - Critical 에러 발생 시
  - 느린 요청 비율이 임계값 초과 시
  - 외부 API 실패율이 임계값 초과 시
  - 스케줄러 작업 실패 시
- [ ] 알림 채널 설정 (이메일, Slack 등)

**예상 작업 시간**: 4시간  
**담당자**: DevOps 팀

---

## Phase 6: 연동 테스트 및 검증

### 6.1 로그 수집 테스트

**목표**: WAS 로그가 Elasticsearch에 정상적으로 수집되는지 확인

**작업 내용**:
- [ ] docker-compose.yml로 모든 서비스 실행
  ```bash
  # 모든 서비스 시작
  docker-compose up -d
  
  # 서비스 상태 확인 (모든 서비스가 Running 상태인지 확인)
  docker-compose ps
  ```

- [ ] WAS에 테스트 요청 전송
  ```bash
  # HTTP 요청 테스트
  curl -X GET http://localhost:8080/api/test
  
  # 또는 브라우저에서 API 호출
  ```

- [ ] Elasticsearch에서 로그 확인
  ```bash
  # 모든 로그 검색
  curl "http://localhost:9200/thinkfast-logs-*/_search?pretty" -H 'Content-Type: application/json' -d'
  {
    "query": { "match_all": {} },
    "size": 10
  }'
  
  # HTTP 요청 로그만 검색
  curl "http://localhost:9200/thinkfast-logs-*/_search?pretty" -H 'Content-Type: application/json' -d'
  {
    "query": {
      "term": {
        "log_type": "http_request"
      }
    },
    "size": 10
  }'
  
  # 에러 로그만 검색
  curl "http://localhost:9200/thinkfast-logs-*/_search?pretty" -H 'Content-Type: application/json' -d'
  {
    "query": {
      "term": {
        "level": "ERROR"
      }
    },
    "size": 10
  }'
  
  # 인덱스 목록 확인
  curl "http://localhost:9200/_cat/indices/thinkfast-logs-*?v"
  
  # 인덱스 통계 확인
  curl "http://localhost:9200/thinkfast-logs-*/_stats?pretty"
  ```

- [ ] Kibana Discover에서 로그 확인
  - 브라우저에서 http://localhost:5601 접속
  - Discover 메뉴 선택
  - 인덱스 패턴 `thinkfast-logs-*` 선택
  - 로그 확인

- [ ] 각 로그 타입별 필드 확인
  ```bash
  # 인덱스 매핑 확인 (필드 구조 확인)
  curl "http://localhost:9200/thinkfast-logs-*/_mapping?pretty"
  
  # 특정 문서의 필드 확인
  curl "http://localhost:9200/thinkfast-logs-*/_search?pretty" -H 'Content-Type: application/json' -d'
  {
    "query": { "match_all": {} },
    "size": 1
  }'
  ```

**검증 체크리스트**:
- [ ] HTTP 요청 로그가 Elasticsearch에 저장됨
- [ ] 로그 타입별 필드가 올바르게 매핑됨
- [ ] 타임스탬프 필드가 올바르게 파싱됨
- [ ] MDC 필드 (request_id, trace_id, user_id)가 포함됨

**예상 작업 시간**: 2시간  
**담당자**: 백엔드 팀

---

### 6.2 성능 테스트

**목표**: ELK 스택이 프로덕션 로그 볼륨을 처리할 수 있는지 확인

**작업 내용**:
- [ ] 로그 생성량 측정 (일일 로그 수, 데이터 크기)
  ```bash
  # 일일 로그 수 확인
  curl "http://localhost:9200/thinkfast-logs-*/_count?pretty"
  
  # 오늘 생성된 로그 수 확인
  curl "http://localhost:9200/thinkfast-logs-*/_count?pretty" -H 'Content-Type: application/json' -d'
  {
    "query": {
      "range": {
        "@timestamp": {
          "gte": "now/d"
        }
      }
    }
  }'
  
  # 인덱스별 문서 수 확인
  curl "http://localhost:9200/_cat/indices/thinkfast-logs-*?v&h=index,docs.count,store.size"
  ```

- [ ] Elasticsearch 인덱싱 속도 확인
  ```bash
  # 인덱싱 통계 확인
  curl "http://localhost:9200/thinkfast-logs-*/_stats/indexing?pretty"
  
  # 노드 통계 확인
  curl "http://localhost:9200/_nodes/stats?pretty"
  ```

- [ ] 디스크 사용량 모니터링
  ```bash
  # Elasticsearch 컨테이너 디스크 사용량 확인
  docker exec thinkfast-elasticsearch du -sh /usr/share/elasticsearch/data
  
  # 호스트에서 볼륨 디스크 사용량 확인
  docker volume inspect thinkfast_elasticsearch_data
  
  # 전체 디스크 사용량 확인
  docker system df
  docker system df -v
  ```

- [ ] 메모리 사용량 모니터링
  ```bash
  # 컨테이너 리소스 사용량 확인
  docker stats thinkfast-elasticsearch
  docker stats thinkfast-kibana
  docker stats thinkfast-filebeat
  
  # Elasticsearch JVM 메모리 확인
  curl "http://localhost:9200/_nodes/stats/jvm?pretty"
  ```

- [ ] 필요 시 리소스 조정 (메모리, 디스크 등)
  ```bash
  # docker-compose.yml에서 메모리 제한 수정 후 재시작
  docker-compose down
  # docker-compose.yml 수정
  docker-compose up -d
  ```

**예상 작업 시간**: 3시간  
**담당자**: DevOps 팀

---

### 6.3 대시보드 검증

**목표**: 생성한 대시보드가 정상적으로 데이터를 표시하는지 확인

**작업 내용**:
- [ ] 각 대시보드에서 데이터 표시 확인
- [ ] 필터링 기능 테스트
- [ ] 시간 범위 선택 기능 테스트
- [ ] 대시보드 반응 속도 확인

**예상 작업 시간**: 2시간  
**담당자**: 백엔드 팀

---

## Phase 7: 문서화 및 운영 가이드

### 7.1 운영 가이드 작성

**목표**: ELK 스택 운영을 위한 문서 작성

**작업 내용**:
- [ ] ELK 스택 시작/중지 방법
- [ ] 로그 인덱스 관리 (인덱스 라이프사이클 정책, 보관 기간)
- [ ] 디스크 공간 관리 (오래된 인덱스 삭제)
- [ ] Elasticsearch 백업/복구 방법
- [ ] 문제 해결 가이드 (로그 수집 안 됨, 인덱싱 실패 등)

**파일 위치**:
- `docs/ELK_운영가이드.md` (신규)

**예상 작업 시간**: 4시간  
**담당자**: DevOps 팀

---

### 7.2 인덱스 라이프사이클 정책 설정

**목표**: 오래된 로그 자동 삭제 및 디스크 공간 관리

**작업 내용**:
- [ ] 인덱스 라이프사이클 정책 정의 (ILM Policy)
- [ ] 보관 기간 설정 (예: 30일)
- [ ] Hot/Warm/Cold 단계 설정 (선택사항)
- [ ] 정책 적용

**작업 절차**:
1. Kibana > Stack Management > Index Lifecycle Policies
2. 정책 생성 및 설정
3. 인덱스 템플릿에 정책 연결

**API를 통한 ILM 정책 설정** (선택사항):
```bash
# ILM 정책 생성 (30일 후 삭제)
curl -X PUT "http://localhost:9200/_ilm/policy/thinkfast-logs-policy" \
  -H 'Content-Type: application/json' \
  -d'
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d"
          }
        }
      },
      "delete": {
        "min_age": "30d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}'

# ILM 정책 확인
curl "http://localhost:9200/_ilm/policy/thinkfast-logs-policy?pretty"
```

**예상 작업 시간**: 2시간  
**담당자**: DevOps 팀

---

## 구현 우선순위

1. **Phase 1-2 (필수)**: Elasticsearch, Kibana 구축
2. **Phase 3 (필수)**: 로그 수집 파이프라인 구성
3. **Phase 4 (필수)**: Docker Compose 통합
4. **Phase 5 (권장)**: Kibana 대시보드 구성
5. **Phase 6 (필수)**: 연동 테스트 및 검증
6. **Phase 7 (권장)**: 문서화 및 운영 가이드

---

## 예상 총 작업 시간

- **최소 구성** (Phase 1-4, 6): 약 20시간
- **권장 구성** (Phase 1-7): 약 35시간

---

## 유용한 Docker 명령어 모음

### 기본 명령어
```bash
# 모든 컨테이너 상태 확인
docker ps -a

# 특정 컨테이너 로그 확인
docker logs <container_name>
docker logs -f <container_name>  # 실시간 로그 확인

# 컨테이너 내부 접속
docker exec -it <container_name> /bin/bash

# 컨테이너 재시작
docker restart <container_name>

# 컨테이너 중지 및 제거
docker stop <container_name>
docker rm <container_name>

# 사용하지 않는 이미지 정리
docker image prune -a

# 사용하지 않는 볼륨 정리 (주의: 데이터 삭제됨)
docker volume prune
```

### Elasticsearch 관련
```bash
# Elasticsearch 클러스터 상태
curl http://localhost:9200/_cluster/health?pretty

# 인덱스 목록
curl "http://localhost:9200/_cat/indices?v"

# 특정 인덱스 삭제 (주의: 데이터 삭제됨)
curl -X DELETE "http://localhost:9200/thinkfast-logs-2025.01.15"

# 인덱스 닫기 (읽기 전용)
curl -X POST "http://localhost:9200/thinkfast-logs-2025.01.15/_close"

# 인덱스 열기
curl -X POST "http://localhost:9200/thinkfast-logs-2025.01.15/_open"
```

### Docker Compose 관련
```bash
# 서비스 상태 확인
docker-compose ps

# 특정 서비스 로그 확인
docker-compose logs -f <service_name>

# 서비스 재시작
docker-compose restart <service_name>

# 서비스 재빌드 (Dockerfile 변경 시)
docker-compose up -d --build <service_name>

# 환경 변수 파일 지정
docker-compose --env-file .env.prod up -d
```

---

## 참고 자료

- **로그 스키마 문서**: `docs/logging-schema.md`
- **Logstash 파이프라인 예시**: `docs/logstash-pipeline.conf`
- **Filebeat 설정 예시**: `docs/filebeat-config.yml`
- **Elasticsearch 공식 문서**: https://www.elastic.co/guide/en/elasticsearch/reference/8.11/index.html
- **Kibana 공식 문서**: https://www.elastic.co/guide/en/kibana/8.11/index.html
- **Filebeat 공식 문서**: https://www.elastic.co/guide/en/beats/filebeat/8.11/index.html
- **Docker Compose 공식 문서**: https://docs.docker.com/compose/

---

**작성일**: 2025-01-15  
**최종 수정일**: 2025-01-15
