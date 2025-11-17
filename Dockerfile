FROM eclipse-temurin:8-jre-jammy
ENV JAR_FILE "thinkfast-0.0.1-SNAPSHOT.jar"
ENV RUN_TYPE "prod"
ADD ./build/libs/${JAR_FILE} /
CMD ["java", "-Dspring.profiles.active=${RUN_TYPE}", "-jar", "/thinkfast-0.0.1-SNAPSHOT.jar"]
EXPOSE 8080