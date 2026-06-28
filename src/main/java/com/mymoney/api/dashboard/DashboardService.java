package com.mymoney.api.dashboard;

import com.mymoney.api.envelope.EnvelopeService;
import com.mymoney.api.envelope.EnvelopeView;
import com.mymoney.api.transaction.Transaction;
import com.mymoney.api.transaction.TransactionRepository;
import com.mymoney.api.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final EnvelopeService envelopeService;

    @Transactional(readOnly = true)
    public DashboardView getDashboard(LocalDate referenceMonth) {
        List<Transaction> transactions =
                transactionRepository.findByFilters(referenceMonth, null, null, null, null, null);
        List<EnvelopeView> envelopes = envelopeService.listForMonth(referenceMonth);

        BigDecimal totalIncome = sumByType(transactions, TransactionType.INCOME);
        BigDecimal totalExpense = sumByType(transactions, TransactionType.EXPENSE);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        List<Transaction> recentTransactions = transactions.stream()
                .sorted(Comparator.comparing(Transaction::getTransactionDate)
                        .thenComparing(Transaction::getCreatedAt)
                        .reversed())
                .limit(10)
                .toList();

        List<DashboardCategoryBreakdownItem> categoryBreakdown = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getCategory().getId(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)))
                .entrySet()
                .stream()
                .map(entry -> {
                    Transaction firstMatch = transactions.stream()
                            .filter(transaction ->
                                    transaction.getCategory().getId().equals(entry.getKey()))
                            .findFirst()
                            .orElseThrow();
                    return new DashboardCategoryBreakdownItem(
                            firstMatch.getCategory().getId().toString(),
                            firstMatch.getCategory().getName(),
                            entry.getValue());
                })
                .sorted(Comparator.comparing(DashboardCategoryBreakdownItem::amount)
                        .reversed())
                .toList();

        return new DashboardView(
                referenceMonth, totalIncome, totalExpense, balance, envelopes, recentTransactions, categoryBreakdown);
    }

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
