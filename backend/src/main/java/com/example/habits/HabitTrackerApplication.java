package com.example.habits;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. Default profile is {@code dev} (zero-setup H2); run against
 * PostgreSQL with {@code SPRING_PROFILES_ACTIVE=postgres} and
 * DATABASE_URL / DATABASE_USERNAME / DATABASE_PASSWORD set.
 */
@SpringBootApplication
public class HabitTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HabitTrackerApplication.class, args);
    }
}
