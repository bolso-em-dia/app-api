package com.mymoney.api.category.api.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CategoryResponse(
        String id,
        String name,
        String icon,
        String color,
        LocalDate createdInMonth,
        LocalDate archivedFromMonth,
        String replacementCategoryId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public CategoryResponse(
            UUID id,
            String name,
            String icon,
            String color,
            LocalDate createdInMonth,
            LocalDate archivedFromMonth,
            UUID replacementCategoryId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this(
                id.toString(),
                name,
                icon,
                color,
                createdInMonth,
                archivedFromMonth,
                replacementCategoryId == null ? null : replacementCategoryId.toString(),
                createdAt,
                updatedAt);
    }
}
