package com.mymoney.api.error;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatusCode;
import org.springframework.validation.FieldError;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> errors) {

    public ApiErrorResponse(OffsetDateTime timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }

    public static ApiErrorResponse from(HttpStatusCode status, String message, String path) {
        return new ApiErrorResponse(OffsetDateTime.now(), status.value(), status.toString(), message, path);
    }

    public static ApiErrorResponse validationError(String message, String path, List<FieldError> fieldErrors) {
        List<FieldErrorDetail> details = fieldErrors.stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return new ApiErrorResponse(OffsetDateTime.now(), 400, "Bad Request", message, path, details);
    }

    public record FieldErrorDetail(String field, String message) {}
}
