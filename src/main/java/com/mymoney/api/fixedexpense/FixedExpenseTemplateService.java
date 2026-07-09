package com.mymoney.api.fixedexpense;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountService;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryService;
import com.mymoney.api.fixedexpense.api.request.CreateFixedExpenseTemplateRequest;
import com.mymoney.api.fixedexpense.api.request.UpdateFixedExpenseTemplateRequest;
import com.mymoney.api.fixedexpense.api.response.FixedExpenseTemplateResponse;
import com.mymoney.api.transaction.EffectiveMonthlyTransactionService;
import com.mymoney.api.transaction.TransactionRepository;
import com.mymoney.api.transaction.TransactionType;
import java.time.LocalDate;
import java.time.YearMonth;
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

    @Transactional(readOnly = true)
    public Page<FixedExpenseTemplateResponse> listAllResponses(
            String search, FixedExpenseTemplateListStatus status, Pageable pageable) {
        return fixedExpenseTemplateRepository.findResponseByFilters(normalizeSearch(search), status.name(), pageable);
    }

    @Transactional(readOnly = true)
    public FixedExpenseTemplateResponse getResponseById(UUID id) {
        return fixedExpenseTemplateRepository
                .findResponseById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Fixed expense template was not found."));
    }

    @Transactional(readOnly = true)
    public FixedExpenseTemplate getById(UUID id) {
        return fixedExpenseTemplateRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Fixed expense template was not found."));
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
        template.setCreatedInMonth(currentReferenceMonth());
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
    public FixedExpenseTemplateResponse archive(UUID id) {
        FixedExpenseTemplate template = getById(id);
        LocalDate archivedFromMonth = currentReferenceMonth();
        if (archivedFromMonth.isBefore(template.getCreatedInMonth())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Archive month cannot be before the fixed expense template creation month.");
        }
        template.setArchivedFromMonth(archivedFromMonth);
        template.setActive(false);
        FixedExpenseTemplate saved = fixedExpenseTemplateRepository.save(template);
        return getResponseById(saved.getId());
    }

    @Transactional
    public void delete(UUID id) {
        FixedExpenseTemplate template = getById(id);
        LocalDate currentMonth = currentReferenceMonth();
        transactionRepository.deleteByFixedExpenseTemplateIdAndReferenceMonthGreaterThanEqual(
                template.getId(), currentMonth);
        fixedExpenseTemplateRepository.delete(template);
    }

    private void apply(
            FixedExpenseTemplate template,
            String name,
            TransactionType type,
            java.math.BigDecimal amount,
            UUID categoryId,
            UUID accountId,
            Integer dueDay) {
        validateDueDay(dueDay);
        Category category = categoryService.getById(categoryId);
        Account account = accountService.getById(accountId);

        template.setName(name.trim());
        template.setType(type);
        template.setAmount(amount);
        template.setCategory(category);
        template.setAccount(account);
        template.setDueDay(dueDay.shortValue());
    }

    private void validateDueDay(Integer dueDay) {
        if (dueDay == null || dueDay < 1 || dueDay > 31) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Due day must be between 1 and 31.");
        }
    }

    private LocalDate currentReferenceMonth() {
        return YearMonth.now().atDay(1);
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
