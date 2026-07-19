package com.mymoney.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymoney.api.error.ApiErrorResponse;
import com.mymoney.api.error.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

class ApiAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        apiAuthenticationEntryPoint = new ApiAuthenticationEntryPoint(objectMapper);
        logger = (Logger) LoggerFactory.getLogger(ApiAuthenticationEntryPoint.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        SecurityContextHolder.clearContext();
    }

    @Test
    void returns401WithJsonBody() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/auth/refresh");
        var response = new MockHttpServletResponse();

        apiAuthenticationEntryPoint.commence(
                request, response, new InsufficientAuthenticationException("missing token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        var body = objectMapper.readValue(response.getContentAsByteArray(), ApiErrorResponse.class);
        assertThat(body.code()).isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED.code());
        assertThat(body.message()).isEqualTo("Authentication is required.");
        assertThat(body.path()).isEqualTo("/api/auth/refresh");
    }

    @Test
    void sanitizesHtmlInPath() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/auth/<script>alert(1)</script>");
        var response = new MockHttpServletResponse();

        apiAuthenticationEntryPoint.commence(
                request, response, new InsufficientAuthenticationException("missing token"));

        var body = objectMapper.readValue(response.getContentAsByteArray(), ApiErrorResponse.class);
        assertThat(body.path()).isEqualTo("/api/auth/&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    void logsWarningWithContext() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/auth/<script>");
        var response = new MockHttpServletResponse();

        apiAuthenticationEntryPoint.commence(
                request, response, new InsufficientAuthenticationException("missing token"));

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("GET /api/auth/&lt;script&gt; by user=anonymous");
            assertThat(event.getFormattedMessage()).contains("missing token");
        });
    }
}
