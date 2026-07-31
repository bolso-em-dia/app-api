package com.mymoney.api.transaction.api.request;

import com.mymoney.api.transaction.MoveTransactionDateScope;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MoveTransactionDateRequest(
        @NotNull MoveTransactionDateScope scope,
        @NotNull LocalDate expectedCurrentTransactionDate,
        @NotNull LocalDate confirmedNewTransactionDate) {}
