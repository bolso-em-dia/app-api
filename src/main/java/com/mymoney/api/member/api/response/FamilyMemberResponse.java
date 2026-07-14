package com.mymoney.api.member.api.response;

import java.time.OffsetDateTime;

public record FamilyMemberResponse(
        String id,
        String name,
        String email,
        String role,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
