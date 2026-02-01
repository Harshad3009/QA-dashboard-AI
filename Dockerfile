# --- Stage 1: Build the Application ---
FROM gradle:8.5-jdk21 AS builder

# Set working directory
WORKDIR /app

# Copy gradle files first (for better caching)
COPY build.gradle settings.gradle ./
COPY src ./src

# Build the JAR file (skip tests to speed up deployment)
RUN gradle bootJar -x test --no-daemon

# --- Stage 2: Run the Application ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]