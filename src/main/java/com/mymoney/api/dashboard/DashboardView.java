package com.mymoney.api.dashboard;

import com.mymoney.api.budget.BudgetView;
import com.mymoney.api.transaction.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardView(
        LocalDate referenceMonth,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        BigDecimal availableBalance,
        BigDecimal reservedBudgetAmount,
        List<BudgetView> budgets,
        List<Transaction> recentTransactions,
        List<DashboardCategoryBreakdownItem> categoryBreakdown) {

    public DashboardView {
        budgets = List.copyOf(budgets);
        recentTransactions = List.copyOf(recentTransactions);
        categoryBreakdown = List.copyOf(categoryBreakdown);
    }
}
