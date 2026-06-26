# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY gradle/wrapper gradle/wrapper
COPY gradle/libs.versions.toml gradle/libs.versions.toml
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
COPY api api
COPY server server
COPY client client
RUN ./gradlew :server:bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=build /app/server/build/libs/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
