package com.mymoney.api.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.account.AccountType;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryRepository;
import com.mymoney.api.shared.DateProvider;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BasicReferenceDataSeedRunnerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DateProvider dateProvider;

    private BasicReferenceDataSeedRunner basicReferenceDataSeedRunner;

    @BeforeEach
    void setUp() {
        when(dateProvider.currentReferenceMonth()).thenReturn(LocalDate.of(2026, 7, 1));
        basicReferenceDataSeedRunner =
                new BasicReferenceDataSeedRunner(categoryRepository, accountRepository, dateProvider);
    }

    @Test
    void seedsExpectedCategoriesAndPixAccountWhenMissing() throws Exception {
        when(categoryRepository.findByNormalizedName(any())).thenReturn(Optional.empty());
        when(accountRepository.findByNormalizedName("Pix")).thenReturn(Optional.empty());

        basicReferenceDataSeedRunner.run();

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository, org.mockito.Mockito.times(14)).save(categoryCaptor.capture());

        List<Category> savedCategories = categoryCaptor.getAllValues();
        assertThat(savedCategories)
                .extracting(Category::getName)
                .containsExactly(
                        "Compras",
                        "Cuidados Pessoais",
                        "Educação",
                        "Empréstimos",
                        "Farmácia",
                        "Lazer",
                        "Mercado",
                        "Pets",
                        "Saúde",
                        "Serviços",
                        "Transporte",
                        "Viagem",
                        "Salário",
                        "Investimentos");
        assertThat(savedCategories)
                .extracting(Category::getIcon)
                .containsExactly(
                        "shopping-cart",
                        "sparkles",
                        "book",
                        "hand-coins",
                        "pill",
                        "gamepad",
                        "shopping-basket",
                        "paw-print",
                        "heart",
                        "wrench",
                        "car",
                        "plane",
                        "briefcase",
                        "hand-coins");
        assertThat(savedCategories)
                .extracting(Category::getColor)
                .containsExactly(
                        "#3b82f6", "#ec4899", "#8b5cf6", "#f97316", "#10b981", "#a855f7", "#84cc16", "#f59e0b",
                        "#ef4444", "#6b7280", "#0ea5e9", "#14b8a6", "#22c55e", "#6366f1");
        assertThat(savedCategories).allSatisfy(category -> {
            assertThat(category.getColor()).isNotBlank();
            assertThat(category.getArchivedFromMonth()).isNull();
            assertThat(category.getCreatedInMonth()).isNotNull();
        });

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getName()).isEqualTo("Pix");
        assertThat(savedAccount.getType()).isEqualTo(AccountType.CHECKING);
        assertThat(savedAccount.getBrand()).isNull();
        assertThat(savedAccount.getColor()).isNull();
        assertThat(savedAccount.getClosingDay()).isNull();
        assertThat(savedAccount.getDueDay()).isNull();
        assertThat(savedAccount.getCreatedInMonth()).isNotNull();
        assertThat(savedAccount.getArchivedFromMonth()).isNull();
    }

    @Test
    void doesNotCreateDuplicatesWhenRecordsAlreadyExist() throws Exception {
        Category existingCategory = new Category();
        existingCategory.setIcon("shopping-cart");
        existingCategory.setColor("#3b82f6");

        when(categoryRepository.findByNormalizedName(any())).thenReturn(Optional.of(existingCategory));
        when(accountRepository.findByNormalizedName(eq("Pix"))).thenReturn(Optional.of(new Account()));

        basicReferenceDataSeedRunner.run();

        verify(categoryRepository, never()).save(any(Category.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void backfillsMissingSeedCategoryColorWithoutCreatingDuplicate() throws Exception {
        Category existingCategory = new Category();
        existingCategory.setName("Compras");
        existingCategory.setIcon("shopping-cart");
        existingCategory.setColor(null);

        when(categoryRepository.findByNormalizedName(any())).thenReturn(Optional.of(existingCategory));
        when(accountRepository.findByNormalizedName(eq("Pix"))).thenReturn(Optional.of(new Account()));

        basicReferenceDataSeedRunner.run();

        verify(categoryRepository).save(existingCategory);
        assertThat(existingCategory.getColor()).isEqualTo("#3b82f6");
    }
}
