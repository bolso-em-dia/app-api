package com.mymoney.api.account.mapper;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.api.response.AccountOptionResponse;
import com.mymoney.api.account.api.response.AccountResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId().toString(),
                account.getName(),
                account.getType().name(),
                account.getBrand(),
                account.getColor(),
                account.getClosingDay(),
                account.getDueDay(),
                account.getCreatedInMonth(),
                account.getArchivedFromMonth(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }

    public AccountOptionResponse toOptionResponse(Account account) {
        return new AccountOptionResponse(
                account.getId().toString(), account.getName(), account.getType().name(), account.getColor());
    }
}
