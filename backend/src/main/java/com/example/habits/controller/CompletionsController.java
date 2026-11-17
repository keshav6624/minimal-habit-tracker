package com.example.habits.controller;

import com.example.habits.dto.CompletionDto;
import com.example.habits.dto.CompletionPutRequest;
import com.example.habits.dto.CompletionsRangeResponse;
import com.example.habits.service.CompletionService;
import com.example.habits.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Per-day completion flags under /habits/{id}/completions.
 */
@Tag(name = "completions")
@RestController
@RequestMapping("/api/v1/habits/{habitId}/completions")
public class CompletionsController {

    private final CompletionService completionService;
    private final CurrentUserService currentUserService;

    public CompletionsController(CompletionService completionService,
                                 CurrentUserService currentUserService) {
        this.completionService = completionService;
        this.currentUserService = currentUserService;
    }

    @Operation(summary = "Idempotent upsert for one local date")
    @PutMapping("/{localDate}")
    public CompletionDto upsert(@PathVariable UUID habitId,
                                @PathVariable LocalDate localDate,
                                @Valid @RequestBody(required = false)
                                CompletionPutRequest body) {
        Boolean completed = body == null ? null : body.completed();
        return completionService.upsert(currentUserService.requireUser(),
                habitId, localDate, completed);
    }

    @Operation(summary = "Delete the record for one local date")
    @DeleteMapping("/{localDate}")
    public ResponseEntity<Void> delete(@PathVariable UUID habitId,
                                       @PathVariable LocalDate localDate) {
        completionService.delete(currentUserService.requireUser(), habitId, localDate);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List completions in a range (max 370 days)")
    @GetMapping
    public CompletionsRangeResponse range(@PathVariable UUID habitId,
                                          @RequestParam LocalDate from,
                                          @RequestParam LocalDate to) {
        return completionService.range(currentUserService.requireUser(), habitId, from, to);
    }
}
