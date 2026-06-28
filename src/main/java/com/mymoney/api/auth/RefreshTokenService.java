package com.mymoney.api.auth;

import com.mymoney.api.config.AppSecurityProperties;
import com.mymoney.api.member.FamilyMember;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppSecurityProperties properties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, AppSecurityProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
    }

    @Transactional
    public void rotate(FamilyMember member, HttpServletResponse response) {
        String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setMember(member);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setExpiresAt(OffsetDateTime.now().plusDays(properties.refreshTokenDays()));
        refreshTokenRepository.save(refreshToken);

        ResponseCookie cookie = ResponseCookie.from(properties.refreshCookieName(), rawToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(properties.refreshTokenDays()))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findValidToken(String rawToken) {
        return refreshTokenRepository
                .findByTokenHash(hash(rawToken))
                .filter(token -> !token.isExpired())
                .filter(token -> !token.isRevoked())
                .filter(token -> token.getMember().isActive());
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(token);
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (properties.refreshCookieName().equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    public void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(properties.refreshCookieName(), "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
