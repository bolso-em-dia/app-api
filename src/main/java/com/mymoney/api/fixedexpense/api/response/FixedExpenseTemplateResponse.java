package com.mymoney.api.fixedexpense.api.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

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
        OffsetDateTime updatedAt) {}
