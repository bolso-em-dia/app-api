package com.mymoney.api.budget;

import java.math.BigDecimal;

public record BudgetView(BudgetModel budgetModel, BigDecimal consumedAmount, BigDecimal remainingAmount) {}
