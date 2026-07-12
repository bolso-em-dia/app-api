package com.mymoney.api.fixedexpense;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountService;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryService;
import com.mymoney.api.fixedexpense.api.request.CreateFixedExpenseTemplateRequest;
import com.mymoney.api.fixedexpense.api.request.UpdateFixedExpenseTemplateRequest;
import com.mymoney.api.fixedexpense.api.response.FixedExpenseTemplateResponse;
import com.mymoney.api.shared.DateProvider;
import com.mymoney.api.shared.DayValidator;
import com.mymoney.api.shared.EntityResolver;
import com.mymoney.api.shared.ErrorMessage;
import com.mymoney.api.shared.InputNormalizer;
import com.mymoney.api.transaction.CurrencyConversionService;
import com.mymoney.api.transaction.EffectiveMonthlyTransactionService;
import com.mymoney.api.transaction.TransactionRepository;
import com.mymoney.api.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FixedExpenseTemplateService {

    private final FixedExpenseTemplateRepository fixedExpenseTemplateRepository;
    private final CategoryService categoryService;
    private final AccountService accountService;
    private final EffectiveMonthlyTransactionService effectiveMonthlyTransactionService;
    private final TransactionRepository transactionRepository;
    private final CurrencyConversionService currencyConversionService;
    private final DateProvider dateProvider;

    @Transactional(readOnly = true)
    public Page<FixedExpenseTemplateResponse> listAllResponses(
            String search, FixedExpenseTemplateListStatus status, Pageable pageable) {
        return fixedExpenseTemplateRepository.findResponseByFilters(normalizeSearch(search), status.name(), pageable);
    }

    @Transactional(readOnly = true)
    public FixedExpenseTemplateResponse getResponseById(UUID id) {
        return fixedExpenseTemplateRepository
                .findResponseById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorMessage.FIXED_EXPENSE_TEMPLATE_NOT_FOUND.message()));
    }

    @Transactional(readOnly = true)
    public FixedExpenseTemplate getById(UUID id) {
        return EntityResolver.resolveOrThrow(
                () -> fixedExpenseTemplateRepository.findById(id),
                ErrorMessage.FIXED_EXPENSE_TEMPLATE_NOT_FOUND.message());
    }

    @Transactional
    public FixedExpenseTemplateResponse create(CreateFixedExpenseTemplateRequest request) {
        FixedExpenseTemplate template = new FixedExpenseTemplate();
        apply(
                template,
                request.name(),
                request.type(),
                request.amount(),
                request.categoryId(),
                request.accountId(),
                request.dueDay());
        template.setCreatedInMonth(dateProvider.currentReferenceMonth());
        template.setActive(true);
        FixedExpenseTemplate saved = fixedExpenseTemplateRepository.save(template);
        effectiveMonthlyTransactionService.syncCurrentMonthTransaction(saved);
        return getResponseById(saved.getId());
    }

    @Transactional
    public FixedExpenseTemplateResponse update(UUID id, UpdateFixedExpenseTemplateRequest request) {
        FixedExpenseTemplate template = getById(id);
        apply(
                template,
                request.name(),
                request.type(),
                request.amount(),
                request.categoryId(),
                request.accountId(),
                request.dueDay());
        FixedExpenseTemplate saved = fixedExpenseTemplateRepository.save(template);
        effectiveMonthlyTransactionService.syncCurrentMonthTransaction(saved);
        return getResponseById(saved.getId());
    }

    @Transactional
    public void delete(UUID id) {
        FixedExpenseTemplate template = getById(id);
        LocalDate currentMonth = dateProvider.currentReferenceMonth();
        transactionRepository.detachFixedExpenseTemplateBeforeMonth(template, currentMonth);
        transactionRepository.deleteByFixedExpenseTemplateIdAndReferenceMonthGreaterThanEqual(
                template.getId(), currentMonth);
        fixedExpenseTemplateRepository.delete(template);
    }

    private void apply(
            FixedExpenseTemplate template,
            String name,
            TransactionType type,
            BigDecimal amount,
            UUID categoryId,
            UUID accountId,
            Integer dueDay) {
        DayValidator.validateDayRange(dueDay, "Due day");
        Category category = categoryService.getById(categoryId);
        Account account = accountService.getById(accountId);
        CurrencyConversionService.ConvertedAmount converted =
                currencyConversionService.convert(amount, account.getCurrency(), false);

        template.setName(InputNormalizer.requireNonBlank(name, "Name"));
        template.setType(type);
        template.setAmount(amount);
        template.setCategory(category);
        template.setAccount(account);
        template.setDueDay(dueDay.shortValue());
        template.setCurrency(account.getCurrency());
        template.setConvertedAmount(converted.convertedAmount());
        template.setExchangeRate(converted.exchangeRate());
    }

    private String normalizeSearch(String value) {
        return InputNormalizer.normalizeSearch(value);
    }
}
