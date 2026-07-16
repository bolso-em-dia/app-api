package com.mymoney.api.budget;

import com.mymoney.api.audit.AuditorResolver;
import com.mymoney.api.budget.api.request.CreateBudgetRequest;
import com.mymoney.api.budget.api.request.UpdateBudgetRequest;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryService;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.shared.DateProvider;
import com.mymoney.api.shared.EntityResolver;
import com.mymoney.api.shared.ErrorMessage;
import com.mymoney.api.shared.InputNormalizer;
import com.mymoney.api.transaction.OwnershipType;
import com.mymoney.api.transaction.TransactionCategoryAnalyzer;
import com.mymoney.api.transaction.TransactionService;
import com.mymoney.api.transaction.api.response.TransactionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;
    private final FamilyMemberRepository familyMemberRepository;
    private final TransactionService transactionService;
    private final TransactionCategoryAnalyzer transactionCategoryAnalyzer;
    private final AuditorResolver auditorResolver;
    private final DateProvider dateProvider;

    @Transactional(readOnly = true)
    public Page<BudgetView> listForMonth(
            LocalDate referenceMonth, String search, BudgetListStatus status, BudgetType type, Pageable pageable) {
        Page<UUID> idPage = budgetRepository.findIdsForMonth(
                referenceMonth, InputNormalizer.normalizeSearch(search), status.name(), type, pageable);
        List<BudgetView> views = loadByIds(idPage.getContent()).stream()
                .map(budget -> toView(budget, referenceMonth))
                .toList();
        return new PageImpl<>(views, pageable, idPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<BudgetView> listForMonth(LocalDate referenceMonth) {
        return loadByIds(budgetRepository
                        .findIdsForMonth(referenceMonth, "", BudgetListStatus.ACTIVE.name(), null, Pageable.unpaged())
                        .getContent())
                .stream()
                .map(budget -> toView(budget, referenceMonth))
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetView getViewById(UUID id, LocalDate referenceMonth) {
        return toView(getById(id), referenceMonth);
    }

    @Transactional(readOnly = true)
    public Budget getById(UUID id) {
        return budgetRepository
                .findWithAssociationsById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessage.BUDGET_NOT_FOUND.message()));
    }

    @Transactional
    public Budget create(CreateBudgetRequest request) {
        var budget = new Budget();
        apply(
                budget,
                request.name(),
                request.type(),
                request.ownerMemberId(),
                request.categoryIds(),
                request.monthlyLimit());
        budget.setCreatedInMonth(dateProvider.currentReferenceMonth());
        budget.setActive(true);
        var saved = budgetRepository.save(budget);
        log.info(
                "Budget created: id={}, name={}, type={}, ownerMemberId={}, memberId={}",
                saved.getId(),
                saved.getName(),
                saved.getType(),
                saved.getOwnerMember() != null ? saved.getOwnerMember().getId() : null,
                auditorResolver.resolveMemberId());
        return saved;
    }

    @Transactional
    public Budget update(UUID id, UpdateBudgetRequest request) {
        var budget = getById(id);
        apply(
                budget,
                request.name(),
                request.type(),
                request.ownerMemberId(),
                request.categoryIds(),
                request.monthlyLimit());
        var saved = budgetRepository.save(budget);
        log.info(
                "Budget updated: id={}, name={}, type={}, ownerMemberId={}, memberId={}",
                saved.getId(),
                saved.getName(),
                saved.getType(),
                saved.getOwnerMember() != null ? saved.getOwnerMember().getId() : null,
                auditorResolver.resolveMemberId());
        return saved;
    }

    @Transactional
    public Budget archive(UUID id, LocalDate referenceMonth) {
        var budget = getById(id);
        if (referenceMonth.isBefore(budget.getCreatedInMonth())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Archive month cannot be before the budget creation month.");
        }
        budget.setArchivedFromMonth(referenceMonth);
        budget.setActive(false);
        var saved = budgetRepository.save(budget);
        log.info(
                "Budget archived: id={}, archivedFromMonth={}, memberId={}",
                saved.getId(),
                saved.getArchivedFromMonth(),
                auditorResolver.resolveMemberId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listTransactions(UUID id, LocalDate referenceMonth) {
        return filterTransactions(getById(id), referenceMonth);
    }

    @Transactional(readOnly = true)
    public List<BudgetCategoryBreakdownItem> categoryBreakdown(UUID id, LocalDate referenceMonth) {
        var transactions = filterTransactions(getById(id), referenceMonth);
        return transactionCategoryAnalyzer
                .analyzeByCategory(
                        transactions,
                        TransactionResponse::categoryId,
                        TransactionResponse::categoryName,
                        TransactionResponse::convertedAmount,
                        Comparator.comparing(
                                TransactionCategoryAnalyzer.CategoryAmount::categoryName,
                                String.CASE_INSENSITIVE_ORDER))
                .stream()
                .map(item -> new BudgetCategoryBreakdownItem(item.categoryId(), item.categoryName(), item.amount()))
                .toList();
    }

    private List<Budget> loadByIds(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<UUID, Budget> budgetsById = budgetRepository.findAllWithAssociationsByIdIn(ids).stream()
                .collect(Collectors.toMap(Budget::getId, budget -> budget, (left, right) -> left, LinkedHashMap::new));

        return ids.stream().map(budgetsById::get).toList();
    }

    private BudgetView toView(Budget budget, LocalDate referenceMonth) {
        BigDecimal consumedAmount = filterTransactions(budget, referenceMonth).stream()
                .map(TransactionResponse::convertedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BudgetView(budget, consumedAmount, budget.getMonthlyLimit().subtract(consumedAmount));
    }

    private List<TransactionResponse> filterTransactions(Budget budget, LocalDate referenceMonth) {
        if (budget.getType() == BudgetType.ALLOWANCE) {
            var ownerId = budget.getOwnerMember() == null
                    ? null
                    : budget.getOwnerMember().getId();
            return transactionService.listResponsesByFilters(
                    referenceMonth,
                    null, // type — budget não filtra income/expense
                    OwnershipType.INDIVIDUAL,
                    null, // accountId
                    null, // categoryIds
                    ownerId);
        }

        // GLOBAL budget — filtra por ownershipType e categoryIds no banco
        List<UUID> categoryIds =
                budget.getCategories().stream().map(Category::getId).toList();
        return transactionService.listResponsesByFilters(
                referenceMonth,
                null, // type
                OwnershipType.SHARED,
                null, // accountId
                categoryIds,
                null); // memberId
    }

    private void apply(
            Budget budget,
            String name,
            BudgetType type,
            UUID ownerMemberId,
            List<UUID> categoryIds,
            BigDecimal monthlyLimit) {
        budget.setName(InputNormalizer.requireNonBlank(name, "Name"));
        budget.setType(type);
        budget.setMonthlyLimit(monthlyLimit);

        if (type == BudgetType.ALLOWANCE) {
            applyAllowanceBudget(budget, ownerMemberId);
            return;
        }

        applyGlobalBudget(budget, categoryIds);
    }

    private void applyAllowanceBudget(Budget budget, UUID ownerMemberId) {
        if (ownerMemberId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Allowance budgets require an owner member.");
        }
        FamilyMember owner = EntityResolver.resolveOrThrow(
                () -> familyMemberRepository.findById(ownerMemberId).filter(FamilyMember::isActive),
                ErrorMessage.FAMILY_MEMBER_NOT_FOUND.message());
        if (budgetRepository.existsAnotherByOwnerMemberIdAndType(owner.getId(), BudgetType.ALLOWANCE, budget.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An allowance budget already exists for this member.");
        }
        budget.setOwnerMember(owner);
        budget.setCategories(new LinkedHashSet<>());
    }

    private void applyGlobalBudget(Budget budget, List<UUID> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Global budgets require at least one category.");
        }

        budget.setOwnerMember(null);
        budget.setCategories(categoryIds.stream()
                .map(categoryService::getById)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }
}
