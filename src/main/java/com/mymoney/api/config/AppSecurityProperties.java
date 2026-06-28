package com.mymoney.api.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
        String jwtSecret,
        long accessTokenMinutes,
        long refreshTokenDays,
        String refreshCookieName,
        List<String> allowedOrigins
) {
}
