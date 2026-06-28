package com.mymoney.api.account.api.request;

import com.mymoney.api.account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotBlank String name,
        @NotNull AccountType type,
        String brand,
        String color,
        Integer closingDay,
        Integer dueDay) {}
