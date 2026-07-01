package com.mymoney.api.fixedexpense.api.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FixedExpenseTemplateResponse(
        String id,
        String name,
        BigDecimal amount,
        String categoryId,
        String categoryName,
        String accountId,
        String accountName,
        Short dueDay,
        LocalDate createdInMonth,
        LocalDate archivedFromMonth,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public FixedExpenseTemplateResponse(
            UUID id,
            String name,
            BigDecimal amount,
            UUID categoryId,
            String categoryName,
            UUID accountId,
            String accountName,
            Short dueDay,
            LocalDate createdInMonth,
            LocalDate archivedFromMonth,
            boolean active,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this(
                id.toString(),
                name,
                amount,
                categoryId.toString(),
                categoryName,
                accountId.toString(),
                accountName,
                dueDay,
                createdInMonth,
                archivedFromMonth,
                active,
                createdAt,
                updatedAt);
    }
}
