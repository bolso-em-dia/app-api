package com.mymoney.api.dashboard.mapper;

import com.mymoney.api.dashboard.DashboardCategoryBreakdownItem;
import com.mymoney.api.dashboard.DashboardView;
import com.mymoney.api.dashboard.api.response.DashboardCategoryBreakdownResponse;
import com.mymoney.api.dashboard.api.response.DashboardResponse;
import com.mymoney.api.dashboard.api.response.DashboardSummaryResponse;
import com.mymoney.api.envelope.mapper.EnvelopeMapper;
import com.mymoney.api.transaction.mapper.TransactionMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DashboardMapper {

    private final EnvelopeMapper envelopeMapper;
    private final TransactionMapper transactionMapper;

    public DashboardMapper(EnvelopeMapper envelopeMapper, TransactionMapper transactionMapper) {
        this.envelopeMapper = envelopeMapper;
        this.transactionMapper = transactionMapper;
    }

    public DashboardResponse toResponse(DashboardView view) {
        return new DashboardResponse(
                view.referenceMonth(),
                new DashboardSummaryResponse(view.totalIncome(), view.totalExpense(), view.balance()),
                view.envelopes().stream()
                        .map(envelope -> envelopeMapper.toResponse(envelope, List.of()))
                        .toList(),
                view.recentTransactions().stream()
                        .map(transactionMapper::toResponse)
                        .toList(),
                view.categoryBreakdown().stream()
                        .map(this::toCategoryBreakdownResponse)
                        .toList());
    }

    private DashboardCategoryBreakdownResponse toCategoryBreakdownResponse(DashboardCategoryBreakdownItem item) {
        return new DashboardCategoryBreakdownResponse(item.categoryId(), item.categoryName(), item.amount());
    }
}
