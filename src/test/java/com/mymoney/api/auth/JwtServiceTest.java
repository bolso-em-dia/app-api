package com.mymoney.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.mymoney.api.config.AppSecurityProperties;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void generateAccessToken_shouldIncludeMidClaim() {
        var jwtService = new JwtService(new AppSecurityProperties(
                "test-secret-test-secret-test-secret",
                15,
                7,
                "bolso_em_dia_refresh_token",
                true,
                List.of("http://localhost:4173")));
        var memberId = UUID.randomUUID();
        var member = FamilyMember.builder()
                .id(memberId)
                .name("Admin")
                .email("admin@bolso-em-dia.local")
                .passwordHash("hash")
                .role(FamilyRole.ADMIN)
                .active(true)
                .mustChangePassword(false)
                .build();

        var token = jwtService.generateAccessToken(member);
        var jwt = jwtService.decode(token);

        assertThat(jwt.getClaimAsString("mid")).isEqualTo(memberId.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat((Boolean) jwt.getClaim("mustChangePwd")).isFalse();
    }
}
