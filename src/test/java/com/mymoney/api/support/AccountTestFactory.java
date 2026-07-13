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
        var account = new Account();
        account.setName("Test Account");
        account.setType(AccountType.CHECKING);
        account.setCreatedInMonth(LocalDate.of(2026, 1, 1));
        customizer.accept(account);
        return account;
    }
}
