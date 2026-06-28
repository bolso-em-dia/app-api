package com.mymoney.api.envelope.mapper;

import com.mymoney.api.envelope.EnvelopeCategoryBreakdownItem;
import com.mymoney.api.envelope.EnvelopeModel;
import com.mymoney.api.envelope.EnvelopeView;
import com.mymoney.api.envelope.api.response.EnvelopeCategoryBreakdownResponse;
import com.mymoney.api.envelope.api.response.EnvelopeCategoryResponse;
import com.mymoney.api.envelope.api.response.EnvelopeResponse;
import com.mymoney.api.transaction.api.response.TransactionResponse;
import com.mymoney.api.transaction.mapper.TransactionMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EnvelopeMapper {

    private final TransactionMapper transactionMapper;

    public EnvelopeMapper(TransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
    }

    public EnvelopeResponse toResponse(EnvelopeView view, List<TransactionResponse> transactions) {
        EnvelopeModel envelope = view.envelopeModel();
        return new EnvelopeResponse(
                envelope.getId().toString(),
                envelope.getName(),
                envelope.getType().name(),
                envelope.getOwnerMember() == null
                        ? null
                        : envelope.getOwnerMember().getId().toString(),
                envelope.getOwnerMember() == null
                        ? null
                        : envelope.getOwnerMember().getName(),
                envelope.getMonthlyLimit(),
                view.consumedAmount(),
                view.remainingAmount(),
                envelope.getCreatedInMonth(),
                envelope.getArchivedFromMonth(),
                envelope.isActive(),
                envelope.getCategories().stream()
                        .map(category -> new EnvelopeCategoryResponse(
                                category.getId().toString(), category.getName(), category.getColor()))
                        .toList(),
                transactions);
    }

    public List<EnvelopeCategoryBreakdownResponse> toCategoryBreakdownResponses(
            List<EnvelopeCategoryBreakdownItem> breakdownItems) {
        return breakdownItems.stream()
                .map(item ->
                        new EnvelopeCategoryBreakdownResponse(item.categoryId(), item.categoryName(), item.amount()))
                .toList();
    }

    public List<TransactionResponse> toTransactionResponses(
            List<com.mymoney.api.transaction.Transaction> transactions) {
        return transactions.stream().map(transactionMapper::toResponse).toList();
    }
}
