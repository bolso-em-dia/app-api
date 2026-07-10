package com.mymoney.api.auth;

import com.mymoney.api.config.AppSecurityProperties;
import com.mymoney.api.member.FamilyMember;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration accessTokenDuration;

    public JwtService(AppSecurityProperties properties) {
        var key = new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        this.accessTokenDuration = Duration.ofMinutes(properties.accessTokenMinutes());
    }

    public String generateAccessToken(FamilyMember member) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("bolso-em-dia")
                .subject(member.getEmail())
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenDuration))
                .claim("role", member.getRole().name())
                .claim("mustChangePwd", member.isMustChangePassword())
                .build();

        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    public Duration accessTokenDuration() {
        return accessTokenDuration;
    }
}
