package com.mymoney.api.dashboard;

import com.mymoney.api.budget.BudgetService;
import com.mymoney.api.budget.BudgetView;
import com.mymoney.api.transaction.Transaction;
import com.mymoney.api.transaction.TransactionCategoryAnalyzer;
import com.mymoney.api.transaction.TransactionService;
import com.mymoney.api.transaction.TransactionType;
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

    @Transactional
    public DashboardView getDashboard(LocalDate referenceMonth) {
        List<Transaction> transactions = transactionService.listByFilters(referenceMonth, null, null, null, null, null);
        List<BudgetView> budgets = budgetService.listForMonth(referenceMonth);

        BigDecimal totalIncome = sumByType(transactions, TransactionType.INCOME);
        BigDecimal totalExpense = sumByType(transactions, TransactionType.EXPENSE);
        BigDecimal balance = totalIncome.subtract(totalExpense);
        BigDecimal reservedBudgetAmount = budgets.stream()
                .map(BudgetView::remainingAmount)
                .filter(remaining -> remaining.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal availableBalance = balance.subtract(reservedBudgetAmount);

        List<Transaction> recentTransactions = transactions.stream()
                .sorted(Comparator.comparing(Transaction::getTransactionDate)
                        .thenComparing(Transaction::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .limit(10)
                .toList();

        List<Transaction> expenseTransactions = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .toList();
        List<DashboardCategoryBreakdownItem> analyzedCategoryBreakdown = transactionCategoryAnalyzer
                .analyzeByCategory(
                        expenseTransactions,
                        Transaction::getConvertedAmount,
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

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(Transaction::getConvertedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
