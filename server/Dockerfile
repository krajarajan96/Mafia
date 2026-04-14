# Stage 1: Build
FROM gradle:8.10-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :server:shadowJar --no-daemon --stacktrace

# Debug: list the output to verify JAR exists
RUN ls -la server/build/libs/

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/server/build/libs/mafia-server.jar ./mafia-server.jar

# Verify the main class is in the manifest
RUN java -jar mafia-server.jar --version || true

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "mafia-server.jar"]