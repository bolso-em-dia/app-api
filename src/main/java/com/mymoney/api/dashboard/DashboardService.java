package com.mymoney.api.dashboard;

import com.mymoney.api.budget.BudgetService;
import com.mymoney.api.budget.BudgetView;
import com.mymoney.api.transaction.TransactionCategoryAnalyzer;
import com.mymoney.api.transaction.TransactionService;
import com.mymoney.api.transaction.TransactionType;
import com.mymoney.api.transaction.api.response.TransactionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final TransactionCategoryAnalyzer transactionCategoryAnalyzer;

    @Transactional(readOnly = true)
    public DashboardView getDashboard(LocalDate referenceMonth) {
        var transactions = transactionService.listResponsesByFilters(referenceMonth, null, null, null, null, null);
        var budgets = budgetService.listForMonth(referenceMonth);

        var totalIncome = sumByType(transactions, TransactionType.INCOME.name());
        var totalExpense = sumByType(transactions, TransactionType.EXPENSE.name());
        var balance = totalIncome.subtract(totalExpense);
        var reservedBudgetAmount = budgets.stream()
                .map(BudgetView::remainingAmount)
                .filter(remaining -> remaining.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var availableBalance = balance.subtract(reservedBudgetAmount);

        var recentTransactions = transactions.stream()
                .sorted(Comparator.comparing(TransactionResponse::transactionDate)
                        .thenComparing(TransactionResponse::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .limit(10)
                .toList();

        var expenseTransactions = transactions.stream()
                .filter(transaction -> transaction.type().equals(TransactionType.EXPENSE.name()))
                .toList();
        var analyzedCategoryBreakdown = transactionCategoryAnalyzer
                .analyzeByCategory(
                        expenseTransactions,
                        TransactionResponse::categoryId,
                        TransactionResponse::categoryName,
                        TransactionResponse::convertedAmount,
                        Comparator.comparing(TransactionCategoryAnalyzer.CategoryAmount::amount)
                                .reversed())
                .stream()
                .map(item -> new DashboardCategoryBreakdownItem(item.categoryId(), item.categoryName(), item.amount()))
                .toList();

        return new DashboardView(
                referenceMonth,
                totalIncome,
                totalExpense,
                balance,
                availableBalance,
                reservedBudgetAmount,
                budgets,
                recentTransactions,
                analyzedCategoryBreakdown);
    }

    private BigDecimal sumByType(List<TransactionResponse> transactions, String type) {
        return transactions.stream()
                .filter(transaction -> transaction.type().equals(type))
                .map(TransactionResponse::convertedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
