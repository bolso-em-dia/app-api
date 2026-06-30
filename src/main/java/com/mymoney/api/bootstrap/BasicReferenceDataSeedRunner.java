package com.mymoney.api.bootstrap;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.account.AccountType;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BasicReferenceDataSeedRunner implements CommandLineRunner {

    private static final String PIX_ACCOUNT_NAME = "Pix";

    private static final List<SeedCategory> DEFAULT_CATEGORIES = List.of(
            new SeedCategory("Compras", "shopping-cart"),
            new SeedCategory("Cuidados Pessoais", "sparkles"),
            new SeedCategory("Educação", "book"),
            new SeedCategory("Empréstimos", "hand-coins"),
            new SeedCategory("Farmácia", "pill"),
            new SeedCategory("Lazer", "gamepad"),
            new SeedCategory("Mercado", "shopping-basket"),
            new SeedCategory("Pets", "paw-print"),
            new SeedCategory("Saúde", "heart"),
            new SeedCategory("Serviços", "wrench"),
            new SeedCategory("Transporte", "car"),
            new SeedCategory("Viagem", "plane"));

    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void run(String... args) {
        LocalDate referenceMonth = currentReferenceMonth();
        seedCategories(referenceMonth);
        seedPixAccount(referenceMonth);
    }

    private void seedCategories(LocalDate referenceMonth) {
        for (SeedCategory seedCategory : DEFAULT_CATEGORIES) {
            if (categoryRepository.findByNormalizedName(seedCategory.name()).isPresent()) {
                continue;
            }

            Category category = new Category();
            category.setName(seedCategory.name());
            category.setIcon(seedCategory.iconId());
            category.setColor(null);
            category.setCreatedInMonth(referenceMonth);
            categoryRepository.save(category);
        }
    }

    private void seedPixAccount(LocalDate referenceMonth) {
        if (accountRepository.findByNormalizedName(PIX_ACCOUNT_NAME).isPresent()) {
            return;
        }

        Account account = new Account();
        account.setName(PIX_ACCOUNT_NAME);
        account.setType(AccountType.CHECKING);
        account.setBrand(null);
        account.setColor(null);
        account.setClosingDay(null);
        account.setDueDay(null);
        account.setCreatedInMonth(referenceMonth);
        accountRepository.save(account);
    }

    private LocalDate currentReferenceMonth() {
        return YearMonth.now().atDay(1);
    }

    private record SeedCategory(String name, String iconId) {}
}
