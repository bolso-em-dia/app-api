package com.mymoney.api.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {

    private RequestLoggingFilter requestLoggingFilter;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        requestLoggingFilter = new RequestLoggingFilter();
        logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        MDC.clear();
    }

    @Test
    void generatesRequestIdWhenHeaderMissing() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/health");
        var response = new MockHttpServletResponse();
        var requestIdSeenByChain = new AtomicReference<String>();

        requestLoggingFilter.doFilter(request, response, (req, res) -> requestIdSeenByChain.set(MDC.get("requestId")));

        assertThat(response.getHeader("X-Request-ID")).isNotBlank();
        assertThat(requestIdSeenByChain.get()).isEqualTo(response.getHeader("X-Request-ID"));
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void reusesRequestIdWhenHeaderPresent() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/accounts");
        request.addHeader("X-Request-ID", "req-123");
        var response = new MockHttpServletResponse();

        requestLoggingFilter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader("X-Request-ID")).isEqualTo("req-123");
    }

    @Test
    void replacesInvalidRequestIdHeader() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/accounts");
        request.addHeader("X-Request-ID", "bad<script>");
        var response = new MockHttpServletResponse();

        requestLoggingFilter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader("X-Request-ID")).isNotBlank();
        assertThat(response.getHeader("X-Request-ID")).isNotEqualTo("bad<script>");
        assertThat(response.getHeader("X-Request-ID")).matches("[A-Za-z0-9._-]{1,64}");
    }

    @Test
    void logsRequestSummaryOnCompletion() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/health");
        var response = new MockHttpServletResponse();

        requestLoggingFilter.doFilter(request, response, (req, res) -> {});

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage()).contains("GET /api/health status=200 elapsed=");
        });
    }
}
