package com.mymoney.api.auth.api.response;

public record AuthResponse(String accessToken, long expiresInSeconds, AuthUserResponse user) {}
