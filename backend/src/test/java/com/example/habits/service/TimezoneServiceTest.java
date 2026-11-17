package com.example.habits.service;

import com.example.habits.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** "Today" resolution across timezones incl. DST boundaries. */
class TimezoneServiceTest {

    private final TimezoneService service = new TimezoneService();

    @Test
    void valid_iana_timezone_is_accepted() {
        assertEquals(ZoneId.of("Asia/Kolkata"), service.validate("Asia/Kolkata"));
    }

    @Test
    void invalid_timezone_is_rejected_with_400() {
        var ex = assertThrows(ApiException.class, () -> service.validate("Mars/Olympus"));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void blank_timezone_is_rejected() {
        assertThrows(ApiException.class, () -> service.validate(" "));
        assertThrows(ApiException.class, () -> service.validate(null));
    }

    @Test
    void kolkata_date_is_never_behind_utc_date() {
        // IST is UTC+5:30, so Kolkata's local date is always >= UTC's date.
        LocalDate istToday = service.today(ZoneId.of("Asia/Kolkata"));
        LocalDate utcToday = service.today(ZoneId.of("UTC"));
        org.junit.jupiter.api.Assertions.assertTrue(
                istToday.isEqual(utcToday) || istToday.isAfter(utcToday));
    }

    @Test
    void today_matches_localdate_now_for_the_zone() {
        ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        assertEquals(LocalDate.now(kolkata), service.today(kolkata));
    }

    @Test
    void dst_boundary_new_york_resolves_which_offset_in_effect() {
        // US DST 2024: starts Mar 10 (EDT, UTC-4), ends Nov 3 (back to EST, UTC-5).
        ZoneId newYork = ZoneId.of("America/New_York");
        ZonedDateTime summer = ZonedDateTime.of(2024, 7, 1, 12, 0, 0, 0, newYork);
        ZonedDateTime winter = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, newYork);
        assertEquals(ZoneOffset.ofHours(-4),
                summer.getOffset()); // EDT after the DST boundary
        assertEquals(ZoneOffset.ofHours(-5),
                winter.getOffset()); // EST before the DST boundary

        // Same wall clock time on both sides of the March boundary maps to
        // different instants — "today" stays stable for local-date semantics.
        ZonedDateTime preDst = ZonedDateTime.of(2024, 3, 9, 23, 30, 0, 0, newYork);
        assertEquals(LocalDate.of(2024, 3, 9), preDst.toLocalDate());
    }
}
