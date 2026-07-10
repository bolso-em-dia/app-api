package com.mymoney.api.account.api.request;

import com.mymoney.api.account.AccountType;
import com.mymoney.api.account.CurrencyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountRequest(
        @NotBlank String name,
        @NotNull AccountType type,
        CurrencyType currency,
        String brand,
        String color,
        Integer closingDay,
        Integer dueDay) {}
