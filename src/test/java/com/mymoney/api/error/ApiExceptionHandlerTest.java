package com.mymoney.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.server.ResponseStatusException;

class ApiExceptionHandlerTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void optimisticLockFailureReturnsConflict() {
        var handler = new ApiExceptionHandler();
        var request = new MockHttpServletRequest("PUT", "/api/accounts/id");

        var response =
                handler.handleOptimisticLock(new ObjectOptimisticLockingFailureException("Account", "id"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION.code());
        assertThat(response.getBody().message()).isEqualTo("Resource was modified by another user.");
        assertThat(response.getBody().path()).isEqualTo("/api/accounts/id");
    }

    @Test
    void codedResponseStatusIncludesCodeInBody() {
        var handler = new ApiExceptionHandler();
        var request = new MockHttpServletRequest("POST", "/api/accounts");

        var response = handler.handleResponseStatus(
                new CodedResponseStatusException(HttpStatus.CONFLICT, ErrorCode.EMAIL_ALREADY_IN_USE), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.EMAIL_ALREADY_IN_USE.code());
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.EMAIL_ALREADY_IN_USE.description());
    }

    @Test
    void plainResponseStatusOmitsCodeInBody() {
        var handler = new ApiExceptionHandler();
        var request = new MockHttpServletRequest("GET", "/api/fail");

        var response =
                handler.handleResponseStatus(new ResponseStatusException(HttpStatus.BAD_REQUEST, "plain"), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isNull();
    }

    @Test
    void unexpectedExceptionReturnsInternalServerErrorAndLogsStatus500() {
        var handler = new ApiExceptionHandler();
        var request = new MockHttpServletRequest("GET", "/api/fail");

        var response = handler.handleUnexpected(new IllegalStateException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.INTERNAL_ERROR.code());
        assertThat(response.getBody().message()).isEqualTo("Unexpected server error.");
        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage()).contains("status=500");
            assertThat(event.getFormattedMessage()).contains("/api/fail");
        });
    }

    @Test
    void authorizationDeniedReturnsForbiddenWithStandardMessage() {
        var handler = new ApiExceptionHandler();
        var request = new MockHttpServletRequest("GET", "/api/accounts");

        var response = handler.handleAuthorizationDenied(new AuthorizationDeniedException("Access denied."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.METHOD_ACCESS_DENIED.code());
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.METHOD_ACCESS_DENIED.description());
        assertThat(response.getBody().path()).isEqualTo("/api/accounts");
    }
}
