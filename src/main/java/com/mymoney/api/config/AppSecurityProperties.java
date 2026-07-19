package com.mymoney.api.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
        String jwtSecret,
        long accessTokenMinutes,
        long refreshTokenDays,
        String refreshCookieName,
        boolean refreshCookieSecure,
        List<String> allowedOrigins) {

    public AppSecurityProperties(
            String jwtSecret,
            long accessTokenMinutes,
            long refreshTokenDays,
            String refreshCookieName,
            boolean refreshCookieSecure,
            List<String> allowedOrigins) {
        this.jwtSecret = jwtSecret;
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
        this.refreshCookieName = refreshCookieName;
        this.refreshCookieSecure = refreshCookieSecure;
        this.allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
