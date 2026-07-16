package com.mymoney.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mymoney.api.config.AppSecurityProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AppSecurityProperties properties;

    private RefreshTokenService refreshTokenService;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, properties);
        lenient().when(properties.refreshCookieName()).thenReturn("bolso_em_dia_refresh_token");

        logger = (Logger) LoggerFactory.getLogger(RefreshTokenService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void extractRefreshToken_logsWarningWhenRequestHasNoCookies() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/refresh");

        var token = refreshTokenService.extractRefreshToken(request);

        assertThat(token).isEmpty();
        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("request has no cookies");
        });
    }

    @Test
    void extractRefreshToken_logsWarningWhenExpectedCookieIsMissing() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/refresh");
        request.setCookies(new Cookie("another_cookie", "value"));

        var token = refreshTokenService.extractRefreshToken(request);

        assertThat(token).isEmpty();
        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("was not found");
        });
    }

    @Test
    void extractRefreshToken_logsWarningWhenCookieValueIsEmpty() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/refresh");
        request.setCookies(new Cookie("bolso_em_dia_refresh_token", "   "));

        var token = refreshTokenService.extractRefreshToken(request);

        assertThat(token).isEmpty();
        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("cookie value is empty");
        });
    }
}
