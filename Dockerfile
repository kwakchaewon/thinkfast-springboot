FROM gradle:7.6.3-jdk8 AS builder
WORKDIR /workspace
COPY . .
# gradle 이미지에는 gradle이 이미 설치되어 있으므로 gradle 명령어 직접 사용
# gradlew의 line ending 문제를 피하기 위해 gradle 명령어 사용
RUN gradle clean bootJar -x test

FROM eclipse-temurin:8-jre-jammy
ENV RUN_TYPE=prod
WORKDIR /app
COPY --from=builder /workspace/build/libs/thinkfast-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=${RUN_TYPE}", "-jar", "/app/app.jar"]