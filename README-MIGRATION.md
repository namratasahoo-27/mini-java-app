# Database Code Modernization - Migration Guide

## Overview
This application has been successfully modernized from a legacy MySQL-based Java application to a cloud-native Spring Boot application with PostgreSQL support.

## Key Changes

### 1. Database Migration: MySQL → PostgreSQL
- **Before**: Manual JDBC connections with MySQL
- **After**: Spring Boot DataSource with HikariCP connection pooling and PostgreSQL

### 2. Dependency Updates (pom.xml)
- Removed: `mysql-connector-java`
- Added:
  - `postgresql` (JDBC driver)
  - `spring-boot-starter-data-jpa` (ORM and connection management)
  - `HikariCP` (connection pooling)
  - `flyway-core` and `flyway-database-postgresql` (database migrations)
  - `spring-boot-starter-actuator` (health checks and metrics)
  - `micrometer-registry-prometheus` (metrics)
  - `spring-retry` (resilience)
  - `spring-boot-starter-logging` (structured logging)

### 3. Configuration Externalization (application.properties)
- **Before**: Hardcoded credentials, hosts, and ports in code and config files
- **After**: Environment variables with sensible defaults using `${VAR_NAME:default_value}` pattern

#### Key Configuration Changes:
- Database URL: `${DATABASE_URL:jdbc:postgresql://localhost:5432/mini_app_db}`
- Database credentials: `${DATABASE_USERNAME:postgres}` and `${DATABASE_PASSWORD:}`
- Redis configuration: All settings externalized
- File paths: Relative paths instead of absolute paths
- All secrets: Moved to environment variables

### 4. Code Modernization (DatabaseService.java)
- **Before**: Manual JDBC connection management, hardcoded values, System.out logging
- **After**:
  - Spring `@Service` component with dependency injection
  - DataSource injection for connection pooling
  - SLF4J logging for structured logs
  - Parameterized queries to prevent SQL injection
  - Try-with-resources for proper resource management
  - `@Retryable` annotation for transient failure handling
  - Connection validation for health checks
  - PostgreSQL-specific error handling

### 5. Application Modernization (MiniApp.java)
- **Before**: Standalone Java application with hardcoded paths
- **After**:
  - Spring Boot application with `@SpringBootApplication`
  - `CommandLineRunner` for initialization
  - Externalized directory paths
  - Proper logging with SLF4J
  - Health check integration

### 6. Database Schema Management
- **New**: Flyway migration support with versioned SQL scripts
- Initial schema: `src/main/resources/db/migration/V1__initial_schema.sql`
- PostgreSQL-specific features:
  - `BIGSERIAL` for auto-increment IDs
  - `TIMESTAMP WITH TIME ZONE` for timestamps
  - Trigger functions for automatic `updated_at` columns

### 7. Health Checks and Monitoring
- **New**: Custom health indicator (`DatabaseHealthIndicator.java`)
- Spring Boot Actuator endpoints enabled:
  - `/actuator/health` - Liveness and readiness probes
  - `/actuator/metrics` - Application metrics
  - `/actuator/prometheus` - Prometheus metrics export

### 8. Security Improvements
- Removed hardcoded credentials from source code
- All sensitive data moved to environment variables
- Parameterized queries prevent SQL injection
- Environment variable template provided (`.env.example`)

## Migration Steps

### Prerequisites
1. PostgreSQL 16+ installed and running
2. Java 17+ installed
3. Maven 3.8+ installed

### Step 1: Database Setup
```bash
# Create PostgreSQL database
createdb mini_app_db

# Or using psql
psql -U postgres
CREATE DATABASE mini_app_db;
\q
```

### Step 2: Environment Configuration
```bash
# Copy environment template
cp .env.example .env

# Edit .env with your actual values
nano .env

# Export environment variables (Linux/Mac)
export $(cat .env | xargs)

# Or use spring-boot-maven-plugin with .env file
```

### Step 3: Build Application
```bash
mvn clean install
```

### Step 4: Run Database Migrations
```bash
# Flyway migrations run automatically on application startup
# Or run manually:
mvn flyway:migrate
```

### Step 5: Run Application
```bash
# Using Maven
mvn spring-boot:run

# Or run JAR
java -jar target/mini-java-app-1.0.0.jar
```

### Step 6: Verify Application
```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","components":{"database":{"status":"UP","details":{...}}}}

# Check metrics
curl http://localhost:8080/actuator/metrics

# Check Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

## Cloud Deployment

### Docker Support
Create a `Dockerfile`:
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/mini-java-app-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:
```bash
docker build -t mini-app:latest .
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/mini_app_db \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=your_password \
  mini-app:latest
```

### Kubernetes Deployment
The application now supports:
- Liveness probe: `GET /actuator/health/liveness`
- Readiness probe: `GET /actuator/health/readiness`
- Metrics scraping: `GET /actuator/prometheus`

Example Kubernetes deployment:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mini-app
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: mini-app
        image: mini-app:latest
        ports:
        - containerPort: 8080
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: url
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
```

