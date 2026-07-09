package com.mymoney.api.security;

import com.mymoney.api.member.FamilyMemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || isAllowedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean mustChangePassword = familyMemberRepository
                .findByEmailIgnoreCase(authentication.getName())
                .filter(member -> member.isActive() && member.isMustChangePassword())
                .isPresent();

        if (mustChangePassword) {
            accessDeniedHandler.handle(request, response, new AccessDeniedException("Password change is required."));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedPath(String path) {
        return ALLOWED_PATHS.contains(path);
    }
}
