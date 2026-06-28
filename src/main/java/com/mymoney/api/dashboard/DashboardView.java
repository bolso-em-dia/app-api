package com.mymoney.api.dashboard;

import com.mymoney.api.envelope.EnvelopeView;
import com.mymoney.api.transaction.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardView(
        LocalDate referenceMonth,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        List<EnvelopeView> envelopes,
        List<Transaction> recentTransactions,
        List<DashboardCategoryBreakdownItem> categoryBreakdown) {

    public DashboardView {
        envelopes = List.copyOf(envelopes);
        recentTransactions = List.copyOf(recentTransactions);
        categoryBreakdown = List.copyOf(categoryBreakdown);
    }
}
