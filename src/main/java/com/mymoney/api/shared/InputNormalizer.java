package com.mymoney.api.shared;

import com.mymoney.api.error.CodedResponseStatusException;
import com.mymoney.api.error.ErrorCode;
import org.springframework.http.HttpStatus;

public final class InputNormalizer {

    private InputNormalizer() {}

    public static String normalizeSearch(String value) {
        return value == null ? "" : value.trim();
    }

    public static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String requireNonBlank(String value, String fieldName) {
        var trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new CodedResponseStatusException(
                    HttpStatus.BAD_REQUEST, ErrorCode.FIELD_CANNOT_BE_EMPTY, fieldName + " cannot be empty.");
        }
        return trimmed;
    }
}
