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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ApiAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private ApiAccessDeniedHandler apiAccessDeniedHandler;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        apiAccessDeniedHandler = new ApiAccessDeniedHandler(objectMapper);
        logger = (Logger) LoggerFactory.getLogger(ApiAccessDeniedHandler.class);
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
    void returns403WithJsonBody() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/accounts");
        var response = new MockHttpServletResponse();

        apiAccessDeniedHandler.handle(request, response, new AccessDeniedException("missing role"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        var body = objectMapper.readValue(response.getContentAsByteArray(), ApiErrorResponse.class);
        assertThat(body.code()).isEqualTo(ErrorCode.FILTER_ACCESS_DENIED.code());
        assertThat(body.message()).isEqualTo("Access denied.");
        assertThat(body.path()).isEqualTo("/api/accounts");
    }

    @Test
    void sanitizesHtmlInPath() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/accounts/<script>alert(1)</script>");
        var response = new MockHttpServletResponse();

        apiAccessDeniedHandler.handle(request, response, new AccessDeniedException("denied"));

        var body = objectMapper.readValue(response.getContentAsByteArray(), ApiErrorResponse.class);
        assertThat(body.path()).isEqualTo("/api/accounts/&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    void logsWarningWithUserAndPath() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/accounts/<script>");
        var response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("alice", "n/a"));

        apiAccessDeniedHandler.handle(request, response, new AccessDeniedException("missing role"));

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("POST /api/accounts/&lt;script&gt; by user=alice");
            assertThat(event.getFormattedMessage()).contains("missing role");
        });
    }
}
