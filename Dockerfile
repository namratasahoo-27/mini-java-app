# Multi-stage Dockerfile for Java Maven Application
# Build Stage
FROM maven:3.9.4-eclipse-temurin-11 AS builder

WORKDIR /workspace

# Copy Maven POM and download dependencies (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build application
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime Stage
FROM eclipse-temurin:11-jre-alpine

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

# Create required directories
RUN mkdir -p /app/config /app/logs /app/temp /app/uploads && \
    chown -R appuser:appuser /app

# Copy JAR from builder stage
COPY --from=builder --chown=appuser:appuser /workspace/target/*.jar /app/app.jar

# Copy configuration files
COPY --chown=appuser:appuser src/main/resources/application.properties /app/config/

# Switch to non-root user
USER appuser

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SERVER_PORT=8080 \
    SERVER_HOST=0.0.0.0 \
    APP_CONFIG_PATH=/app/config/application.properties \
    LOG_PATH=/app/logs \
    TEMP_PATH=/app/temp \
    UPLOAD_PATH=/app/uploads

# Expose application port
EXPOSE 8080

# Set entrypoint
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
