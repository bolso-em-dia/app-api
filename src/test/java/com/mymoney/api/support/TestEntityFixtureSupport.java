package com.mymoney.api.support;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.budget.Budget;
import com.mymoney.api.budget.BudgetRepository;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryRepository;
import com.mymoney.api.exchangerate.ExchangeRate;
import com.mymoney.api.exchangerate.ExchangeRateRepository;
import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateRepository;
import com.mymoney.api.transaction.Transaction;
import com.mymoney.api.transaction.TransactionRepository;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class TestEntityFixtureSupport {

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final FixedExpenseTemplateRepository fixedExpenseTemplateRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public TestEntityFixtureSupport(
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            BudgetRepository budgetRepository,
            TransactionRepository transactionRepository,
            FixedExpenseTemplateRepository fixedExpenseTemplateRepository,
            ExchangeRateRepository exchangeRateRepository) {
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.fixedExpenseTemplateRepository = fixedExpenseTemplateRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    public Account persistAccount(Consumer<Account> customizer) {
        return accountRepository.save(AccountTestFactory.create(customizer));
    }

    public Category persistCategory(Consumer<Category> customizer) {
        return categoryRepository.save(CategoryTestFactory.create(customizer));
    }

    public Budget persistBudget(Consumer<Budget> customizer) {
        return budgetRepository.save(BudgetTestFactory.create(customizer));
    }

    public Transaction persistTransaction(Consumer<Transaction> customizer) {
        return transactionRepository.save(TransactionTestFactory.create(customizer));
    }

    public FixedExpenseTemplate persistFixedExpenseTemplate(Consumer<FixedExpenseTemplate> customizer) {
        return fixedExpenseTemplateRepository.save(FixedExpenseTemplateTestFactory.create(customizer));
    }

    public ExchangeRate persistExchangeRate(Consumer<ExchangeRate> customizer) {
        return exchangeRateRepository.save(ExchangeRateTestFactory.create(customizer));
    }
}
