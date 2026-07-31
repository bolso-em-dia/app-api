package com.mymoney.api.transaction;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountService;
import com.mymoney.api.account.AccountType;
import com.mymoney.api.audit.AuditorResolver;
import com.mymoney.api.budget.BudgetRepository;
import com.mymoney.api.category.CategoryService;
import com.mymoney.api.error.CodedResponseStatusException;
import com.mymoney.api.error.ErrorCode;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.shared.DateProvider;
import com.mymoney.api.shared.EntityResolver;
import com.mymoney.api.shared.InputNormalizer;
import com.mymoney.api.transaction.api.request.CreateTransactionRequest;
import com.mymoney.api.transaction.api.request.MoveTransactionDateRequest;
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
                throw new CodedResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PAGE_SIZE);
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
                .orElseThrow(
                        () -> new CodedResponseStatusException(HttpStatus.NOT_FOUND, ErrorCode.TRANSACTION_NOT_FOUND));
    }

    @Transactional
    public void materializeMonth(LocalDate referenceMonth) {
        effectiveMonthlyTransactionService.materializeMonth(referenceMonth);
    }

    @Transactional(readOnly = true)
    public Transaction getById(UUID id) {
        return EntityResolver.resolveOrThrow(() -> transactionRepository.findById(id), ErrorCode.TRANSACTION_NOT_FOUND);
    }

    @Transactional
    public TransactionResponse moveDate(UUID id, MoveTransactionDateRequest request) {
        var transaction = getDetailedById(id);
        validateMoveDateAllowed(transaction);
        validateMoveScope(transaction, request.scope());
        validateMoveDateConfirmation(transaction, request);

        var transactionsToMove = resolveTransactionsToMove(transaction, request.scope());
        for (var item : transactionsToMove) {
            var movedTransactionDate = moveToNextMonth(item.getTransactionDate());
            var movedReferenceMonth = resolveReferenceMonth(
                    movedTransactionDate, item.getAccount(), item.getType(), item.getReferenceMonthPolicy());

            validateIndividualAllowance(item.getOwnershipType(), item.getMember(), movedReferenceMonth);
            item.setTransactionDate(movedTransactionDate);
            item.setReferenceMonth(movedReferenceMonth);
            transactionRepository.save(item);
        }

        log.info(
                "Transaction date moved: id={}, scope={}, installmentGroupId={}, memberId={}",
                transaction.getId(),
                request.scope(),
                transaction.getInstallmentGroupId(),
                auditorResolver.resolveMemberId());
        return getResponseById(id);
    }

    @Transactional
    public List<TransactionResponse> create(CreateTransactionRequest request) {
        validateInstallmentCount(request.installmentCount());
        var category = categoryService.getById(request.categoryId());
        var account = accountService.getById(request.accountId());
        var member = resolveMember(request.ownershipType(), request.memberId());
        var referenceMonthPolicy = resolveCreateReferenceMonthPolicy(request.referenceMonthPolicy());

        var installmentCount = request.installmentCount() == null ? 1 : request.installmentCount();
        var firstReferenceMonth =
                resolveReferenceMonth(request.transactionDate(), account, request.type(), referenceMonthPolicy);
        validateInstallmentHorizon(firstReferenceMonth, installmentCount);
        var installmentGroupId = installmentCount > 1 ? UUID.randomUUID() : null;
        var installmentAmounts = calculateInstallmentAmounts(request.amount(), installmentCount);
        var actorMemberId = auditorResolver.resolveMemberId();

        var created = new ArrayList<TransactionResponse>();
        for (int i = 0; i < installmentCount; i++) {
            var transactionDate = request.transactionDate().plusMonths(i);
            var referenceMonth = firstReferenceMonth.plusMonths(i);
            var transaction = new Transaction();
            validateIndividualAllowance(request.ownershipType(), member, referenceMonth);
            transaction.setType(request.type());
            transaction.setOwnershipType(request.ownershipType());
            transaction.setSourceType(
                    installmentCount > 1 ? TransactionSourceType.INSTALLMENT : TransactionSourceType.MANUAL);
            transaction.setDescription(InputNormalizer.requireNonBlank(request.description(), "Description"));
            BigDecimal rawAmount = installmentAmounts.get(i);
            transaction.setAmount(rawAmount);
            applyCurrency(transaction, account, rawAmount, true);
            transaction.setTransactionDate(transactionDate);
            transaction.setReferenceMonth(referenceMonth);
            transaction.setReferenceMonthPolicy(referenceMonthPolicy);
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
        var referenceMonthPolicy = resolveUpdateReferenceMonthPolicy(transaction, request.referenceMonthPolicy());
        var referenceMonth =
                resolveReferenceMonth(request.transactionDate(), account, request.type(), referenceMonthPolicy);
        validateIndividualAllowance(request.ownershipType(), member, referenceMonth);

        transaction.setType(request.type());
        transaction.setOwnershipType(request.ownershipType());
        transaction.setDescription(InputNormalizer.requireNonBlank(request.description(), "Description"));
        transaction.setAmount(request.amount());
        applyCurrency(transaction, account, request.amount(), true);
        transaction.setTransactionDate(request.transactionDate());
        transaction.setReferenceMonth(referenceMonth);
        transaction.setReferenceMonthPolicy(referenceMonthPolicy);
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
    public TransactionResponse updateReferenceMonthPolicy(UUID id, ReferenceMonthPolicy referenceMonthPolicy) {
        var transaction = getById(id);
        validateReferenceMonthPolicyUpdateAllowed(transaction);

        transaction.setReferenceMonthPolicy(referenceMonthPolicy);
        transaction.setReferenceMonth(resolveReferenceMonth(
                transaction.getTransactionDate(),
                transaction.getAccount(),
                transaction.getType(),
                referenceMonthPolicy));

        var saved = transactionRepository.save(transaction);
        log.info(
                "Transaction reference month policy updated: id={}, referenceMonth={}, policy={}, memberId={}",
                saved.getId(),
                saved.getReferenceMonth(),
                saved.getReferenceMonthPolicy(),
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
            throw new CodedResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INDIVIDUAL_TRANSACTION_REQUIRES_MEMBER);
        }

        return EntityResolver.resolveOrThrow(
                () -> familyMemberRepository.findById(memberId).filter(FamilyMember::isActive),
                ErrorCode.FAMILY_MEMBER_NOT_FOUND);
    }

    private void validateIndividualAllowance(
            OwnershipType ownershipType, FamilyMember member, LocalDate referenceMonth) {
        if (ownershipType == OwnershipType.SHARED || member == null) {
            return;
        }

        if (budgetRepository.existsActiveAllowanceByOwnerMemberIdAndReferenceMonth(member.getId(), referenceMonth)) {
            return;
        }

        throw new CodedResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INDIVIDUAL_TRANSACTION_REQUIRES_ALLOWANCE);
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
            throw new CodedResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INSTALLMENT_COUNT_RANGE);
        }
    }

    private void validateInstallmentHorizon(LocalDate firstReferenceMonth, int installmentCount) {
        LocalDate lastReferenceMonth = firstReferenceMonth.plusMonths(installmentCount - 1L);
        LocalDate maxReferenceMonth = dateProvider.currentReferenceMonth().plusYears(MAX_INSTALLMENT_YEARS);
        if (lastReferenceMonth.isAfter(maxReferenceMonth)) {
            throw new CodedResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INSTALLMENT_PLAN_TOO_LONG);
        }
    }

    private ReferenceMonthPolicy resolveCreateReferenceMonthPolicy(ReferenceMonthPolicy referenceMonthPolicy) {
        return referenceMonthPolicy == null ? ReferenceMonthPolicy.AUTO : referenceMonthPolicy;
    }

    private ReferenceMonthPolicy resolveUpdateReferenceMonthPolicy(
            Transaction transaction, ReferenceMonthPolicy referenceMonthPolicy) {
        if (referenceMonthPolicy != null) {
            return referenceMonthPolicy;
        }
        return transaction.getReferenceMonthPolicy() == null
                ? ReferenceMonthPolicy.AUTO
                : transaction.getReferenceMonthPolicy();
    }

    private LocalDate resolveReferenceMonth(
            LocalDate transactionDate,
            Account account,
            TransactionType transactionType,
            ReferenceMonthPolicy referenceMonthPolicy) {
        var policy = referenceMonthPolicy == null ? ReferenceMonthPolicy.AUTO : referenceMonthPolicy;
        validateReferenceMonthPolicy(account, transactionType, policy);

        var transactionMonth = referenceMonthFromDate(transactionDate);
        if (policy == ReferenceMonthPolicy.FORCE_CURRENT || !supportsCreditCardNextMonth(account, transactionType)) {
            return transactionMonth;
        }
        if (policy == ReferenceMonthPolicy.FORCE_NEXT) {
            return transactionMonth.plusMonths(1);
        }
        if (shouldMoveToNextMonth(transactionDate, account)) {
            return transactionMonth.plusMonths(1);
        }
        return transactionMonth;
    }

    private void validateReferenceMonthPolicy(
            Account account, TransactionType transactionType, ReferenceMonthPolicy referenceMonthPolicy) {
        if (referenceMonthPolicy != ReferenceMonthPolicy.FORCE_NEXT) {
            return;
        }
        if (supportsCreditCardNextMonth(account, transactionType)) {
            return;
        }
        throw new CodedResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.REFERENCE_MONTH_POLICY_NOT_ALLOWED);
    }

    private boolean supportsCreditCardNextMonth(Account account, TransactionType transactionType) {
        return transactionType == TransactionType.EXPENSE
                && account.getType() == AccountType.CREDIT_CARD
                && account.getClosingDay() != null;
    }

    private boolean shouldMoveToNextMonth(LocalDate transactionDate, Account account) {
        if (account.getClosingDay() == null) {
            return false;
        }
        return transactionDate.getDayOfMonth() > account.getClosingDay();
    }

    private void validateReferenceMonthPolicyUpdateAllowed(Transaction transaction) {
        if (transaction.getSourceType() != TransactionSourceType.MANUAL
                || !supportsCreditCardNextMonth(transaction.getAccount(), transaction.getType())) {
            throw new CodedResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.REFERENCE_MONTH_POLICY_UPDATE_NOT_ALLOWED);
        }
    }

    private Transaction getDetailedById(UUID id) {
        return EntityResolver.resolveOrThrow(
                () -> transactionRepository.findDetailedById(id), ErrorCode.TRANSACTION_NOT_FOUND);
    }

    private void validateMoveDateAllowed(Transaction transaction) {
        if (transaction.getSourceType() == TransactionSourceType.MANUAL
                || transaction.getSourceType() == TransactionSourceType.INSTALLMENT) {
            return;
        }

        throw new CodedResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.MOVE_TO_NEXT_MONTH_NOT_ALLOWED);
    }

    private void validateMoveScope(Transaction transaction, MoveTransactionDateScope scope) {
        if (scope != MoveTransactionDateScope.FUTURE) {
            return;
        }
        if (transaction.getInstallmentGroupId() != null && transaction.getInstallmentNumber() != null) {
            return;
        }

        throw new CodedResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.MOVE_TRANSACTION_DATE_FUTURE_SCOPE_NOT_ALLOWED);
    }

    private void validateMoveDateConfirmation(Transaction transaction, MoveTransactionDateRequest request) {
        if (!transaction.getTransactionDate().equals(request.expectedCurrentTransactionDate())) {
            throw new CodedResponseStatusException(
                    HttpStatus.CONFLICT, ErrorCode.TRANSACTION_DATE_CONFIRMATION_MISMATCH);
        }

        var expectedNewTransactionDate = moveToNextMonth(transaction.getTransactionDate());
        if (expectedNewTransactionDate.equals(request.confirmedNewTransactionDate())) {
            return;
        }

        throw new CodedResponseStatusException(
                HttpStatus.CONFLICT, ErrorCode.NEW_TRANSACTION_DATE_CONFIRMATION_MISMATCH);
    }

    private List<Transaction> resolveTransactionsToMove(Transaction transaction, MoveTransactionDateScope scope) {
        if (scope == MoveTransactionDateScope.SINGLE) {
            return List.of(transaction);
        }

        return transactionRepository
                .findByInstallmentGroupIdAndInstallmentNumberGreaterThanEqualOrderByInstallmentNumberAsc(
                        transaction.getInstallmentGroupId(), transaction.getInstallmentNumber());
    }

    private LocalDate moveToNextMonth(LocalDate transactionDate) {
        var nextMonth = YearMonth.from(transactionDate).plusMonths(1);
        return nextMonth.atDay(Math.min(transactionDate.getDayOfMonth(), nextMonth.lengthOfMonth()));
    }

    private LocalDate referenceMonthFromDate(LocalDate transactionDate) {
        return YearMonth.from(transactionDate).atDay(1);
    }
}
