package com.taskmanager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    @Bean
    public CommandLineRunner runDatabaseMigrations(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                log.info("Running manual database migrations...");
                // Force file_url to be TEXT so it can hold giant S3 pre-signed URLs
                jdbcTemplate.execute("ALTER TABLE tasks ALTER COLUMN file_url TYPE TEXT;");
                log.info("Successfully altered file_url to TEXT.");
            } catch (Exception e) {
                log.warn("Migration skipped or failed (column might already be TEXT): {}", e.getMessage());
            }
        };
    }
}
