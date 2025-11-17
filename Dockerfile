# GitHub Actions에서 바로 이미지를 빌드할 수 있도록 멀티 스테이지 빌드 구성
FROM gradle:7.6.3-jdk8 AS builder
WORKDIR /workspace

COPY . .

RUN chmod +x gradlew
RUN ./gradlew clean build -x test

FROM eclipse-temurin:8-jre-jammy
# bootJar로 생성된 실행 가능한 fat JAR만 복사 (plain JAR 제외)
COPY --from=builder /workspace/build/libs/thinkfast-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]