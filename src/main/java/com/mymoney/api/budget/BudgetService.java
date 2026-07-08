package com.mymoney.api.budget;

import com.mymoney.api.budget.api.request.CreateBudgetRequest;
import com.mymoney.api.budget.api.request.UpdateBudgetRequest;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryService;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.transaction.OwnershipType;
import com.mymoney.api.transaction.Transaction;
import com.mymoney.api.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetModelRepository budgetModelRepository;
    private final CategoryService categoryService;
    private final FamilyMemberRepository familyMemberRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public Page<BudgetView> listForMonth(
            LocalDate referenceMonth, String search, BudgetListStatus status, BudgetType type, Pageable pageable) {
        Page<UUID> idPage = budgetModelRepository.findIdsForMonth(
                referenceMonth, normalizeSearch(search), status.name(), type, pageable);
        List<BudgetView> views = loadByIds(idPage.getContent()).stream()
                .map(budget -> toView(budget, referenceMonth))
                .toList();
        return new PageImpl<>(views, pageable, idPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<BudgetView> listForMonth(LocalDate referenceMonth) {
        return loadByIds(budgetModelRepository
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
    public BudgetModel getById(UUID id) {
        return budgetModelRepository
                .findWithAssociationsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget was not found."));
    }

    @Transactional
    public BudgetModel create(CreateBudgetRequest request) {
        BudgetModel budget = new BudgetModel();
        apply(
                budget,
                request.name(),
                request.type(),
                request.ownerMemberId(),
                request.categoryIds(),
                request.monthlyLimit());
        budget.setCreatedInMonth(currentReferenceMonth());
        budget.setActive(true);
        return budgetModelRepository.save(budget);
    }

    @Transactional
    public BudgetModel update(UUID id, UpdateBudgetRequest request) {
        BudgetModel budget = getById(id);
        apply(
                budget,
                request.name(),
                request.type(),
                request.ownerMemberId(),
                request.categoryIds(),
                request.monthlyLimit());
        return budgetModelRepository.save(budget);
    }

    @Transactional
    public BudgetModel archive(UUID id, LocalDate referenceMonth) {
        BudgetModel budget = getById(id);
        if (referenceMonth.isBefore(budget.getCreatedInMonth())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Archive month cannot be before the budget creation month.");
        }
        budget.setArchivedFromMonth(referenceMonth);
        budget.setActive(false);
        return budgetModelRepository.save(budget);
    }

    @Transactional(readOnly = true)
    public List<Transaction> listTransactions(UUID id, LocalDate referenceMonth) {
        return filterTransactions(getById(id), referenceMonth);
    }

    @Transactional(readOnly = true)
    public List<BudgetCategoryBreakdownItem> categoryBreakdown(UUID id, LocalDate referenceMonth) {
        List<Transaction> transactions = filterTransactions(getById(id), referenceMonth);
        return transactions.stream()
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getCategory().getId(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)))
                .entrySet()
                .stream()
                .map(entry -> {
                    Transaction firstMatch = transactions.stream()
                            .filter(transaction ->
                                    transaction.getCategory().getId().equals(entry.getKey()))
                            .findFirst()
                            .orElseThrow();
                    return new BudgetCategoryBreakdownItem(
                            firstMatch.getCategory().getId().toString(),
                            firstMatch.getCategory().getName(),
                            entry.getValue());
                })
                .sorted(Comparator.comparing(BudgetCategoryBreakdownItem::categoryName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<BudgetModel> loadByIds(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<UUID, BudgetModel> budgetsById = budgetModelRepository.findAllWithAssociationsByIdIn(ids).stream()
                .collect(Collectors.toMap(
                        BudgetModel::getId, budget -> budget, (left, right) -> left, LinkedHashMap::new));

        return ids.stream().map(budgetsById::get).toList();
    }

    private BudgetView toView(BudgetModel budget, LocalDate referenceMonth) {
        BigDecimal consumedAmount = filterTransactions(budget, referenceMonth).stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BudgetView(budget, consumedAmount, budget.getMonthlyLimit().subtract(consumedAmount));
    }

    private List<Transaction> filterTransactions(BudgetModel budget, LocalDate referenceMonth) {
        List<Transaction> monthlyTransactions =
                transactionRepository.findByFilters(referenceMonth, null, null, null, null, null);

        if (budget.getType() == BudgetType.ALLOWANCE) {
            UUID ownerId = budget.getOwnerMember() == null
                    ? null
                    : budget.getOwnerMember().getId();
            return monthlyTransactions.stream()
                    .filter(transaction -> transaction.getOwnershipType() == OwnershipType.INDIVIDUAL)
                    .filter(transaction -> transaction.getMember() != null)
                    .filter(transaction -> transaction.getMember().getId().equals(ownerId))
                    .toList();
        }

        Set<UUID> categoryIds =
                budget.getCategories().stream().map(Category::getId).collect(Collectors.toSet());
        return monthlyTransactions.stream()
                .filter(transaction -> transaction.getOwnershipType() == OwnershipType.SHARED)
                .filter(transaction ->
                        categoryIds.contains(transaction.getCategory().getId()))
                .toList();
    }

    private void apply(
            BudgetModel budget,
            String name,
            BudgetType type,
            UUID ownerMemberId,
            List<UUID> categoryIds,
            BigDecimal monthlyLimit) {
        budget.setName(name.trim());
        budget.setType(type);
        budget.setMonthlyLimit(monthlyLimit);

        if (type == BudgetType.ALLOWANCE) {
            if (ownerMemberId == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "Allowance budgets require an owner member.");
            }
            FamilyMember owner = familyMemberRepository
                    .findById(ownerMemberId)
                    .filter(FamilyMember::isActive)
                    .orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Family member was not found."));
            if (!owner.isAllowanceEnabled()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "Allowance budgets require a member with allowance enabled.");
            }
            if (budget.getId() == null
                    && budgetModelRepository.existsByOwnerMemberIdAndType(owner.getId(), BudgetType.ALLOWANCE)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "An allowance budget already exists for this member.");
            }
            budget.setOwnerMember(owner);
            budget.setCategories(new LinkedHashSet<>());
            return;
        }

        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Global budgets require at least one category.");
        }

        budget.setOwnerMember(null);
        budget.setCategories(categoryIds.stream()
                .map(categoryService::getById)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
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
