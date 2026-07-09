package com.mymoney.api.fixedexpense.api.request;

import com.mymoney.api.transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateFixedExpenseTemplateRequest(
        @NotBlank String name,
        @NotNull TransactionType type,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull UUID categoryId,
        @NotNull UUID accountId,
        @NotNull Integer dueDay) {}
