package com.mymoney.api.preference.api.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record UpdateUserPreferencesRequest(
        UUID defaultAccountId, @NotBlank String locale, boolean showBalanceWithBudgets, boolean showForeignCurrency) {}
