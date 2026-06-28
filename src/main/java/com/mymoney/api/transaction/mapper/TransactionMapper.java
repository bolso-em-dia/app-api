package com.mymoney.api.transaction.mapper;

import com.mymoney.api.transaction.Transaction;
import com.mymoney.api.transaction.api.response.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId().toString(),
                transaction.getType().name(),
                transaction.getOwnershipType().name(),
                transaction.getSourceType().name(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getReferenceMonth(),
                transaction.getAccount().getId().toString(),
                transaction.getAccount().getName(),
                transaction.getCategory().getId().toString(),
                transaction.getCategory().getName(),
                transaction.getMember() == null
                        ? null
                        : transaction.getMember().getId().toString(),
                transaction.getMember() == null ? null : transaction.getMember().getName(),
                transaction.getInstallmentGroupId(),
                transaction.getInstallmentNumber(),
                transaction.getInstallmentTotal(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt());
    }
}
