package com.mymoney.api.audit;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuditorResolver {

    public UUID resolveMemberId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getCredentials() instanceof Jwt jwt)) {
            throw new IllegalStateException("Authenticated JWT was not found.");
        }

        var memberId = jwt.getClaimAsString("mid");
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalStateException("JWT claim 'mid' was not found.");
        }

        return UUID.fromString(memberId);
    }
}
