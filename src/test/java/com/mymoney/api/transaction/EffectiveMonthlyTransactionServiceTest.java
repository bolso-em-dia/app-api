package com.mymoney.api.transaction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.CurrencyType;
import com.mymoney.api.category.Category;
import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateRepository;
import com.mymoney.api.shared.DateProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EffectiveMonthlyTransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FixedExpenseTemplateRepository fixedExpenseTemplateRepository;

    @Mock
    private CurrencyConversionService currencyConversionService;

    @Mock
    private DateProvider dateProvider;

    private EffectiveMonthlyTransactionService service;

    private LocalDate currentMonth;
    private LocalDate nextMonth;
    private LocalDate monthPlus4;

    @BeforeEach
    void setUp() {
        currentMonth = YearMonth.now().atDay(1);
        nextMonth = currentMonth.plusMonths(1);
        monthPlus4 = currentMonth.plusMonths(4);
        lenient().when(dateProvider.currentReferenceMonth()).thenReturn(currentMonth);
        lenient()
                .when(currencyConversionService.convert(any(), any(), eq(false)))
                .thenAnswer(invocation ->
                        new CurrencyConversionService.ConvertedAmount(invocation.getArgument(0), null, "BRL"));
        service = new EffectiveMonthlyTransactionService(
                transactionRepository, fixedExpenseTemplateRepository, currencyConversionService, dateProvider);
    }

    @Test
    void materializeMonth_CallsSaveAll_WhenTemplatesExist() {
        // Arrange
        FixedExpenseTemplate template = createTemplate();
        when(fixedExpenseTemplateRepository.findActiveNotMaterializedForMonth(eq(nextMonth)))
                .thenReturn(List.of(template));
        when(transactionRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        service.materializeMonth(nextMonth);

        // Assert
        verify(transactionRepository).saveAll(any(List.class));
    }

    @Test
    void materializeMonth_DoesNothing_WhenNoTemplates() {
        // Arrange
        when(fixedExpenseTemplateRepository.findActiveNotMaterializedForMonth(eq(nextMonth)))
                .thenReturn(new ArrayList<>());

        // Act
        service.materializeMonth(nextMonth);

        // Assert
        verify(transactionRepository, never()).saveAll(any(List.class));
    }

    @Test
    void listEffectiveTransactions_DoesNotCallMaterialize_Automatically() {
        // Arrange
        when(transactionRepository.findByFilters(
                        eq(nextMonth), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null)))
                .thenReturn(new ArrayList<>());

        // Act
        service.listEffectiveTransactions(nextMonth, null, null, null, null, null, null);

        // Assert
        verify(fixedExpenseTemplateRepository, never()).findActiveNotMaterializedForMonth(any());
    }

    private FixedExpenseTemplate createTemplate() {
        FixedExpenseTemplate template = new FixedExpenseTemplate();
        template.setId(UUID.randomUUID());
        template.setName("Test Template");
        template.setType(TransactionType.EXPENSE);
        template.setAmount(new BigDecimal("100.00"));
        template.setCurrency(CurrencyType.BRL);
        template.setDueDay((short) 15);
        template.setCreatedInMonth(currentMonth);
        template.setArchivedFromMonth(null);

        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Test Category");
        template.setCategory(category);

        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setName("Test Account");
        account.setCurrency(CurrencyType.BRL);
        template.setAccount(account);

        return template;
    }
}
