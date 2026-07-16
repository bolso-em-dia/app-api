package com.mymoney.api.fixedexpense;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountService;
import com.mymoney.api.audit.AuditorResolver;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FixedExpenseTemplateService {

    private final FixedExpenseTemplateRepository fixedExpenseTemplateRepository;
    private final CategoryService categoryService;
    private final AccountService accountService;
    private final EffectiveMonthlyTransactionService effectiveMonthlyTransactionService;
    private final TransactionRepository transactionRepository;
    private final CurrencyConversionService currencyConversionService;
    private final AuditorResolver auditorResolver;
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
        var template = new FixedExpenseTemplate();
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
        var saved = fixedExpenseTemplateRepository.save(template);
        effectiveMonthlyTransactionService.syncCurrentMonthTransaction(saved);
        log.info(
                "Fixed expense template created: id={}, name={}, type={}, memberId={}",
                saved.getId(),
                saved.getName(),
                saved.getType(),
                auditorResolver.resolveMemberId());
        return getResponseById(saved.getId());
    }

    @Transactional
    public FixedExpenseTemplateResponse update(UUID id, UpdateFixedExpenseTemplateRequest request) {
        var template = getById(id);
        apply(
                template,
                request.name(),
                request.type(),
                request.amount(),
                request.categoryId(),
                request.accountId(),
                request.dueDay());
        var saved = fixedExpenseTemplateRepository.save(template);
        effectiveMonthlyTransactionService.syncCurrentMonthTransaction(saved);
        log.info(
                "Fixed expense template updated: id={}, name={}, type={}, memberId={}",
                saved.getId(),
                saved.getName(),
                saved.getType(),
                auditorResolver.resolveMemberId());
        return getResponseById(saved.getId());
    }

    @Transactional
    public void delete(UUID id) {
        var template = getById(id);
        var currentMonth = dateProvider.currentReferenceMonth();
        transactionRepository.detachFixedExpenseTemplateBeforeMonth(template, currentMonth);
        transactionRepository.deleteByFixedExpenseTemplateIdAndReferenceMonthGreaterThanEqual(
                template.getId(), currentMonth);
        fixedExpenseTemplateRepository.delete(template);
        log.info(
                "Fixed expense template deleted: id={}, currentMonth={}, memberId={}",
                template.getId(),
                currentMonth,
                auditorResolver.resolveMemberId());
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
