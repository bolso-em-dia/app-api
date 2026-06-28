package com.mymoney.api.auth.api;

public record AuthUserResponse(
        String id,
        String name,
        String email,
        String role,
        boolean mesadaEnabled
) {
}
