package com.mymoney.api.budget.api.request;

import com.mymoney.api.budget.BudgetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateBudgetRequest(
        @NotBlank String name,
        @NotNull BudgetType type,
        UUID ownerMemberId,
        List<UUID> categoryIds,
        @NotNull @DecimalMin("0.01") BigDecimal monthlyLimit) {}
