package com.sindoflow.ops.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    private final DataSource dataSource;

    public FlywayConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migratePublicSchema() {
        log.info("Running Flyway migrations for public schema");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas("public")
                .locations("classpath:db/migration/public")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        log.info("Public schema migration complete");
    }

    public void migrateTenantSchema(String schemaName) {
        log.info("Running Flyway migrations for tenant schema: {}", schemaName);
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        log.info("Tenant schema migration complete: {}", schemaName);
    }
}
