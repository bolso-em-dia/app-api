package com.mymoney.api.auth;

import com.mymoney.api.auth.api.request.ChangePasswordRequest;
import com.mymoney.api.auth.api.request.LoginRequest;
import com.mymoney.api.auth.api.response.AuthResponse;
import com.mymoney.api.auth.api.response.AuthUserResponse;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.preference.UserPreferencesService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final FamilyMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserPreferencesService userPreferencesService;
    private final AuthenticatedMemberResolver authenticatedMemberResolver;

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        var normalizedEmail = request.email().trim().toLowerCase();
        log.info("Auth login attempt for email={}", normalizedEmail);

        var member = memberRepository
                .findByEmailIgnoreCase(request.email())
                .filter(FamilyMember::isActive)
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            log.warn("Auth login rejected for email={}", normalizedEmail);
            throw invalidCredentials();
        }

        log.info("Auth login succeeded for memberId={} email={}", member.getId(), normalizedEmail);
        return issueTokens(member, response);
    }

    @Transactional
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        log.debug("Auth refresh started for path={} cookieCount={}", request.getRequestURI(), cookieCount(request));

        var rawRefreshToken = refreshTokenService.extractRefreshToken(request).orElseThrow(() -> {
            log.warn(
                    "Auth refresh rejected because refresh token cookie is missing. path={} cookieCount={}",
                    request.getRequestURI(),
                    cookieCount(request));
            return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is missing.");
        });

        var refreshToken = refreshTokenService.findValidToken(rawRefreshToken).orElseThrow(() -> {
            log.warn("Auth refresh rejected because refresh token is invalid. path={}", request.getRequestURI());
            return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid.");
        });

        refreshTokenService.revoke(refreshToken);
        log.info(
                "Auth refresh succeeded for memberId={}",
                refreshToken.getMember().getId());
        return issueTokens(refreshToken.getMember(), response);
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        log.debug("Auth logout started for path={} cookieCount={}", request.getRequestURI(), cookieCount(request));
        refreshTokenService
                .extractRefreshToken(request)
                .flatMap(refreshTokenService::findValidToken)
                .ifPresent(token -> {
                    refreshTokenService.revoke(token);
                    log.debug(
                            "Auth logout revoked refresh token for memberId={}",
                            token.getMember().getId());
                });
        refreshTokenService.clearRefreshCookie(response);
        log.debug("Auth logout cleared refresh cookie for path={}", request.getRequestURI());
    }

    @Transactional(readOnly = true)
    public AuthUserResponse currentUser() {
        return mapUser(authenticatedMemberResolver.resolve());
    }

    @Transactional
    public AuthUserResponse changeCurrentUserPassword(ChangePasswordRequest request) {
        var member = authenticatedMemberResolver.resolve();

        if (!passwordEncoder.matches(request.currentPassword(), member.getPasswordHash())) {
            log.warn(
                    "Auth password change rejected for memberId={} because current password does not match",
                    member.getId());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Current password is incorrect.");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            log.warn(
                    "Auth password change rejected for memberId={} because confirmation does not match",
                    member.getId());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Password confirmation does not match.");
        }

        member.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        member.setMustChangePassword(false);
        log.info("Auth password changed for memberId={}", member.getId());
        return mapUser(memberRepository.save(member));
    }

    private AuthResponse issueTokens(FamilyMember member, HttpServletResponse response) {
        refreshTokenService.rotate(member, response);
        return new AuthResponse(
                jwtService.generateAccessToken(member),
                jwtService.accessTokenDuration().toSeconds(),
                mapUser(member));
    }

    private AuthUserResponse mapUser(FamilyMember member) {
        return new AuthUserResponse(
                member.getId().toString(),
                member.getName(),
                member.getEmail(),
                member.getRole().name(),
                member.isMustChangePassword(),
                userPreferencesService.resolvePreferences(member));
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }

    private int cookieCount(HttpServletRequest request) {
        var cookies = request.getCookies();
        return cookies == null ? 0 : cookies.length;
    }
}
