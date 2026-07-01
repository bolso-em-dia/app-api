package com.mymoney.api.budget;

import java.math.BigDecimal;

public record BudgetCategoryBreakdownItem(String categoryId, String categoryName, BigDecimal amount) {}
