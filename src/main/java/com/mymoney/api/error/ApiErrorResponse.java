package com.mymoney.api.error;

import java.time.OffsetDateTime;
import org.springframework.http.HttpStatusCode;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path) {

    public static ApiErrorResponse from(HttpStatusCode status, String message, String path) {
        return new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.toString(),
                message,
                path);
    }
}
