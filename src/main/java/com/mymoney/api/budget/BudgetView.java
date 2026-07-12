package com.mymoney.api.budget;

import java.math.BigDecimal;

public record BudgetView(Budget budget, BigDecimal consumedAmount, BigDecimal remainingAmount) {}
