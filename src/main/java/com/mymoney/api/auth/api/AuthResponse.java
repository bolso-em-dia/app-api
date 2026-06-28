package com.mymoney.api.auth.api;

public record AuthResponse(
        String accessToken,
        long expiresInSeconds,
        AuthUserResponse user
) {
}
