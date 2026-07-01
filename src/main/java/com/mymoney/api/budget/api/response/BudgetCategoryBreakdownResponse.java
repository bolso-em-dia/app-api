package com.mymoney.api.budget.api.response;

import java.math.BigDecimal;

public record BudgetCategoryBreakdownResponse(String categoryId, String categoryName, BigDecimal amount) {}
