package com.mymoney.api.category.api.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CategoryResponse(
        String id,
        String name,
        String icon,
        String color,
        LocalDate createdInMonth,
        LocalDate archivedFromMonth,
        String replacementCategoryId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
