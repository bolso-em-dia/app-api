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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        FamilyMember member = memberRepository
                .findByEmailIgnoreCase(request.email())
                .filter(FamilyMember::isActive)
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw invalidCredentials();
        }

        return issueTokens(member, response);
    }

    @Transactional
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = refreshTokenService
                .extractRefreshToken(request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is missing."));

        RefreshToken refreshToken = refreshTokenService
                .findValidToken(rawRefreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid."));

        refreshTokenService.revoke(refreshToken);
        return issueTokens(refreshToken.getMember(), response);
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        refreshTokenService
                .extractRefreshToken(request)
                .flatMap(refreshTokenService::findValidToken)
                .ifPresent(refreshTokenService::revoke);
        refreshTokenService.clearRefreshCookie(response);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse currentUser() {
        return mapUser(authenticatedMemberResolver.resolve());
    }

    @Transactional
    public AuthUserResponse changeCurrentUserPassword(ChangePasswordRequest request) {
        FamilyMember member = authenticatedMemberResolver.resolve();

        if (!passwordEncoder.matches(request.currentPassword(), member.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Current password is incorrect.");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Password confirmation does not match.");
        }

        member.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        member.setMustChangePassword(false);
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
}