## Resolved Issues

### Critical Issues Fixed (8)
1. ✅ Hardcoded database host and port → Externalized to `${DATABASE_URL}`
2. ✅ Hardcoded database name → Included in `${DATABASE_URL}`
3. ✅ Hardcoded database credentials → Externalized to `${DATABASE_USERNAME}` and `${DATABASE_PASSWORD}`
4. ✅ Manual JDBC connection management → Replaced with Spring DataSource and HikariCP
5. ✅ Hardcoded database URL in config → Externalized with environment variables
6. ✅ Database credentials in config file → Externalized to environment variables
7. ✅ Hardcoded Redis cache configuration → Externalized to environment variables
8. ✅ Missing database schema definition → Created Flyway migrations

### High Priority Issues Fixed (9)
1. ✅ Explicit JDBC driver loading → Removed (auto-registered in JDBC 4.0+)
2. ✅ Singleton database connection → Replaced with connection pooling
3. ✅ SQL injection vulnerability risk → Implemented parameterized queries
4. ✅ Hardcoded database URL in config → Externalized
5. ✅ Hardcoded connection pool settings → Externalized and configurable
6. ✅ No database migration tool → Integrated Flyway
7. ✅ No ORM framework → Integrated Spring Data JPA with Hibernate
8. ✅ MySQL-specific JDBC driver → Migrated to PostgreSQL driver
9. ✅ Database/cache logic coupling → Separated concerns with Spring services

### Medium Priority Issues Fixed (5)
1. ✅ Missing connection validation → Implemented health checks with `isConnectionValid()`
2. ✅ Hardcoded query timeout → Externalized to configuration
3. ✅ No transaction management → Spring Boot provides transaction support via `@Transactional`
4. ✅ No database metrics/observability → Integrated Micrometer and Actuator
5. ✅ Resource leak risk → Implemented try-with-resources pattern

### Low Priority Issues Fixed (2)
1. ✅ Generic SQLException handling → Implemented specific error handling with SQL state checking
2. ✅ System.out/System.err logging → Replaced with SLF4J structured logging

## PostgreSQL-Specific Changes

### Type Mappings
- `INT AUTO_INCREMENT` → `BIGSERIAL`
- `DATETIME` → `TIMESTAMP WITH TIME ZONE`
- `VARCHAR` → `VARCHAR` (same, but case-sensitive in PostgreSQL)
- `TEXT` → `TEXT` (same)
- `BOOLEAN` → `BOOLEAN` (same)

### Function Differences
- `NOW()` → `CURRENT_TIMESTAMP` (both work, but CURRENT_TIMESTAMP is SQL standard)
- `IFNULL()` → `COALESCE()`
- `CONCAT()` → `||` or `CONCAT()` (both work)
- `LIMIT x, y` → `LIMIT y OFFSET x`

### Features Added
- Automatic `updated_at` triggers
- `BIGSERIAL` for efficient auto-increment
- `TIMESTAMP WITH TIME ZONE` for proper timezone handling

## Testing

### Unit Tests
Run unit tests:
```bash
mvn test
```

### Integration Tests
Run integration tests with Testcontainers:
```bash
mvn verify
```

### Manual Testing
```bash
# Test database connection
curl http://localhost:8080/actuator/health

# Check connection pool metrics
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

## Production Recommendations

1. **Secrets Management**
   - Use AWS Secrets Manager, Azure Key Vault, or HashiCorp Vault
   - Never commit `.env` file to version control
   - Rotate credentials regularly

2. **Connection Pooling**
   - Monitor HikariCP metrics
   - Adjust pool size based on load: `DB_POOL_MAX_SIZE`
   - Set appropriate timeouts for cloud environments

3. **Database Migrations**
   - Test migrations in staging first
   - Use Flyway's `baseline` feature for existing databases
   - Keep migrations in version control

4. **Monitoring**
   - Enable Prometheus metrics export
   - Set up alerts for database connection failures
   - Monitor connection pool exhaustion

5. **High Availability**
   - Use PostgreSQL read replicas for read-heavy workloads
   - Configure retry logic for transient failures
   - Implement circuit breakers for cascading failure prevention

## Troubleshooting

### Connection Refused
- Verify PostgreSQL is running: `pg_isready`
- Check PostgreSQL port: `netstat -an | grep 5432`
- Verify DATABASE_URL environment variable

### Migration Failures
- Check Flyway migration history: `SELECT * FROM flyway_schema_history;`
- Reset migrations (development only): `mvn flyway:clean`
- Baseline existing database: `mvn flyway:baseline`

### Connection Pool Exhaustion
- Increase pool size: `DB_POOL_MAX_SIZE=50`
- Check for connection leaks in application code
- Monitor metrics: `/actuator/metrics/hikaricp.connections`

## References
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [HikariCP Documentation](https://github.com/brettwooldridge/HikariCP)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
