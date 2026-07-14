package com.mymoney.api.error;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

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

        return ResponseEntity.status(status)
                .body(ApiErrorResponse.from(status, sanitize(message), sanitizePath(request)));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiErrorResponse> handleValidationFailure(Exception exception, HttpServletRequest request) {
        var fieldErrors = List.<FieldError>of();
        if (exception instanceof MethodArgumentNotValidException manve) {
            fieldErrors = manve.getBindingResult().getFieldErrors();
        } else if (exception instanceof BindException be) {
            fieldErrors = be.getBindingResult().getFieldErrors();
        }

        String message = "Request validation failed.";
        var errorDetails = fieldErrors.stream()
                .map(fe -> new ApiErrorResponse.FieldErrorDetail(
                        sanitize(fe.getField()), sanitize(fe.getDefaultMessage())))
                .toList();
        log.warn(
                "Validation failed on {} {} by user={} status={} fields=[{}]",
                request.getMethod(),
                request.getRequestURI(),
                currentUser(),
                HttpStatus.BAD_REQUEST.value(),
                fieldErrors.stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .collect(java.util.stream.Collectors.joining(", ")));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.validationError(sanitize(message), sanitizePath(request), errorDetails));
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
                .body(ApiErrorResponse.from(HttpStatus.BAD_REQUEST, sanitize(message), sanitizePath(request)));
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
                "Access Denied",
                exception);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.from(HttpStatus.FORBIDDEN, "Access Denied", sanitizePath(request)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        String message = "Invalid value for parameter '" + exception.getName() + "'.";
        log.warn(
                "Type mismatch on {} {} by user={} status={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                currentUser(),
                HttpStatus.BAD_REQUEST.value(),
                message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.from(HttpStatus.BAD_REQUEST, sanitize(message), sanitizePath(request)));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException exception, HttpServletRequest request) {
        String message = "Resource was modified by another user.";
        log.warn(
                "Optimistic lock failure on {} {} by user={} status={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                currentUser(),
                HttpStatus.CONFLICT.value(),
                message,
                exception);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.from(HttpStatus.CONFLICT, sanitize(message), sanitizePath(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception, HttpServletRequest request) {
        log.warn(
                "Invalid argument on {} {} by user={} status={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                currentUser(),
                HttpStatus.BAD_REQUEST.value(),
                exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.from(
                        HttpStatus.BAD_REQUEST, sanitize(exception.getMessage()), sanitizePath(request)));
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
                        HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.", sanitizePath(request)));
    }

    private String sanitizePath(HttpServletRequest request) {
        return sanitize(request.getRequestURI());
    }

    private String sanitize(String value) {
        return value == null ? null : HtmlUtils.htmlEscape(value);
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "anonymous";
        }
        return authentication.getName();
    }
}
