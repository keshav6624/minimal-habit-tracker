package com.example.habits.controller;

import com.example.habits.dto.HabitCreateRequest;
import com.example.habits.dto.HabitDto;
import com.example.habits.dto.HabitUpdateRequest;
import com.example.habits.dto.PageEnvelope;
import com.example.habits.service.CurrentUserService;
import com.example.habits.service.HabitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Habit CRUD. Controllers stay thin; ownership checks and validation live in
 * the service layer.
 */
@Tag(name = "habits")
@RestController
@RequestMapping("/api/v1/habits")
public class HabitsController {

    private final HabitService habitService;
    private final CurrentUserService currentUserService;

    public HabitsController(HabitService habitService, CurrentUserService currentUserService) {
        this.habitService = habitService;
        this.currentUserService = currentUserService;
    }

    @Operation(summary = "List habits (status=active|archived, paged)")
    @GetMapping
    public PageEnvelope<HabitDto> list(@RequestParam(required = false) String status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return habitService.list(currentUserService.requireUser(), status, page, size);
    }

    @Operation(summary = "Create a habit")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HabitDto create(@Valid @RequestBody HabitCreateRequest request) {
        return habitService.create(currentUserService.requireUser(), request);
    }

    @Operation(summary = "Get one habit")
    @GetMapping("/{habitId}")
    public HabitDto get(@PathVariable UUID habitId) {
        return habitService.get(currentUserService.requireUser(), habitId);
    }

    @Operation(summary = "Partial update (null fields unchanged, empty clears description)")
    @PatchMapping("/{habitId}")
    public HabitDto update(@PathVariable UUID habitId,
                           @Valid @RequestBody HabitUpdateRequest request) {
        return habitService.update(currentUserService.requireUser(), habitId, request);
    }

    @Operation(summary = "Archive (soft delete)")
    @PostMapping("/{habitId}/archive")
    public HabitDto archive(@PathVariable UUID habitId) {
        return habitService.archive(currentUserService.requireUser(), habitId);
    }

    @Operation(summary = "Restore an archived habit")
    @PostMapping("/{habitId}/restore")
    public HabitDto restore(@PathVariable UUID habitId) {
        return habitService.restore(currentUserService.requireUser(), habitId);
    }

    @Operation(summary = "Hard delete (permanent; completions cascade)")
    @DeleteMapping("/{habitId}")
    public ResponseEntity<Void> delete(@PathVariable UUID habitId) {
        habitService.delete(currentUserService.requireUser(), habitId);
        return ResponseEntity.noContent().build();
    }
}
