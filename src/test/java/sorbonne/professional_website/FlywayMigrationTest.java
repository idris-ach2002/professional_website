package sorbonne.professional_website;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest {

    @Test
    void allCurrentMigrationsRunOnAnEmptyPostgresqlDatabase() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("professional_website_flyway_test")
                .withUsername("test")
                .withPassword("test")) {
            postgres.start();

            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(false)
                    .load();

            flyway.migrate();

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword())) {
                assertThat(queryInt(connection,
                        "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE"))
                        .isEqualTo(11);
                assertThat(queryBoolean(connection,
                        "SELECT to_regclass('public.app_owner') IS NOT NULL"))
                        .isTrue();
                assertThat(queryBoolean(connection,
                        "SELECT to_regclass('public.website_version') IS NOT NULL"))
                        .isTrue();
                assertThat(queryBoolean(connection,
                        "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'website_version' AND column_name = 'row_version')"))
                        .isTrue();
                assertThat(queryBoolean(connection,
                        "SELECT to_regclass('public.analytics_event') IS NOT NULL"))
                        .isTrue();
                assertThat(queryBoolean(connection,
                        "SELECT to_regclass('public.content_translation') IS NOT NULL"))
                        .isTrue();
                assertThat(queryBoolean(connection,
                        "SELECT to_regclass('public.background_job') IS NOT NULL"))
                        .isTrue();
                assertThat(queryBoolean(connection,
                        "SELECT to_regclass('public.outbox_event') IS NOT NULL"))
                        .isTrue();
                assertThat(queryBoolean(connection,
                        "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'website_version' AND column_name = 'publication_status')"))
                        .isTrue();
                assertThat(queryBoolean(connection,
                        "SELECT to_regclass('public.publication_audit') IS NOT NULL"))
                        .isTrue();
                assertThat(queryBoolean(connection,
                        "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'outbox_event' AND column_name = 'next_attempt_at')"))
                        .isTrue();
                assertThat(queryBoolean(connection,
                        "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'background_job' AND column_name = 'heartbeat_at')"))
                        .isTrue();
                assertThat(queryBoolean(connection,
                        "SELECT to_regclass('public.runtime_performance_sample') IS NOT NULL"))
                        .isTrue();
                assertThat(queryBoolean(connection,
                        "SELECT to_regclass('public.front_item_visibility') IS NOT NULL"))
                        .isTrue();
            }
        }
    }

    private static int queryInt(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static boolean queryBoolean(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getBoolean(1);
        }
    }
}
