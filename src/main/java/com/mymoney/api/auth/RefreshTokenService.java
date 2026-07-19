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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppSecurityProperties properties;

    @Transactional
    public void rotate(FamilyMember member, HttpServletResponse response) {
        var rawToken = UUID.randomUUID() + "." + UUID.randomUUID();

        var refreshToken = new RefreshToken();
        refreshToken.setMember(member);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setExpiresAt(OffsetDateTime.now().plusDays(properties.refreshTokenDays()));
        refreshTokenRepository.save(refreshToken);
        log.debug("Rotated refresh token for memberId={}", member.getId());

        var cookie = ResponseCookie.from(properties.refreshCookieName(), rawToken)
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(properties.refreshTokenDays()))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findValidToken(String rawToken) {
        var token = refreshTokenRepository.findByTokenHash(hash(rawToken));
        if (token.isEmpty()) {
            log.debug("Refresh token validation failed because token was not found");
            return Optional.empty();
        }

        var refreshToken = token.orElseThrow();
        if (refreshToken.isExpired()) {
            log.warn(
                    "Refresh token validation failed because token is expired for memberId={}",
                    refreshToken.getMember().getId());
            return Optional.empty();
        }
        if (refreshToken.isRevoked()) {
            log.warn(
                    "Refresh token validation failed because token is revoked for memberId={}",
                    refreshToken.getMember().getId());
            return Optional.empty();
        }
        if (!refreshToken.getMember().isActive()) {
            log.warn(
                    "Refresh token validation failed because member is inactive for memberId={}",
                    refreshToken.getMember().getId());
            return Optional.empty();
        }

        return Optional.of(refreshToken);
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(token);
        log.debug("Revoked refresh token for memberId={}", token.getMember().getId());
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies == null) {
            log.warn(
                    "Refresh token cookie extraction failed because request has no cookies. path={}",
                    request.getRequestURI());
            return Optional.empty();
        }

        for (Cookie cookie : cookies) {
            if (properties.refreshCookieName().equals(cookie.getName())) {
                if (cookie.getValue() == null || cookie.getValue().isBlank()) {
                    log.warn(
                            "Refresh token cookie extraction failed because cookie value is empty. path={}",
                            request.getRequestURI());
                    return Optional.empty();
                }
                return Optional.of(cookie.getValue());
            }
        }

        log.warn(
                "Refresh token cookie extraction failed because cookie {} was not found. path={} cookieCount={}",
                properties.refreshCookieName(),
                request.getRequestURI(),
                cookies.length);
        return Optional.empty();
    }

    public void clearRefreshCookie(HttpServletResponse response) {
        var cookie = ResponseCookie.from(properties.refreshCookieName(), "")
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
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

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        var cutoff = OffsetDateTime.now().minusDays(7);
        var deleted = refreshTokenRepository.deleteByExpiresAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} expired refresh tokens older than {}.", deleted, cutoff);
            return;
        }

        log.info("No expired refresh tokens to purge. cutoff={}", cutoff);
    }
}
