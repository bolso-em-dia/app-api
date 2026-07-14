package com.mymoney.api.support;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountType;
import java.time.LocalDate;
import java.util.function.Consumer;

public final class AccountTestFactory {

    private AccountTestFactory() {}

    public static Account create() {
        return create(account -> {});
    }

    public static Account create(Consumer<Account> customizer) {
        var account = Account.builder()
                .name("Test Account")
                .type(AccountType.CHECKING)
                .createdInMonth(LocalDate.of(2026, 1, 1))
                .build();
        customizer.accept(account);
        return account;
    }
}
