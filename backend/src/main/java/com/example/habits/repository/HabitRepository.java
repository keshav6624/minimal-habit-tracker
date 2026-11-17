package com.example.habits.repository;

import com.example.habits.domain.Habit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HabitRepository extends JpaRepository<Habit, UUID> {

    /** All habits of a user, active and archived (used by export/analytics). */
    List<Habit> findByUserId(UUID userId);

    /**
     * Ownership-aware lookup: returns the habit only when owned by the given
     * user, so a foreign id is indistinguishable from a missing one (404).
     */
    Optional<Habit> findByIdAndUserId(UUID id, UUID userId);

    Page<Habit> findByUserIdAndArchivedAtIsNull(UUID userId, Pageable pageable);

    Page<Habit> findByUserIdAndArchivedAtIsNotNull(UUID userId, Pageable pageable);
}
