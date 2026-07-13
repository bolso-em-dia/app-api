package com.mymoney.api.dashboard.mapper;

import com.mymoney.api.budget.mapper.BudgetMapper;
import com.mymoney.api.dashboard.DashboardCategoryBreakdownItem;
import com.mymoney.api.dashboard.DashboardView;
import com.mymoney.api.dashboard.api.response.DashboardCategoryBreakdownResponse;
import com.mymoney.api.dashboard.api.response.DashboardResponse;
import com.mymoney.api.dashboard.api.response.DashboardSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DashboardMapper {

    private final BudgetMapper budgetMapper;

    public DashboardMapper(BudgetMapper budgetMapper) {
        this.budgetMapper = budgetMapper;
    }

    public DashboardResponse toResponse(DashboardView view) {
        return new DashboardResponse(
                view.referenceMonth(),
                new DashboardSummaryResponse(
                        view.totalIncome(),
                        view.totalExpense(),
                        view.balance(),
                        view.availableBalance(),
                        view.reservedBudgetAmount()),
                view.budgets().stream()
                        .map(budget -> budgetMapper.toResponse(budget, List.of()))
                        .toList(),
                view.recentTransactions(),
                view.categoryBreakdown().stream()
                        .map(this::toCategoryBreakdownResponse)
                        .toList());
    }

    private DashboardCategoryBreakdownResponse toCategoryBreakdownResponse(DashboardCategoryBreakdownItem item) {
        return new DashboardCategoryBreakdownResponse(item.categoryId(), item.categoryName(), item.amount());
    }
}
