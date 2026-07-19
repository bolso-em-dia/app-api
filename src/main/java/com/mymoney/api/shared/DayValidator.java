package com.mymoney.api.shared;

import com.mymoney.api.error.CodedResponseStatusException;
import com.mymoney.api.error.ErrorCode;
import org.springframework.http.HttpStatus;

public final class DayValidator {

    private DayValidator() {}

    public static void validateDayRange(Integer day, String fieldName) {
        if (day == null || day < 1 || day > 31) {
            throw new CodedResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ErrorCode.DAY_OUT_OF_RANGE,
                    fieldName + " must be between 1 and 31.");
        }
    }

    public static void requireDueDay(Short dueDay) {
        if (dueDay == null) {
            throw new CodedResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.DUE_DAY_REQUIRED);
        }
    }
}
