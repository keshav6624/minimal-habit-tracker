package com.example.habits.repository;

import com.example.habits.domain.HabitCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HabitCompletionRepository extends JpaRepository<HabitCompletion, UUID> {

    Optional<HabitCompletion> findByHabitIdAndLocalDate(UUID habitId, LocalDate localDate);

    boolean existsByHabitIdAndLocalDate(UUID habitId, LocalDate localDate);

    /**
     * Removes the record entirely (documented DELETE contract choice).
     * Explicit @Modifying delete: a derived delete method cannot declare a
     * count return type, and this keeps it a single round-trip.
     */
    @Modifying
    @Query("""
            delete from HabitCompletion c
            where c.habit.id = :habitId and c.localDate = :localDate
            """)
    int deleteByHabitIdAndLocalDate(@Param("habitId") UUID habitId,
                                    @Param("localDate") LocalDate localDate);

    List<HabitCompletion> findByHabitIdAndLocalDateBetweenOrderByLocalDateAsc(
            UUID habitId, LocalDate from, LocalDate to);

    /** All completions of one user across their habits (export + analytics). */
    @Query("""
            select c from HabitCompletion c
            join c.habit h
            where h.user.id = :userId
            order by c.localDate asc
            """)
    List<HabitCompletion> findAllByUserId(@Param("userId") UUID userId);

    /** Range-filtered variant used by analytics to bound the scan. */
    @Query("""
            select c from HabitCompletion c
            join c.habit h
            where h.user.id = :userId
              and c.localDate between :from and :to
            order by c.localDate asc
            """)
    List<HabitCompletion> findAllByUserIdAndDateRange(@Param("userId") UUID userId,
                                                      @Param("from") LocalDate from,
                                                      @Param("to") LocalDate to);
}
