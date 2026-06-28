package com.mymoney.api.auth;

import com.mymoney.api.auth.api.AuthResponse;
import com.mymoney.api.auth.api.AuthUserResponse;
import com.mymoney.api.auth.api.LoginRequest;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final FamilyMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            FamilyMemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated.");
        }

        FamilyMember member = memberRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User was not found."));

        return mapUser(member);
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
                member.isAllowanceEnabled());
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }
}
