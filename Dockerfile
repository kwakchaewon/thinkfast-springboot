FROM gradle:7.6.3-jdk8 AS builder
WORKDIR /workspace
COPY . .
# gradlew의 Windows line ending(CRLF) 문제 해결 및 실행 권한 부여
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
# gradlew를 사용하여 프로젝트의 Gradle wrapper 버전(8.13) 사용
# --no-daemon: Docker 빌드에서는 daemon 사용 안 함 (권장)
# --stacktrace: 상세한 에러 스택 트레이스 출력
RUN ./gradlew clean bootJar -x test --no-daemon --stacktrace

FROM eclipse-temurin:8-jre-jammy
ENV RUN_TYPE=prod
WORKDIR /app
COPY --from=builder /workspace/build/libs/thinkfast-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=${RUN_TYPE}", "-jar", "/app/app.jar"]