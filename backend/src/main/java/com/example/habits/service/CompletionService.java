package com.example.habits.service;

import com.example.habits.domain.Habit;
import com.example.habits.domain.HabitCompletion;
import com.example.habits.domain.User;
import com.example.habits.dto.CompletionDto;
import com.example.habits.dto.CompletionsRangeResponse;
import com.example.habits.exception.ApiException;
import com.example.habits.exception.ConflictException;
import com.example.habits.exception.ErrorType;
import com.example.habits.exception.NotFoundException;
import com.example.habits.repository.HabitCompletionRepository;
import com.example.habits.repository.HabitRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Idempotent per-day completion upserts and range reads.
 *
 * <p>Rules enforced here: the date must be within [habit.startDate, today in
 * the owner's timezone]; archived habits reject new completions (409 — restore
 * first); the (habit_id, local_date) UNIQUE constraint is the concurrency
 * backstop: a losing insert catches DataIntegrityViolationException and
 * re-selects, so concurrent duplicate PUTs converge to exactly one row and both
 * requests succeed.</p>
 */
@Service
public class CompletionService {

    private static final int MAX_RANGE_DAYS = 370;

    private final HabitRepository habitRepository;
    private final HabitCompletionRepository completionRepository;
    private final TimezoneService timezoneService;
    private final NewTxInsertRunner insertRunner;

    public CompletionService(HabitRepository habitRepository,
                             HabitCompletionRepository completionRepository,
                             TimezoneService timezoneService,
                             NewTxInsertRunner insertRunner) {
        this.habitRepository = habitRepository;
        this.completionRepository = completionRepository;
        this.timezoneService = timezoneService;
        this.insertRunner = insertRunner;
    }

    @Transactional
    public CompletionDto upsert(User user, UUID habitId, LocalDate localDate,
                                Boolean completedFlag) {
        boolean completed = completedFlag == null || completedFlag;
        Habit habit = findOwned(habitId, user.getId());
        validateDate(habit, localDate, user);

        Optional<HabitCompletion> existing =
                completionRepository.findByHabitIdAndLocalDate(habitId, localDate);
        HabitCompletion row;
        if (existing.isPresent()) {
            row = existing.get();
            row.setCompleted(completed);
            row.setCompletedAt(completed ? Instant.now() : null);
        } else {
            row = new HabitCompletion(habit, localDate, completed);
            if (completed) {
                row.setCompletedAt(Instant.now());
            }
            try {
                // Insert in its own transaction: on databases like PostgreSQL the
                // transaction is aborted after a constraint violation, so the
                // failing insert must not poison THIS transaction.
                insertRunner.insert(row);
            } catch (DataIntegrityViolationException e) {
                // Concurrent duplicate insert lost the race: re-select the winner
                // (whose flag already matches for identical idempotent PUTs).
                row = completionRepository.findByHabitIdAndLocalDate(habitId, localDate)
                        .orElseThrow(() -> new IllegalStateException(
                                "Unique row vanished after constraint violation", e));
            }
        }
        return new CompletionDto(row.getLocalDate(), row.isCompleted(), row.getCompletedAt());
    }

    /** Removes the record entirely (documented contract choice); 404 when absent. */
    @Transactional
    public void delete(User user, UUID habitId, LocalDate localDate) {
        Habit habit = findOwned(habitId, user.getId());
        int removed = completionRepository.deleteByHabitIdAndLocalDate(habit.getId(), localDate);
        if (removed == 0) {
            throw new NotFoundException("Completion not found for this date");
        }
    }

    @Transactional(readOnly = true)
    public CompletionsRangeResponse range(User user, UUID habitId, LocalDate from,
                                          LocalDate to) {
        findOwned(habitId, user.getId());
        if (from == null || to == null) {
            throw ApiException.validation("from", "from and to are both required (YYYY-MM-DD)");
        }
        if (from.isAfter(to)) {
            throw ApiException.validation("from", "from must be on or before to");
        }
        if (to.toEpochDay() - from.toEpochDay() + 1 > MAX_RANGE_DAYS) {
            throw ApiException.validation("from",
                    "range must be at most " + MAX_RANGE_DAYS + " days");
        }
        List<CompletionDto> completions = completionRepository
                .findByHabitIdAndLocalDateBetweenOrderByLocalDateAsc(habitId, from, to)
                .stream()
                .map(c -> new CompletionDto(c.getLocalDate(), c.isCompleted(),
                        c.getCompletedAt()))
                .toList();
        return new CompletionsRangeResponse(from, to, completions);
    }

    /**
     * Separate bean so {@code REQUIRES_NEW} applies through the proxy (a nested
     * self-invocation would be ignored).
     */
    @Component
    static class NewTxInsertRunner {

        private final HabitCompletionRepository completionRepository;

        NewTxInsertRunner(HabitCompletionRepository completionRepository) {
            this.completionRepository = completionRepository;
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void insert(HabitCompletion row) {
            completionRepository.saveAndFlush(row);
        }
    }

    private Habit findOwned(UUID habitId, UUID userId) {
        return habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new NotFoundException("Habit not found"));
    }

    private void validateDate(Habit habit, LocalDate localDate, User user) {
        ZoneId zone = timezoneService.validate(user.getTimezone());
        LocalDate today = timezoneService.today(zone);
        if (localDate.isAfter(today)) {
            throw ApiException.validation("localDate",
                    "must not be in the future (today is " + today + ")");
        }
        if (localDate.isBefore(habit.getStartDate())) {
            throw ApiException.validation("localDate",
                    "must not be before the habit start date (" + habit.getStartDate() + ")");
        }
        if (habit.getArchivedAt() != null) {
            throw new ConflictException(ErrorType.CONFLICT,
                    "Habit is archived; restore it before recording completions");
        }
    }
}
