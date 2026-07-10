package com.mymoney.api.transaction.api.response;

import com.mymoney.api.transaction.OwnershipType;
import com.mymoney.api.transaction.TransactionSourceType;
import com.mymoney.api.transaction.TransactionType;
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
        BigDecimal originalAmount,
        String currency,
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
        String fixedExpenseTemplateId,
        boolean projected,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public TransactionResponse(
            UUID id,
            TransactionType type,
            OwnershipType ownershipType,
            TransactionSourceType sourceType,
            String description,
            BigDecimal amount,
            BigDecimal originalAmount,
            String currency,
            LocalDate transactionDate,
            LocalDate referenceMonth,
            UUID accountId,
            String accountName,
            UUID categoryId,
            String categoryName,
            UUID memberId,
            String memberName,
            UUID installmentGroupId,
            Short installmentNumber,
            Short installmentTotal,
            UUID fixedExpenseTemplateId,
            boolean projected,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this(
                id.toString(),
                type.name(),
                ownershipType.name(),
                sourceType.name(),
                description,
                amount,
                originalAmount,
                currency,
                transactionDate,
                referenceMonth,
                accountId.toString(),
                accountName,
                categoryId.toString(),
                categoryName,
                memberId == null ? null : memberId.toString(),
                memberName,
                installmentGroupId,
                installmentNumber,
                installmentTotal,
                fixedExpenseTemplateId == null ? null : fixedExpenseTemplateId.toString(),
                projected,
                createdAt,
                updatedAt);
    }
}
