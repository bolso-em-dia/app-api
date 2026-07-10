package com.mymoney.api.account.api.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AccountResponse(
        String id,
        String name,
        String type,
        String currency,
        String brand,
        String color,
        Short closingDay,
        Short dueDay,
        LocalDate createdInMonth,
        LocalDate archivedFromMonth,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
