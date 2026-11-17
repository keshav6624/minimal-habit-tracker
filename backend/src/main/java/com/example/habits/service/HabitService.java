package com.example.habits.service;

import com.example.habits.domain.Habit;
import com.example.habits.domain.User;
import com.example.habits.dto.HabitCreateRequest;
import com.example.habits.dto.HabitDto;
import com.example.habits.dto.HabitUpdateRequest;
import com.example.habits.dto.PageEnvelope;
import com.example.habits.domain.ScheduleType;
import com.example.habits.exception.ApiException;
import com.example.habits.exception.NotFoundException;
import com.example.habits.repository.HabitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Habit CRUD + archive/restore. ALL methods enforce ownership: a habit id that
 * exists but belongs to another user is indistinguishable from a missing one
 * (404, never 403) to prevent id enumeration.
 */
@Service
public class HabitService {

    private static final int MAX_PAGE_SIZE = 100;

    private final HabitRepository habitRepository;

    public HabitService(HabitRepository habitRepository) {
        this.habitRepository = habitRepository;
    }

    @Transactional
    public HabitDto create(User user, HabitCreateRequest request) {
        List<Integer> days = request.scheduleDays() == null
                ? new ArrayList<>() : request.scheduleDays();
        validateSchedule(request.scheduleType(), days);
        requireName(request.name());

        Habit habit = new Habit();
        habit.setUser(user);
        applyFields(habit, request.name(), request.description(), request.color(),
                request.icon(), request.scheduleType(), days, request.startDate());
        return toDto(habitRepository.save(habit));
    }

    @Transactional(readOnly = true)
    public PageEnvelope<HabitDto> list(User user, String status, int page, int size) {
        StatusFilter filter = StatusFilter.parse(status);
        if (page < 0) {
            throw ApiException.validation("page", "must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw ApiException.validation("size", "must be between 1 and " + MAX_PAGE_SIZE);
        }
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Habit> result = switch (filter) {
            case ACTIVE -> habitRepository.findByUserIdAndArchivedAtIsNull(user.getId(), pageable);
            case ARCHIVED -> habitRepository.findByUserIdAndArchivedAtIsNotNull(user.getId(), pageable);
        };
        List<HabitDto> content = result.getContent().stream().map(HabitService::toDto).toList();
        return new PageEnvelope<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public HabitDto get(User user, java.util.UUID habitId) {
        return toDto(findOwned(habitId, user.getId()));
    }

    @Transactional
    public HabitDto update(User user, java.util.UUID habitId, HabitUpdateRequest request) {
        Habit habit = findOwned(habitId, user.getId());

        // Compute the effective post-patch state, then validate it as a whole.
        String name = request.name() != null ? request.name() : habit.getName();
        if (request.name() != null) {
            requireName(name);
        }
        String description = request.description() != null ? request.description()
                : habit.getDescription(); // empty string clears it
        String color = request.color() != null ? request.color() : habit.getColor();
        String icon = request.icon() != null ? request.icon() : habit.getIcon();
        ScheduleType scheduleType = request.scheduleType() != null
                ? request.scheduleType() : habit.getScheduleType();
        List<Integer> scheduleDays = request.scheduleDays() != null
                ? request.scheduleDays() : habit.getScheduleDays();
        LocalDate startDate = request.startDate() != null ? request.startDate()
                : habit.getStartDate();

        validateSchedule(scheduleType, scheduleDays);

        applyFields(habit, name, description, color, icon, scheduleType, scheduleDays,
                startDate);
        return toDto(habitRepository.save(habit));
    }

    @Transactional
    public HabitDto archive(User user, java.util.UUID habitId) {
        Habit habit = findOwned(habitId, user.getId());
        if (habit.getArchivedAt() == null) {
            habit.setArchivedAt(Instant.now());
            habitRepository.save(habit);
        }
        return toDto(habit);
    }

    @Transactional
    public HabitDto restore(User user, java.util.UUID habitId) {
        Habit habit = findOwned(habitId, user.getId());
        if (habit.getArchivedAt() != null) {
            habit.setArchivedAt(null);
            habitRepository.save(habit);
        }
        return toDto(habit);
    }

    /**
     * HARD delete — permanent and irreversible (completions are removed by the
     * CASCADE FK). There is no trash bin in the MVP.
     */
    @Transactional
    public void delete(User user, java.util.UUID habitId) {
        Habit habit = findOwned(habitId, user.getId());
        habitRepository.delete(habit);
    }

    /** Owned lookup; anything else (missing or foreign) is a 404. */
    private Habit findOwned(java.util.UUID habitId, java.util.UUID userId) {
        return habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new NotFoundException("Habit not found"));
    }

    private void applyFields(Habit habit, String name, String description, String color,
                             String icon, ScheduleType scheduleType, List<Integer> days,
                             LocalDate startDate) {
        habit.setName(name.trim());
        habit.setDescription(description == null || description.isBlank()
                ? null : description);
        habit.setColor(color == null || color.isBlank() ? null : color.trim());
        habit.setIcon(icon == null || icon.isBlank() ? null : icon.trim());
        habit.setScheduleType(scheduleType);
        // Deduplicate while keeping order; nulls are rejected by validation.
        Set<Integer> distinct = new LinkedHashSet<>(days);
        habit.setScheduleDays(new ArrayList<>(distinct));
        habit.setStartDate(startDate);
    }

    private void validateSchedule(ScheduleType scheduleType, List<Integer> days) {
        List<Integer> safe = days == null ? List.of() : days;
        switch (scheduleType) {
            case DAILY, WEEKDAYS -> {
                if (!safe.isEmpty()) {
                    throw ApiException.validation("scheduleDays",
                            "must be empty for " + scheduleType + " schedule");
                }
            }
            case CUSTOM -> {
                if (safe.isEmpty()) {
                    throw ApiException.validation("scheduleDays",
                            "is required and must be non-empty for CUSTOM schedule");
                }
                if (safe.stream().anyMatch(d -> d == null || d < 1 || d > 7)) {
                    throw ApiException.validation("scheduleDays",
                            "must be a subset of 1..7 (1=Mon .. 7=Sun)");
                }
            }
        }
    }

    private void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw ApiException.validation("name", "must not be blank");
        }
    }

    public static HabitDto toDto(Habit habit) {
        return new HabitDto(habit.getId(), habit.getName(), habit.getDescription(),
                habit.getColor(), habit.getIcon(), habit.getScheduleType(),
                List.copyOf(habit.getScheduleDays()), habit.getStartDate(),
                habit.getArchivedAt(), habit.getCreatedAt(), habit.getUpdatedAt());
    }

    private enum StatusFilter {
        ACTIVE, ARCHIVED;

        static StatusFilter parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return ACTIVE; // default listing shows active habits only
            }
            return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "active" -> ACTIVE;
                case "archived" -> ARCHIVED;
                default -> throw ApiException.validation("status",
                        "must be one of active, archived");
            };
        }
    }
}
