package com.mymoney.api.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

class AuditorResolverTest {

    private final AuditorResolver auditorResolver = new AuditorResolver();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveMemberId_shouldReadMidClaimFromJwt() {
        var memberId = UUID.randomUUID();
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("admin@bolso-em-dia.local")
                .claim("mid", memberId.toString())
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(jwt.getSubject(), jwt));

        assertThat(auditorResolver.resolveMemberId()).isEqualTo(memberId);
    }

    @Test
    void resolveMemberId_shouldThrowWhenMidClaimIsMissing() {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("admin@bolso-em-dia.local")
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(jwt.getSubject(), jwt));

        assertThatThrownBy(auditorResolver::resolveMemberId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT claim 'mid' was not found.");
    }
}
