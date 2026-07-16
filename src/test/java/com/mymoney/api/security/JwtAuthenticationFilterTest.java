package com.mymoney.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mymoney.api.auth.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
        logger = (Logger) LoggerFactory.getLogger(JwtAuthenticationFilter.class);
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
    void doFilterInternal_logsWarningWhenBearerTokenIsInvalid() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/auth/refresh");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");

        when(jwtService.decode("invalid-token")).thenThrow(new JwtException("Bad JWT"));

        jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("method=GET");
            assertThat(event.getFormattedMessage()).contains("path=/api/auth/refresh");
            assertThat(event.getFormattedMessage()).contains("Bad JWT");
        });
    }
}
