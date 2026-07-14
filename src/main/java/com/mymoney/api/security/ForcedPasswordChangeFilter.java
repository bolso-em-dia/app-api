package com.mymoney.api.security;

import com.mymoney.api.member.FamilyMemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class ForcedPasswordChangeFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATHS =
            Set.of("/api/auth/me", "/api/auth/logout", "/api/auth/change-password");

    private final FamilyMemberRepository familyMemberRepository;
    private final ApiAccessDeniedHandler accessDeniedHandler;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || isAllowedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean needsPasswordChange = isFlaggedInJwt(authentication) && isFlaggedInDatabase(authentication);

        if (needsPasswordChange) {
            accessDeniedHandler.handle(request, response, new AccessDeniedException("Password change is required."));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isFlaggedInJwt(Authentication authentication) {
        if (authentication.getCredentials() instanceof Jwt jwt) {
            return Boolean.TRUE.equals(jwt.getClaim("mustChangePwd"));
        }
        return false;
    }

    private boolean isFlaggedInDatabase(Authentication authentication) {
        return familyMemberRepository
                .findByEmailIgnoreCase(authentication.getName())
                .filter(member -> member.isActive() && member.isMustChangePassword())
                .isPresent();
    }

    private boolean isAllowedPath(String path) {
        return ALLOWED_PATHS.contains(path);
    }
}
