package com.mymoney.api.dashboard.api.response;

import com.mymoney.api.budget.api.response.BudgetResponse;
import com.mymoney.api.transaction.api.response.TransactionResponse;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        LocalDate referenceMonth,
        DashboardSummaryResponse summary,
        List<BudgetResponse> budgets,
        List<TransactionResponse> recentTransactions,
        List<DashboardCategoryBreakdownResponse> categoryBreakdown) {

    public DashboardResponse {
        budgets = List.copyOf(budgets);
        recentTransactions = List.copyOf(recentTransactions);
        categoryBreakdown = List.copyOf(categoryBreakdown);
    }
}
