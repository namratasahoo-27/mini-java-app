package com.test.config;

import com.test.DatabaseService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for database connectivity
 * Used by Kubernetes liveness and readiness probes
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DatabaseService databaseService;

    public DatabaseHealthIndicator(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public Health health() {
        try {
            if (databaseService.isConnectionValid()) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Connection validated successfully")
                    .build();
            } else {
                return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Connection validation failed")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
