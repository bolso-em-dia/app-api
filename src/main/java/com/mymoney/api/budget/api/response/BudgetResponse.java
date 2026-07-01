package com.mymoney.api.budget.api.response;

import com.mymoney.api.transaction.api.response.TransactionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BudgetResponse(
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
        List<BudgetCategoryResponse> categories,
        List<TransactionResponse> transactions) {}
