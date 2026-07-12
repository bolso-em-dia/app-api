package com.mymoney.api.shared;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class DayValidator {

    private DayValidator() {}

    public static void validateDayRange(Integer day, String fieldName) {
        if (day == null || day < 1 || day > 31) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, fieldName + " must be between 1 and 31.");
        }
    }

    public static void requireDueDay(Short dueDay) {
        if (dueDay == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due day is required.");
        }
    }
}
