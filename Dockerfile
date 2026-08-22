# Multi-stage Dockerfile for Settle Spring Boot Application

# Stage 1: Build stage with JDK & Maven
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage with minimal JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy built jar from builder stage
COPY --from=builder /app/target/settle-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
