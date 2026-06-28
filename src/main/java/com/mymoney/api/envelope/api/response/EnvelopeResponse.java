package com.mymoney.api.envelope.api.response;

import com.mymoney.api.transaction.api.response.TransactionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EnvelopeResponse(
        String id,
        String name,
        String type,
        String ownerMemberId,
        String ownerMemberName,
        BigDecimal monthlyLimit,
        BigDecimal consumedAmount,
        BigDecimal remainingAmount,
        LocalDate createdInMonth,
        LocalDate archivedFromMonth,
        boolean active,
        List<EnvelopeCategoryResponse> categories,
        List<TransactionResponse> transactions) {}
