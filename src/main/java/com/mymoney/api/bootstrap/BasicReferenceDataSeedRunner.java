package com.mymoney.api.bootstrap;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.account.AccountType;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryRepository;
import com.mymoney.api.shared.DateProvider;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BasicReferenceDataSeedRunner implements CommandLineRunner {

    private static final String PIX_ACCOUNT_NAME = "Pix";

    private static final List<SeedCategory> DEFAULT_CATEGORIES = List.of(
            new SeedCategory("Compras", "shopping-cart", "#3b82f6"),
            new SeedCategory("Cuidados Pessoais", "sparkles", "#ec4899"),
            new SeedCategory("Educação", "book", "#8b5cf6"),
            new SeedCategory("Empréstimos", "hand-coins", "#f97316"),
            new SeedCategory("Farmácia", "pill", "#10b981"),
            new SeedCategory("Lazer", "gamepad", "#a855f7"),
            new SeedCategory("Mercado", "shopping-basket", "#84cc16"),
            new SeedCategory("Pets", "paw-print", "#f59e0b"),
            new SeedCategory("Saúde", "heart", "#ef4444"),
            new SeedCategory("Serviços", "wrench", "#6b7280"),
            new SeedCategory("Transporte", "car", "#0ea5e9"),
            new SeedCategory("Viagem", "plane", "#14b8a6"),
            new SeedCategory("Salário", "briefcase", "#22c55e"),
            new SeedCategory("Investimentos", "hand-coins", "#6366f1"));

    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final DateProvider dateProvider;

    @Override
    @Transactional
    public void run(String... args) {
        var referenceMonth = dateProvider.currentReferenceMonth();
        var createdCategories = seedCategories(referenceMonth);
        var createdAccounts = seedPixAccount(referenceMonth);
        log.info("Seed completed: {} categories, {} accounts created", createdCategories, createdAccounts);
    }

    private int seedCategories(LocalDate referenceMonth) {
        var createdCategories = 0;
        for (SeedCategory seedCategory : DEFAULT_CATEGORIES) {
            var existingCategory =
                    categoryRepository.findByNormalizedName(seedCategory.name()).orElse(null);
            if (existingCategory != null) {
                boolean shouldSave = false;

                if (isBlank(existingCategory.getIcon())) {
                    existingCategory.setIcon(seedCategory.iconId());
                    shouldSave = true;
                }

                if (isBlank(existingCategory.getColor())) {
                    existingCategory.setColor(seedCategory.color());
                    shouldSave = true;
                }

                if (shouldSave) {
                    categoryRepository.save(existingCategory);
                }
                continue;
            }

            var category = new Category();
            category.setName(seedCategory.name());
            category.setIcon(seedCategory.iconId());
            category.setColor(seedCategory.color());
            category.setCreatedInMonth(referenceMonth);
            categoryRepository.save(category);
            createdCategories++;
        }
        return createdCategories;
    }

    private int seedPixAccount(LocalDate referenceMonth) {
        if (accountRepository.findByNormalizedName(PIX_ACCOUNT_NAME).isPresent()) {
            return 0;
        }

        var account = new Account();
        account.setName(PIX_ACCOUNT_NAME);
        account.setType(AccountType.CHECKING);
        account.setBrand(null);
        account.setColor(null);
        account.setClosingDay(null);
        account.setDueDay(null);
        account.setCreatedInMonth(referenceMonth);
        accountRepository.save(account);
        return 1;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SeedCategory(String name, String iconId, String color) {}
}
