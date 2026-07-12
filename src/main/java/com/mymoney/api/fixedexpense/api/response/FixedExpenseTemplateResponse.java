package com.mymoney.api.fixedexpense.api.response;

import com.mymoney.api.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FixedExpenseTemplateResponse(
        String id,
        String name,
        String type,
        BigDecimal amount,
        BigDecimal convertedAmount,
        BigDecimal exchangeRate,
        String currency,
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
            TransactionType type,
            BigDecimal amount,
            BigDecimal convertedAmount,
            BigDecimal exchangeRate,
            String currency,
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
                type.name(),
                amount,
                convertedAmount,
                exchangeRate,
                currency,
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
