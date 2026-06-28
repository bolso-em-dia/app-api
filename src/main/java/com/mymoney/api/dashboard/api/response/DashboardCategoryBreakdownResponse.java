package com.mymoney.api.dashboard.api.response;

import java.math.BigDecimal;

public record DashboardCategoryBreakdownResponse(String categoryId, String categoryName, BigDecimal amount) {}
