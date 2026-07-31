package com.mymoney.api.transaction.api.request;

import com.mymoney.api.transaction.ReferenceMonthPolicy;
import jakarta.validation.constraints.NotNull;

public record UpdateTransactionReferenceMonthPolicyRequest(@NotNull ReferenceMonthPolicy referenceMonthPolicy) {}
