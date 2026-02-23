package com.owlsburg.ops.config;

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
    public void migrate() {
        log.info("Running Flyway migrations");
        Flyway.configure()
                .dataSource(dataSource)
                .schemas("public")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();
        log.info("Flyway migrations complete");
    }
}
