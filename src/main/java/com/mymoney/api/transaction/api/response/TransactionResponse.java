package com.mymoney.api.transaction.api.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        String id,
        String type,
        String ownershipType,
        String sourceType,
        String description,
        BigDecimal amount,
        LocalDate transactionDate,
        LocalDate referenceMonth,
        String accountId,
        String accountName,
        String categoryId,
        String categoryName,
        String memberId,
        String memberName,
        UUID installmentGroupId,
        Short installmentNumber,
        Short installmentTotal,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
