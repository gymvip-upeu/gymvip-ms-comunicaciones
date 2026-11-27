package com.gymvip.comms.infrastructure;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(name = "gymvip.operations-metrics-enabled", havingValue = "true")
public class OperationsMetrics {
    @Bean
    MeterBinder communicationsOperations(JdbcTemplate jdbc) {
        return registry -> {
            Gauge.builder("gymvip.communications.retries.recorded", jdbc, db -> number(db, """
                    SELECT COALESCE(SUM(CASE WHEN attempt_count > 1 THEN attempt_count - 1 ELSE 0 END), 0)
                    FROM communication_attempts
                    """)).description("Delivery retries recorded across retained attempts").register(registry);
            Gauge.builder("gymvip.database.reachable", jdbc, db -> number(db, "SELECT 1"))
                    .description("PostgreSQL responds to a query").register(registry);
        };
    }

    private static double number(JdbcTemplate jdbc, String sql) {
        try {
            Number value = jdbc.queryForObject(sql, Number.class);
            return value == null ? Double.NaN : value.doubleValue();
        } catch (DataAccessException exception) {
            return Double.NaN;
        }
    }
}
