package com.mymoney.api.error;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatusCode;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        Integer code,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> errors) {

    public ApiErrorResponse(OffsetDateTime timestamp, int status, String error, String message, String path) {
        this(timestamp, status, null, error, message, path, null);
    }

    public ApiErrorResponse(
            OffsetDateTime timestamp, int status, Integer code, String error, String message, String path) {
        this(timestamp, status, code, error, message, path, null);
    }

    public static ApiErrorResponse from(HttpStatusCode status, String message, String path) {
        return new ApiErrorResponse(OffsetDateTime.now(), status.value(), null, status.toString(), message, path);
    }

    public static ApiErrorResponse coded(HttpStatusCode status, ErrorCode errorCode, String path) {
        return coded(status, errorCode, errorCode.description(), path);
    }

    public static ApiErrorResponse coded(HttpStatusCode status, ErrorCode errorCode, String message, String path) {
        return new ApiErrorResponse(
                OffsetDateTime.now(), status.value(), errorCode.code(), status.toString(), message, path, null);
    }

    public static ApiErrorResponse validationError(String message, String path, List<FieldErrorDetail> fieldErrors) {
        return new ApiErrorResponse(
                OffsetDateTime.now(),
                400,
                ErrorCode.VALIDATION_FAILED.code(),
                "Bad Request",
                message,
                path,
                fieldErrors);
    }

    public record FieldErrorDetail(String field, String message) {}
}
