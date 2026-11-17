package com.example.habits.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Completion flag for a habit on one local calendar day. Unique on
 * (habit_id, local_date) — the service layer upserts idempotently against that
 * constraint. {@code completed=false} rows are kept (not deleted) so that
 * explicitly un-checked days remain auditable.
 */
@Entity
@Table(name = "habit_completions",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_habit_completions_habit_date",
               columnNames = {"habit_id", "local_date"}))
public class HabitCompletion {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    /** Calendar day in the owner's timezone. */
    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

    @Column(nullable = false)
    private boolean completed;

    /** Set when completed flips to true; cleared when it flips back to false. */
    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public HabitCompletion() {
    }

    public HabitCompletion(Habit habit, LocalDate localDate, boolean completed) {
        this.habit = habit;
        this.localDate = localDate;
        this.completed = completed;
    }

    public UUID getId() {
        return id;
    }

    public Habit getHabit() {
        return habit;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
