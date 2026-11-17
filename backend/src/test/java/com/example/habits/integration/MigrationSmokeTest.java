package com.example.habits.integration;

import com.example.habits.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flyway migrations ran cleanly on H2 (PostgreSQL mode): all four versions
 * applied successfully and every table is queryable.
 */
class MigrationSmokeTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void all_four_migrations_applied() {
        // Row 1 is Flyway's own "<< Flyway Schema History table created >>"
        // marker (version=null); the four real migrations carry versions 1..4.
        List<String> versions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = true "
                        + "and version is not null", String.class);
        assertThat(versions).containsExactlyInAnyOrder("1", "2", "3", "4");
        Integer failed = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = false",
                Integer.class);
        assertThat(failed).isZero();
    }

    @Test
    void all_tables_exist() {
        assertThat(jdbcTemplate.queryForObject("select count(*) from users", Integer.class))
                .isGreaterThanOrEqualTo(0);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from user_settings", Integer.class))
                .isGreaterThanOrEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("select count(*) from habits", Integer.class))
                .isGreaterThanOrEqualTo(0);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from habit_completions", Integer.class))
                .isGreaterThanOrEqualTo(0);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from password_reset_tokens", Integer.class))
                .isGreaterThanOrEqualTo(0);
    }

    @Test
    void named_constraints_present() {
        // H2 runs with DATABASE_TO_LOWER=TRUE, so identifiers compare lowercase.
        Integer constraint = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.table_constraints
                where constraint_name in
                  ('pk_users','uq_users_email','fk_habits_user','ck_habits_schedule_type',
                   'uq_habit_completions_habit_date')
                """, Integer.class);
        assertThat(constraint).isEqualTo(5);
    }
}
