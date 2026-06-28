package com.mymoney.api.dashboard.api.response;

import com.mymoney.api.envelope.api.response.EnvelopeResponse;
import com.mymoney.api.transaction.api.response.TransactionResponse;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        LocalDate referenceMonth,
        DashboardSummaryResponse summary,
        List<EnvelopeResponse> envelopes,
        List<TransactionResponse> recentTransactions,
        List<DashboardCategoryBreakdownResponse> categoryBreakdown) {

    public DashboardResponse {
        envelopes = List.copyOf(envelopes);
        recentTransactions = List.copyOf(recentTransactions);
        categoryBreakdown = List.copyOf(categoryBreakdown);
    }
}
