package com.mymoney.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.member.FamilyRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class ForcedPasswordChangeFilterTest {

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private ApiAccessDeniedHandler accessDeniedHandler;

    private ForcedPasswordChangeFilter forcedPasswordChangeFilter;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        forcedPasswordChangeFilter = new ForcedPasswordChangeFilter(familyMemberRepository, accessDeniedHandler);
        logger = (Logger) LoggerFactory.getLogger(ForcedPasswordChangeFilter.class);
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
    void doFilterInternal_logsWarningWhenRequestIsBlocked() throws Exception {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("admin@bolso-em-dia.local")
                .claim("mid", UUID.randomUUID().toString())
                .claim("mustChangePwd", true)
                .build();
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin@bolso-em-dia.local", jwt, java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var member = FamilyMember.builder()
                .id(UUID.randomUUID())
                .name("Admin")
                .email("admin@bolso-em-dia.local")
                .passwordHash("hash")
                .role(FamilyRole.ADMIN)
                .active(true)
                .mustChangePassword(true)
                .build();
        when(familyMemberRepository.findByEmailIgnoreCase("admin@bolso-em-dia.local"))
                .thenReturn(Optional.of(member));

        var request = new MockHttpServletRequest("GET", "/api/me/preferences");
        forcedPasswordChangeFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(accessDeniedHandler).handle(any(), any(), any());
        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("admin@bolso-em-dia.local");
            assertThat(event.getFormattedMessage()).contains("/api/me/preferences");
        });
    }
}
