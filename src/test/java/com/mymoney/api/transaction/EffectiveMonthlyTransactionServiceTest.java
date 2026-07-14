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

    @BeforeEach
    void setUp() {
        currentMonth = YearMonth.now().atDay(1);
        nextMonth = currentMonth.plusMonths(1);
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
        Category category =
                Category.builder().id(UUID.randomUUID()).name("Test Category").build();

        Account account = Account.builder()
                .id(UUID.randomUUID())
                .name("Test Account")
                .currency(CurrencyType.BRL)
                .build();

        return FixedExpenseTemplate.builder()
                .id(UUID.randomUUID())
                .name("Test Template")
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00"))
                .currency(CurrencyType.BRL)
                .dueDay((short) 15)
                .createdInMonth(currentMonth)
                .category(category)
                .account(account)
                .build();
    }
}
