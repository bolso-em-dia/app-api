package com.mymoney.api.error;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.mymoney.api.logging.SecurityLoggingHelper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception, HttpServletRequest request) {
        var status = exception.getStatusCode();
        var message = exception.getReason() == null ? status.toString() : exception.getReason();
        Integer code = null;
        if (exception instanceof CodedResponseStatusException codedException) {
            code = codedException.getCode();
        }

        if (status.is5xxServerError()) {
            log.error(
                    "Application failure on {} {} by user={} status={} message={}",
                    request.getMethod(),
                    SecurityLoggingHelper.sanitizePath(request),
                    SecurityLoggingHelper.currentUser(),
                    status.value(),
                    SecurityLoggingHelper.sanitize(message),
                    exception);
        } else if (status.value() >= 400) {
            log.warn(
                    "Request failed on {} {} by user={} status={} message={}",
                    request.getMethod(),
                    SecurityLoggingHelper.sanitizePath(request),
                    SecurityLoggingHelper.currentUser(),
                    status.value(),
                    SecurityLoggingHelper.sanitize(message));
        }

        return ResponseEntity.status(status)
                .body(
                        code == null
                                ? ApiErrorResponse.from(
                                        status,
                                        SecurityLoggingHelper.sanitize(message),
                                        SecurityLoggingHelper.sanitizePath(request))
                                : ApiErrorResponse.coded(
                                        status,
                                        ((CodedResponseStatusException) exception).getErrorCode(),
                                        SecurityLoggingHelper.sanitize(message),
                                        SecurityLoggingHelper.sanitizePath(request)));
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
                        SecurityLoggingHelper.sanitize(fe.getField()),
                        SecurityLoggingHelper.sanitize(fe.getDefaultMessage())))
                .toList();
        log.warn(
                "Validation failed on {} {} by user={} status={} fields=[{}]",
                request.getMethod(),
                SecurityLoggingHelper.sanitizePath(request),
                SecurityLoggingHelper.currentUser(),
                HttpStatus.BAD_REQUEST.value(),
                fieldErrors.stream()
                        .map(fe -> SecurityLoggingHelper.sanitize(fe.getField()) + ": "
                                + SecurityLoggingHelper.sanitize(fe.getDefaultMessage()))
                        .collect(java.util.stream.Collectors.joining(", ")));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.validationError(
                        SecurityLoggingHelper.sanitize(message),
                        SecurityLoggingHelper.sanitizePath(request),
                        errorDetails));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        var message = ErrorCode.INVALID_REQUEST_BODY.description();
        var errorCode = ErrorCode.INVALID_REQUEST_BODY;
        Throwable cause = exception.getCause();
        if (cause instanceof UnrecognizedPropertyException unrecognizedPropertyException) {
            errorCode = ErrorCode.UNRECOGNIZED_FIELD;
            message =
                    "Request body contains unsupported field: " + unrecognizedPropertyException.getPropertyName() + ".";
        }

        log.warn(
                "Unreadable request on {} {} by user={} status={} message={}",
                request.getMethod(),
                SecurityLoggingHelper.sanitizePath(request),
                SecurityLoggingHelper.currentUser(),
                HttpStatus.BAD_REQUEST.value(),
                SecurityLoggingHelper.sanitize(message));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.coded(
                        HttpStatus.BAD_REQUEST,
                        errorCode,
                        SecurityLoggingHelper.sanitize(message),
                        SecurityLoggingHelper.sanitizePath(request)));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthorizationDenied(
            AuthorizationDeniedException exception, HttpServletRequest request) {
        log.warn(
                "Request denied on {} {} by user={} status={} message={}",
                request.getMethod(),
                SecurityLoggingHelper.sanitizePath(request),
                SecurityLoggingHelper.currentUser(),
                HttpStatus.FORBIDDEN.value(),
                ErrorCode.METHOD_ACCESS_DENIED.description(),
                exception);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.coded(
                        HttpStatus.FORBIDDEN,
                        ErrorCode.METHOD_ACCESS_DENIED,
                        SecurityLoggingHelper.sanitizePath(request)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        var message = ErrorCode.INVALID_PARAMETER.description() + " '" + exception.getName() + "'.";
        log.warn(
                "Type mismatch on {} {} by user={} status={} message={}",
                request.getMethod(),
                SecurityLoggingHelper.sanitizePath(request),
                SecurityLoggingHelper.currentUser(),
                HttpStatus.BAD_REQUEST.value(),
                SecurityLoggingHelper.sanitize(message));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.coded(
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.INVALID_PARAMETER,
                        SecurityLoggingHelper.sanitize(message),
                        SecurityLoggingHelper.sanitizePath(request)));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException exception, HttpServletRequest request) {
        var message = ErrorCode.CONCURRENT_MODIFICATION.description();
        log.warn(
                "Optimistic lock failure on {} {} by user={} status={} message={}",
                request.getMethod(),
                SecurityLoggingHelper.sanitizePath(request),
                SecurityLoggingHelper.currentUser(),
                HttpStatus.CONFLICT.value(),
                SecurityLoggingHelper.sanitize(message),
                exception);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.coded(
                        HttpStatus.CONFLICT,
                        ErrorCode.CONCURRENT_MODIFICATION,
                        SecurityLoggingHelper.sanitize(message),
                        SecurityLoggingHelper.sanitizePath(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception, HttpServletRequest request) {
        log.warn(
                "Invalid argument on {} {} by user={} status={} message={}",
                request.getMethod(),
                SecurityLoggingHelper.sanitizePath(request),
                SecurityLoggingHelper.currentUser(),
                HttpStatus.BAD_REQUEST.value(),
                SecurityLoggingHelper.sanitize(exception.getMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.coded(
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.INVALID_PARAMETER,
                        SecurityLoggingHelper.sanitize(exception.getMessage()),
                        SecurityLoggingHelper.sanitizePath(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error(
                "Unhandled exception on {} {} by user={} status={}",
                request.getMethod(),
                SecurityLoggingHelper.sanitizePath(request),
                SecurityLoggingHelper.currentUser(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.coded(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ErrorCode.INTERNAL_ERROR,
                        SecurityLoggingHelper.sanitizePath(request)));
    }
}
