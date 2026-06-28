package com.mymoney.api.dashboard.api.response;

import java.math.BigDecimal;

public record DashboardSummaryResponse(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal balance) {}
