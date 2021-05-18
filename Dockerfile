FROM openjdk:11-jdk-slim
EXPOSE 50051
ARG JAR_FILE=build/libs/*-all.jar
ADD ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
