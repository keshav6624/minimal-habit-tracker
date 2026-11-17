package com.example.habits.service;

import com.example.habits.exception.ApiException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Central place for "today in the user's timezone" logic so completions,
 * analytics, and validation all agree on the same definition of the current
 * local date.
 */
@Service
public class TimezoneService {

    /** Validates an IANA timezone id, e.g. "Asia/Kolkata". Throws 400 otherwise. */
    public ZoneId validate(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw ApiException.validation("timezone", "must not be blank");
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            throw ApiException.validation("timezone", "must be a valid IANA timezone");
        }
    }

    /** Today's date in the given zone (UTC if zone null — defensive only). */
    public LocalDate today(ZoneId zone) {
        return LocalDate.now(zone != null ? zone : ZoneOffset.UTC);
    }

    /** Convenience overload taking a raw (already validated) timezone string. */
    public LocalDate today(String timezone) {
        return today(validate(timezone));
    }
}
