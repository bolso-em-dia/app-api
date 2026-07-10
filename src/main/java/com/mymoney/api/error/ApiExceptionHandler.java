package com.mymoney.api.error;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiErrorResponse> handleValidationFailure(Exception exception, HttpServletRequest request) {
        List<FieldError> fieldErrors = List.of();
        if (exception instanceof MethodArgumentNotValidException manve) {
            fieldErrors = manve.getBindingResult().getFieldErrors();
        } else if (exception instanceof BindException be) {
            fieldErrors = be.getBindingResult().getFieldErrors();
        }

        String message = "Request validation failed.";
        log.warn(
                "Validation failed on {} {} by user={} status={} fields=[{}]",
                request.getMethod(),
                request.getRequestURI(),
                currentUser(),
                fieldErrors.stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .collect(java.util.stream.Collectors.joining(", ")));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.validationError(message, request.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        String message = "Request body is invalid.";
        Throwable cause = exception.getCause();
        if (cause instanceof UnrecognizedPropertyException unrecognizedPropertyException) {
            message =
                    "Request body contains unsupported field: " + unrecognizedPropertyException.getPropertyName() + ".";
        }

        log.warn(
                "Unreadable request on {} {} by user={} status={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                currentUser(),
                HttpStatus.BAD_REQUEST.value(),
                message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.from(HttpStatus.BAD_REQUEST, message, request.getRequestURI()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthorizationDenied(
            AuthorizationDeniedException exception, HttpServletRequest request) {
        log.warn(
                "Request denied on {} {} by user={} status={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                currentUser(),
                HttpStatus.FORBIDDEN.value(),
                "Access Denied");

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.from(HttpStatus.FORBIDDEN, "Access Denied", request.getRequestURI()));
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
