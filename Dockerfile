# Build the Spring Boot application with the project's Java 21 Maven toolchain.
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B package

# Run with a smaller Java 21 runtime image as an unprivileged user.
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S msetu && adduser -S -G msetu msetu
COPY --from=build --chown=msetu:msetu /build/target/summitsky-1.0.0.jar /app/app.jar
USER msetu

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
