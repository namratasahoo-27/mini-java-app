# Multi-stage Dockerfile for Java Spring Boot Application
# Build stage
FROM maven:3.9.4-eclipse-temurin-11 AS builder

WORKDIR /workspace

# Copy dependency management files first for better layer caching
COPY pom.xml .

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:11-jre

WORKDIR /app

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy the built artifact from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Set ownership to non-root user
RUN chown -R appuser:appuser /app

# Create necessary directories for application
RUN mkdir -p /app/config /app/logs /app/temp /app/uploads && \
    chown -R appuser:appuser /app/config /app/logs /app/temp /app/uploads

# Switch to non-root user
USER appuser

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    PORT=8080 \
    SERVER_HOST=0.0.0.0 \
    CONFIG_DIR=/app/config \
    LOG_DIR=/app/logs \
    TEMP_DIR=/app/temp \
    UPLOAD_DIR=/app/uploads \
    TZ=UTC

# Expose application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
