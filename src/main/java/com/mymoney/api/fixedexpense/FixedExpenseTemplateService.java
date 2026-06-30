package com.mymoney.api.fixedexpense;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountService;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryService;
import com.mymoney.api.fixedexpense.api.request.ArchiveFixedExpenseTemplateRequest;
import com.mymoney.api.fixedexpense.api.request.CreateFixedExpenseTemplateRequest;
import com.mymoney.api.fixedexpense.api.request.UpdateFixedExpenseTemplateRequest;
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

    @Transactional(readOnly = true)
    public Page<FixedExpenseTemplate> listAll(String search, FixedExpenseTemplateListStatus status, Pageable pageable) {
        return fixedExpenseTemplateRepository.findByFilters(normalizeSearch(search), status.name(), pageable);
    }

    @Transactional(readOnly = true)
    public FixedExpenseTemplate getById(UUID id) {
        return fixedExpenseTemplateRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Fixed expense template was not found."));
    }

    @Transactional
    public FixedExpenseTemplate create(CreateFixedExpenseTemplateRequest request) {
        FixedExpenseTemplate template = new FixedExpenseTemplate();
        apply(template, request.name(), request.amount(), request.categoryId(), request.accountId(), request.dueDay());
        template.setCreatedInMonth(currentReferenceMonth());
        template.setActive(true);
        return fixedExpenseTemplateRepository.save(template);
    }

    @Transactional
    public FixedExpenseTemplate update(UUID id, UpdateFixedExpenseTemplateRequest request) {
        FixedExpenseTemplate template = getById(id);
        apply(template, request.name(), request.amount(), request.categoryId(), request.accountId(), request.dueDay());
        return fixedExpenseTemplateRepository.save(template);
    }

    @Transactional
    public FixedExpenseTemplate archive(UUID id, ArchiveFixedExpenseTemplateRequest request) {
        FixedExpenseTemplate template = getById(id);
        if (request.archivedFromMonth().isBefore(template.getCreatedInMonth())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Archive month cannot be before the fixed expense template creation month.");
        }
        template.setArchivedFromMonth(request.archivedFromMonth());
        template.setActive(false);
        return fixedExpenseTemplateRepository.save(template);
    }

    private void apply(
            FixedExpenseTemplate template,
            String name,
            java.math.BigDecimal amount,
            UUID categoryId,
            UUID accountId,
            Integer dueDay) {
        validateDueDay(dueDay);
        Category category = categoryService.getById(categoryId);
        Account account = accountService.getById(accountId);

        template.setName(name.trim());
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
