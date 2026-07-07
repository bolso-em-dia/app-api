package com.mymoney.api.transaction;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountService;
import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryService;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.transaction.api.request.CreateTransactionRequest;
import com.mymoney.api.transaction.api.request.UpdateTransactionRequest;
import com.mymoney.api.transaction.api.response.TransactionResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final int DEFAULT_DESCRIPTION_SUGGESTION_LIMIT = 8;
    private static final int MAX_DESCRIPTION_SUGGESTION_LIMIT = 12;

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final AccountService accountService;
    private final FamilyMemberRepository familyMemberRepository;

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listResponseByFilters(
            LocalDate referenceMonth,
            TransactionType type,
            OwnershipType ownershipType,
            UUID accountId,
            List<UUID> categoryIds,
            UUID memberId,
            Pageable pageable) {
        return transactionRepository.findResponseByFilters(
                referenceMonth, type, ownershipType, accountId, categoryIds, memberId, pageable);
    }

    @Transactional(readOnly = true)
    public List<String> listDescriptionSuggestions(String query, Integer limit) {
        String normalizedQuery = query == null ? "" : query.trim();
        int normalizedLimit;
        if (limit == null) {
            normalizedLimit = DEFAULT_DESCRIPTION_SUGGESTION_LIMIT;
        } else {
            normalizedLimit = Math.max(1, Math.min(limit, MAX_DESCRIPTION_SUGGESTION_LIMIT));
        }
        return transactionRepository.findDescriptionSuggestions(normalizedQuery, PageRequest.of(0, normalizedLimit));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> listByFilters(
            LocalDate referenceMonth,
            TransactionType type,
            OwnershipType ownershipType,
            UUID accountId,
            List<UUID> categoryIds,
            UUID memberId,
            Pageable pageable) {
        return transactionRepository.findByFilters(
                referenceMonth, type, ownershipType, accountId, categoryIds, memberId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Transaction> listByFilters(
            LocalDate referenceMonth,
            TransactionType type,
            OwnershipType ownershipType,
            UUID accountId,
            List<UUID> categoryIds,
            UUID memberId) {
        return transactionRepository.findByFilters(
                referenceMonth, type, ownershipType, accountId, categoryIds, memberId);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getResponseById(UUID id) {
        return transactionRepository
                .findResponseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction was not found."));
    }

    @Transactional(readOnly = true)
    public Transaction getById(UUID id) {
        return transactionRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction was not found."));
    }

    @Transactional
    public List<TransactionResponse> create(CreateTransactionRequest request) {
        validateInstallmentCount(request.installmentCount());
        Category category = categoryService.getById(request.categoryId());
        Account account = accountService.getById(request.accountId());
        FamilyMember member = resolveMember(request.ownershipType(), request.memberId());

        int installmentCount = request.installmentCount() == null ? 1 : request.installmentCount();
        UUID installmentGroupId = installmentCount > 1 ? UUID.randomUUID() : null;

        List<TransactionResponse> created = new ArrayList<>();
        for (int i = 0; i < installmentCount; i++) {
            LocalDate transactionDate = request.transactionDate().plusMonths(i);
            Transaction transaction = new Transaction();
            transaction.setType(request.type());
            transaction.setOwnershipType(request.ownershipType());
            transaction.setSourceType(
                    installmentCount > 1 ? TransactionSourceType.INSTALLMENT : TransactionSourceType.MANUAL);
            transaction.setDescription(request.description().trim());
            transaction.setAmount(request.amount());
            transaction.setTransactionDate(transactionDate);
            transaction.setReferenceMonth(referenceMonthFromDate(transactionDate));
            transaction.setAccount(account);
            transaction.setCategory(category);
            transaction.setMember(member);
            transaction.setInstallmentGroupId(installmentGroupId);
            transaction.setInstallmentNumber(installmentCount > 1 ? (short) (i + 1) : null);
            transaction.setInstallmentTotal(installmentCount > 1 ? (short) installmentCount : null);
            created.add(getResponseById(transactionRepository.save(transaction).getId()));
        }

        return created;
    }

    @Transactional
    public TransactionResponse update(UUID id, UpdateTransactionRequest request) {
        Transaction transaction = getById(id);
        Category category = categoryService.getById(request.categoryId());
        Account account = accountService.getById(request.accountId());
        FamilyMember member = resolveMember(request.ownershipType(), request.memberId());

        transaction.setType(request.type());
        transaction.setOwnershipType(request.ownershipType());
        transaction.setDescription(request.description().trim());
        transaction.setAmount(request.amount());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setReferenceMonth(referenceMonthFromDate(request.transactionDate()));
        transaction.setCategory(category);
        transaction.setAccount(account);
        transaction.setMember(member);

        return getResponseById(transactionRepository.save(transaction).getId());
    }

    @Transactional
    public void delete(UUID id, DeleteScope scope) {
        Transaction transaction = getById(id);
        if (transaction.getInstallmentGroupId() == null || scope == DeleteScope.SINGLE) {
            transactionRepository.delete(transaction);
            return;
        }

        if (scope == DeleteScope.FUTURE) {
            transactionRepository.deleteByInstallmentGroupIdAndInstallmentNumberGreaterThanEqual(
                    transaction.getInstallmentGroupId(), transaction.getInstallmentNumber());
            return;
        }

        transactionRepository.deleteByInstallmentGroupId(transaction.getInstallmentGroupId());
    }

    private FamilyMember resolveMember(OwnershipType ownershipType, UUID memberId) {
        if (ownershipType == OwnershipType.SHARED) {
            return null;
        }

        if (memberId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Individual transactions require a member with allowance enabled.");
        }

        FamilyMember member = familyMemberRepository
                .findById(memberId)
                .filter(FamilyMember::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Family member was not found."));

        if (!member.isAllowanceEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Individual transactions require a member with allowance enabled.");
        }

        return member;
    }

    private void validateInstallmentCount(Integer installmentCount) {
        if (installmentCount == null) {
            return;
        }
        if (installmentCount < 1 || installmentCount > 120) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Installment count must be between 1 and 120.");
        }
    }

    private LocalDate referenceMonthFromDate(LocalDate transactionDate) {
        return YearMonth.from(transactionDate).atDay(1);
    }
}
