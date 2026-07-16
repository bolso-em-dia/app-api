package com.mymoney.api.transaction;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountService;
import com.mymoney.api.audit.AuditorResolver;
import com.mymoney.api.budget.BudgetRepository;
import com.mymoney.api.category.CategoryService;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.shared.DateProvider;
import com.mymoney.api.shared.EntityResolver;
import com.mymoney.api.shared.ErrorMessage;
import com.mymoney.api.shared.InputNormalizer;
import com.mymoney.api.transaction.api.request.CreateTransactionRequest;
import com.mymoney.api.transaction.api.request.UpdateTransactionRequest;
import com.mymoney.api.transaction.api.response.TransactionResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final int DEFAULT_DESCRIPTION_SUGGESTION_LIMIT = 8;
    private static final int MAX_DESCRIPTION_SUGGESTION_LIMIT = 12;
    private static final int MAX_INSTALLMENT_YEARS = 2;

    private final TransactionRepository transactionRepository;
    private final EffectiveMonthlyTransactionService effectiveMonthlyTransactionService;
    private final CategoryService categoryService;
    private final AccountService accountService;
    private final BudgetRepository budgetRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final CurrencyConversionService currencyConversionService;
    private final AuditorResolver auditorResolver;
    private final DateProvider dateProvider;
    private final com.mymoney.api.transaction.mapper.TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listResponseByFilters(
            LocalDate referenceMonth,
            TransactionType type,
            OwnershipType ownershipType,
            UUID accountId,
            List<UUID> categoryIds,
            UUID memberId,
            String search,
            Pageable pageable) {
        return transactionRepository.findResponseByFilters(
                referenceMonth,
                type,
                ownershipType,
                accountId,
                categoryIds,
                memberId,
                InputNormalizer.normalizeNullable(search),
                pageable);
    }

    @Transactional(readOnly = true)
    public List<String> listDescriptionSuggestions(String query, Integer limit) {
        String normalizedQuery = InputNormalizer.normalizeSearch(query);
        int normalizedLimit;
        if (limit == null) {
            normalizedLimit = DEFAULT_DESCRIPTION_SUGGESTION_LIMIT;
        } else {
            if (limit < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be positive.");
            }
            normalizedLimit = Math.max(1, Math.min(limit, MAX_DESCRIPTION_SUGGESTION_LIMIT));
        }
        LocalDate since = dateProvider.currentReferenceMonth().minusMonths(12);
        return transactionRepository.findDescriptionSuggestions(
                normalizedQuery, since, PageRequest.of(0, normalizedLimit));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listResponsesByFilters(
            LocalDate referenceMonth,
            TransactionType type,
            OwnershipType ownershipType,
            UUID accountId,
            List<UUID> categoryIds,
            UUID memberId) {
        return effectiveMonthlyTransactionService
                .listEffectiveTransactions(referenceMonth, type, ownershipType, accountId, categoryIds, memberId, null)
                .stream()
                .map(item -> transactionMapper.toResponse(item.transaction(), item.projected()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getResponseById(UUID id) {
        return transactionRepository
                .findResponseById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorMessage.TRANSACTION_NOT_FOUND.message()));
    }

    @Transactional
    public void materializeMonth(LocalDate referenceMonth) {
        effectiveMonthlyTransactionService.materializeMonth(referenceMonth);
    }

    @Transactional(readOnly = true)
    public Transaction getById(UUID id) {
        return transactionRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorMessage.TRANSACTION_NOT_FOUND.message()));
    }

    @Transactional
    public List<TransactionResponse> create(CreateTransactionRequest request) {
        validateInstallmentCount(request.installmentCount());
        var category = categoryService.getById(request.categoryId());
        var account = accountService.getById(request.accountId());
        var member = resolveMember(request.ownershipType(), request.memberId());

        var installmentCount = request.installmentCount() == null ? 1 : request.installmentCount();
        validateInstallmentHorizon(request.transactionDate(), installmentCount);
        var installmentGroupId = installmentCount > 1 ? UUID.randomUUID() : null;
        var installmentAmounts = calculateInstallmentAmounts(request.amount(), installmentCount);
        var actorMemberId = auditorResolver.resolveMemberId();

        var created = new ArrayList<TransactionResponse>();
        for (int i = 0; i < installmentCount; i++) {
            var transactionDate = request.transactionDate().plusMonths(i);
            var transaction = new Transaction();
            validateIndividualAllowance(request.ownershipType(), member, transactionDate);
            transaction.setType(request.type());
            transaction.setOwnershipType(request.ownershipType());
            transaction.setSourceType(
                    installmentCount > 1 ? TransactionSourceType.INSTALLMENT : TransactionSourceType.MANUAL);
            transaction.setDescription(InputNormalizer.requireNonBlank(request.description(), "Description"));
            BigDecimal rawAmount = installmentAmounts.get(i);
            transaction.setAmount(rawAmount);
            applyCurrency(transaction, account, rawAmount, true);
            transaction.setTransactionDate(transactionDate);
            transaction.setReferenceMonth(referenceMonthFromDate(transactionDate));
            transaction.setAccount(account);
            transaction.setCategory(category);
            transaction.setMember(member);
            transaction.setInstallmentGroupId(installmentGroupId);
            transaction.setInstallmentNumber(installmentCount > 1 ? (short) (i + 1) : null);
            transaction.setInstallmentTotal(installmentCount > 1 ? (short) installmentCount : null);
            var saved = transactionRepository.save(transaction);
            log.info(
                    "Transaction created: id={}, type={}, ownershipType={}, referenceMonth={}, memberId={}",
                    saved.getId(),
                    saved.getType(),
                    saved.getOwnershipType(),
                    saved.getReferenceMonth(),
                    actorMemberId);
            created.add(getResponseById(saved.getId()));
        }

        return created;
    }

    @Transactional
    public TransactionResponse update(UUID id, UpdateTransactionRequest request) {
        var transaction = getById(id);
        var category = categoryService.getById(request.categoryId());
        var account = accountService.getById(request.accountId());
        var member = resolveMember(request.ownershipType(), request.memberId());
        validateIndividualAllowance(request.ownershipType(), member, request.transactionDate());

        transaction.setType(request.type());
        transaction.setOwnershipType(request.ownershipType());
        transaction.setDescription(InputNormalizer.requireNonBlank(request.description(), "Description"));
        transaction.setAmount(request.amount());
        applyCurrency(transaction, account, request.amount(), true);
        transaction.setTransactionDate(request.transactionDate());
        transaction.setReferenceMonth(referenceMonthFromDate(request.transactionDate()));
        transaction.setCategory(category);
        transaction.setAccount(account);
        transaction.setMember(member);

        var saved = transactionRepository.save(transaction);
        log.info(
                "Transaction updated: id={}, type={}, ownershipType={}, referenceMonth={}, memberId={}",
                saved.getId(),
                saved.getType(),
                saved.getOwnershipType(),
                saved.getReferenceMonth(),
                auditorResolver.resolveMemberId());
        return getResponseById(saved.getId());
    }

    @Transactional
    public void delete(UUID id, DeleteScope scope) {
        var transaction = getById(id);
        var actorMemberId = auditorResolver.resolveMemberId();
        if (transaction.getInstallmentGroupId() == null || scope == DeleteScope.SINGLE) {
            transactionRepository.delete(transaction);
            log.info(
                    "Transaction deleted: id={}, scope={}, installmentGroupId={}, memberId={}",
                    transaction.getId(),
                    scope,
                    transaction.getInstallmentGroupId(),
                    actorMemberId);
            return;
        }

        if (scope == DeleteScope.FUTURE) {
            transactionRepository.deleteByInstallmentGroupIdAndInstallmentNumberGreaterThanEqual(
                    transaction.getInstallmentGroupId(), transaction.getInstallmentNumber());
            log.info(
                    "Transaction deleted: id={}, scope={}, installmentGroupId={}, memberId={}",
                    transaction.getId(),
                    scope,
                    transaction.getInstallmentGroupId(),
                    actorMemberId);
            return;
        }

        transactionRepository.deleteByInstallmentGroupId(transaction.getInstallmentGroupId());
        log.info(
                "Transaction deleted: id={}, scope={}, installmentGroupId={}, memberId={}",
                transaction.getId(),
                scope,
                transaction.getInstallmentGroupId(),
                actorMemberId);
    }

    private FamilyMember resolveMember(OwnershipType ownershipType, UUID memberId) {
        if (ownershipType == OwnershipType.SHARED) {
            return null;
        }

        if (memberId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Individual transactions require a member.");
        }

        return EntityResolver.resolveOrThrow(
                () -> familyMemberRepository.findById(memberId).filter(FamilyMember::isActive),
                ErrorMessage.FAMILY_MEMBER_NOT_FOUND.message());
    }

    private void validateIndividualAllowance(
            OwnershipType ownershipType, FamilyMember member, LocalDate transactionDate) {
        if (ownershipType == OwnershipType.SHARED || member == null) {
            return;
        }

        if (budgetRepository.existsActiveAllowanceByOwnerMemberIdAndReferenceMonth(
                member.getId(), referenceMonthFromDate(transactionDate))) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Individual transactions require a valid allowance budget for the selected member.");
    }

    private List<BigDecimal> calculateInstallmentAmounts(BigDecimal totalAmount, int installmentCount) {
        BigDecimal normalizedTotal = totalAmount.setScale(2, RoundingMode.HALF_UP);
        if (installmentCount == 1) {
            return List.of(normalizedTotal);
        }

        long totalCents = normalizedTotal.movePointRight(2).longValueExact();
        long baseCents = totalCents / installmentCount;
        long remainderCents = totalCents % installmentCount;

        List<BigDecimal> installmentAmounts = new ArrayList<>(installmentCount);
        for (int i = 0; i < installmentCount; i++) {
            long cents = baseCents + (i < remainderCents ? 1 : 0);
            installmentAmounts.add(BigDecimal.valueOf(cents, 2));
        }

        return installmentAmounts;
    }

    private void applyCurrency(Transaction transaction, Account account, BigDecimal amount, boolean throwIfMissing) {
        CurrencyConversionService.ConvertedAmount converted =
                currencyConversionService.convert(amount, account.getCurrency(), throwIfMissing);
        transaction.setCurrency(converted.currency());
        transaction.setConvertedAmount(converted.convertedAmount());
        transaction.setExchangeRate(converted.exchangeRate());
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

    private void validateInstallmentHorizon(LocalDate transactionDate, int installmentCount) {
        LocalDate lastReferenceMonth = referenceMonthFromDate(transactionDate.plusMonths(installmentCount - 1L));
        LocalDate maxReferenceMonth = dateProvider.currentReferenceMonth().plusYears(MAX_INSTALLMENT_YEARS);
        if (lastReferenceMonth.isAfter(maxReferenceMonth)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Installment plan cannot exceed 2 years.");
        }
    }

    private LocalDate referenceMonthFromDate(LocalDate transactionDate) {
        return YearMonth.from(transactionDate).atDay(1);
    }
}
