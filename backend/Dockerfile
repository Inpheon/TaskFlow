FROM gradle:8.14.4-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY backend backend
RUN gradle :backend:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/backend/build/libs/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
