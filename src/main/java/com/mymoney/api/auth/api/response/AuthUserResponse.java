package com.mymoney.api.auth.api.response;

import com.mymoney.api.preference.api.response.UserPreferencesResponse;

public record AuthUserResponse(
        String id,
        String name,
        String email,
        String role,
        boolean mustChangePassword,
        UserPreferencesResponse preferences) {}
