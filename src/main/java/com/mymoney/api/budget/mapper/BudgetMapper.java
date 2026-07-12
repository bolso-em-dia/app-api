package com.mymoney.api.budget.mapper;

import com.mymoney.api.budget.Budget;
import com.mymoney.api.budget.BudgetCategoryBreakdownItem;
import com.mymoney.api.budget.BudgetView;
import com.mymoney.api.budget.api.response.BudgetCategoryBreakdownResponse;
import com.mymoney.api.budget.api.response.BudgetCategoryResponse;
import com.mymoney.api.budget.api.response.BudgetResponse;
import com.mymoney.api.transaction.api.response.TransactionResponse;
import com.mymoney.api.transaction.mapper.TransactionMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BudgetMapper {

    private final TransactionMapper transactionMapper;

    public BudgetMapper(TransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
    }

    public BudgetResponse toResponse(BudgetView view, List<TransactionResponse> transactions) {
        Budget budget = view.budget();
        return new BudgetResponse(
                budget.getId().toString(),
                budget.getName(),
                budget.getType().name(),
                budget.getOwnerMember() == null
                        ? null
                        : budget.getOwnerMember().getId().toString(),
                budget.getOwnerMember() == null ? null : budget.getOwnerMember().getName(),
                budget.getMonthlyLimit(),
                view.consumedAmount(),
                view.remainingAmount(),
                budget.getCreatedInMonth(),
                budget.getArchivedFromMonth(),
                budget.isActive(),
                budget.getCategories().stream()
                        .map(category -> new BudgetCategoryResponse(
                                category.getId().toString(), category.getName(), category.getColor()))
                        .toList(),
                transactions);
    }

    public List<BudgetCategoryBreakdownResponse> toCategoryBreakdownResponses(
            List<BudgetCategoryBreakdownItem> breakdownItems) {
        return breakdownItems.stream()
                .map(item -> new BudgetCategoryBreakdownResponse(item.categoryId(), item.categoryName(), item.amount()))
                .toList();
    }

    public List<TransactionResponse> toTransactionResponses(
            List<com.mymoney.api.transaction.Transaction> transactions) {
        return transactions.stream().map(transactionMapper::toResponse).toList();
    }
}
