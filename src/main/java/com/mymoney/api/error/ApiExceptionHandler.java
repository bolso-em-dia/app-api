package com.mymoney.api.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception, HttpServletRequest request) {
        HttpStatusCode status = exception.getStatusCode();
        String message = exception.getReason() == null ? status.toString() : exception.getReason();

        if (status.is5xxServerError()) {
            log.error(
                    "Application failure on {} {} by user={} status={} message={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    currentUser(),
                    status.value(),
                    message,
                    exception);
        } else if (status.value() >= 400) {
            log.warn(
                    "Request failed on {} {} by user={} status={} message={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    currentUser(),
                    status.value(),
                    message);
        }

        return ResponseEntity.status(status).body(ApiErrorResponse.from(status, message, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error(
                "Unhandled exception on {} {} by user={}",
                request.getMethod(),
                request.getRequestURI(),
                currentUser(),
                exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.from(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.", request.getRequestURI()));
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "anonymous";
        }
        return authentication.getName();
    }
}
