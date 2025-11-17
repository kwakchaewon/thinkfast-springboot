FROM gradle:7.6.3-jdk8 AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew clean bootJar -x test

FROM eclipse-temurin:8-jre-jammy
ENV RUN_TYPE "prod"
WORKDIR /app
COPY --from=builder /workspace/build/libs/thinkfast-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=${RUN_TYPE}", "-jar", "/app/app.jar"]