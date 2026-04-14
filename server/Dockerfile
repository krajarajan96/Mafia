FROM gradle:8.10-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :server:shadowJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/server/build/libs/mafia-server.jar ./mafia-server.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "mafia-server.jar"]
