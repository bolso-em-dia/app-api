package com.mymoney.api.dashboard;

import java.math.BigDecimal;

public record DashboardCategoryBreakdownItem(String categoryId, String categoryName, BigDecimal amount) {}
