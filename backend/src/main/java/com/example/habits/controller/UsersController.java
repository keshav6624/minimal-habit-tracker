package com.example.habits.controller;

import com.example.habits.dto.DeleteAccountRequest;
import com.example.habits.dto.ExportJson;
import com.example.habits.dto.SettingsDto;
import com.example.habits.dto.UserDto;
import com.example.habits.service.CurrentUserService;
import com.example.habits.service.ExportService;
import com.example.habits.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Current-user profile, settings, data export and account deletion.
 */
@Tag(name = "users")
@RestController
@RequestMapping("/api/v1/users/me")
public class UsersController {

    private final UserService userService;
    private final ExportService exportService;
    private final CurrentUserService currentUserService;

    public UsersController(UserService userService, ExportService exportService,
                           CurrentUserService currentUserService) {
        this.userService = userService;
        this.exportService = exportService;
        this.currentUserService = currentUserService;
    }

    @Operation(summary = "Current user profile")
    @GetMapping
    public UserDto me() {
        return userService.getMe(currentUserService.requireUser());
    }

    @Operation(summary = "Get settings")
    @GetMapping("/settings")
    public SettingsDto getSettings() {
        return userService.getSettings(currentUserService.requireUser());
    }

    @Operation(summary = "Update settings")
    @PutMapping("/settings")
    public SettingsDto updateSettings(@Valid @RequestBody SettingsDto request) {
        // The service returns the SAVED state; the session principal's copy
        // of the user is stale after a timezone change.
        return userService.updateSettings(currentUserService.requireUser(), request);
    }

    @Operation(summary = "Export all own data (format=json|csv)")
    @GetMapping("/export")
    public ResponseEntity<?> export(@RequestParam(defaultValue = "json") String format) {
        ExportService.Format fmt = exportService.parseFormat(format);
        var user = currentUserService.requireUser();
        if (fmt == ExportService.Format.JSON) {
            ExportJson payload = exportService.exportJson(user);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"habit-tracker-export.json\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload);
        }
        String csv = exportService.renderCsv(user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"habit-tracker-export.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    @Operation(summary = "Delete the account permanently (password re-verification)")
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest request,
                                              HttpServletRequest servletRequest,
                                              HttpServletResponse servletResponse) {
        userService.deleteAccount(currentUserService.requireUser(), request.password(),
                servletRequest, servletResponse);
        return ResponseEntity.noContent().build();
    }
}
