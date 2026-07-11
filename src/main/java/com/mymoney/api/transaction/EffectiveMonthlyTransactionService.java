package com.mymoney.api.transaction;

import com.mymoney.api.account.CurrencyType;
import com.mymoney.api.exchangerate.ExchangeRate;
import com.mymoney.api.exchangerate.ExchangeRateRepository;
import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EffectiveMonthlyTransactionService {

    private static final Comparator<EffectiveTransaction> EFFECTIVE_TRANSACTION_ORDER = Comparator.comparing(
                    (EffectiveTransaction item) -> item.transaction().getTransactionDate())
            .thenComparing(item -> item.transaction().getCreatedAt(), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(item -> item.transaction().getId());

    private final TransactionRepository transactionRepository;
    private final FixedExpenseTemplateRepository fixedExpenseTemplateRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    @Transactional
    public List<EffectiveTransaction> listEffectiveTransactions(
            LocalDate referenceMonth,
            TransactionType type,
            OwnershipType ownershipType,
            UUID accountId,
            List<UUID> categoryIds,
            UUID memberId) {
        if (!referenceMonth.isAfter(currentReferenceMonth())) {
            ensureMaterializedForMonth(referenceMonth);
        }

        List<Transaction> persistedTransactions =
                transactionRepository.findByReferenceMonthOrderByTransactionDateAscCreatedAtAsc(referenceMonth);
        Set<UUID> materializedTemplateIds = materializedTemplateIds(persistedTransactions);

        List<EffectiveTransaction> effectiveTransactions = new ArrayList<>(persistedTransactions.size());
        for (Transaction transaction : persistedTransactions) {
            effectiveTransactions.add(new EffectiveTransaction(transaction, false));
        }

        if (referenceMonth.isAfter(currentReferenceMonth())) {
            for (FixedExpenseTemplate template : fixedExpenseTemplateRepository.findActiveForMonth(referenceMonth)) {
                if (materializedTemplateIds.contains(template.getId())) {
                    continue;
                }
                effectiveTransactions.add(new EffectiveTransaction(projectTransaction(template, referenceMonth), true));
            }
        }

        return effectiveTransactions.stream()
                .filter(item ->
                        matchesFilters(item.transaction(), type, ownershipType, accountId, categoryIds, memberId))
                .sorted(EFFECTIVE_TRANSACTION_ORDER)
                .toList();
    }

    @Transactional
    public void ensureMaterializedForMonth(LocalDate referenceMonth) {
        if (referenceMonth.isAfter(currentReferenceMonth())) {
            return;
        }

        List<Transaction> persistedTransactions =
                transactionRepository.findByReferenceMonthOrderByTransactionDateAscCreatedAtAsc(referenceMonth);
        Set<UUID> materializedTemplateIds = materializedTemplateIds(persistedTransactions);

        for (FixedExpenseTemplate template : fixedExpenseTemplateRepository.findActiveForMonth(referenceMonth)) {
            if (materializedTemplateIds.contains(template.getId())) {
                continue;
            }
            transactionRepository.save(materializeTransaction(template, referenceMonth));
        }
    }

    @Transactional
    public void syncCurrentMonthTransaction(FixedExpenseTemplate template) {
        LocalDate referenceMonth = currentReferenceMonth();
        if (!isTemplateActiveForMonth(template, referenceMonth)) {
            return;
        }

        Transaction transaction = transactionRepository
                .findByFixedExpenseTemplateIdAndReferenceMonth(template.getId(), referenceMonth)
                .orElseGet(() -> materializeTransaction(template, referenceMonth));

        applyTemplateValues(transaction, template, referenceMonth);
        transactionRepository.save(transaction);
    }

    private Set<UUID> materializedTemplateIds(List<Transaction> transactions) {
        Set<UUID> templateIds = new HashSet<>();
        for (Transaction transaction : transactions) {
            if (transaction.getFixedExpenseTemplate() != null) {
                templateIds.add(transaction.getFixedExpenseTemplate().getId());
            }
        }
        return templateIds;
    }

    private boolean matchesFilters(
            Transaction transaction,
            TransactionType type,
            OwnershipType ownershipType,
            UUID accountId,
            List<UUID> categoryIds,
            UUID memberId) {
        if (type != null && transaction.getType() != type) {
            return false;
        }
        if (ownershipType != null && transaction.getOwnershipType() != ownershipType) {
            return false;
        }
        if (accountId != null && !transaction.getAccount().getId().equals(accountId)) {
            return false;
        }
        if (categoryIds != null
                && !categoryIds.isEmpty()
                && !categoryIds.contains(transaction.getCategory().getId())) {
            return false;
        }
        if (memberId != null) {
            return transaction.getMember() != null
                    && transaction.getMember().getId().equals(memberId);
        }
        return true;
    }

    private Transaction projectTransaction(FixedExpenseTemplate template, LocalDate referenceMonth) {
        Transaction transaction = new Transaction();
        transaction.setId(projectedTransactionId(template.getId(), referenceMonth));
        applyTemplateValues(transaction, template, referenceMonth);
        return transaction;
    }

    private Transaction materializeTransaction(FixedExpenseTemplate template, LocalDate referenceMonth) {
        Transaction transaction = new Transaction();
        applyTemplateValues(transaction, template, referenceMonth);
        return transaction;
    }

    private void applyTemplateValues(Transaction transaction, FixedExpenseTemplate template, LocalDate referenceMonth) {
        transaction.setType(template.getType());
        transaction.setOwnershipType(OwnershipType.SHARED);
        transaction.setSourceType(TransactionSourceType.FIXED_EXPENSE);
        transaction.setDescription(template.getName());
        if (template.getCurrency() == CurrencyType.USD) {
            BigDecimal rate = exchangeRateRepository
                    .findFirstByCurrencyOrderByFetchedAtDesc("USD")
                    .map(ExchangeRate::getRate)
                    .orElse(BigDecimal.ONE);
            transaction.setOriginalAmount(template.getAmount());
            transaction.setCurrency("USD");
            transaction.setAmount(template.getAmount().multiply(rate));
        } else {
            transaction.setAmount(template.getAmount());
        }
        transaction.setTransactionDate(resolveTransactionDate(referenceMonth, template.getDueDay()));
        transaction.setReferenceMonth(referenceMonth);
        transaction.setAccount(template.getAccount());
        transaction.setCategory(template.getCategory());
        transaction.setMember(null);
        transaction.setFixedExpenseTemplate(template);
        transaction.setInstallmentGroupId(null);
        transaction.setInstallmentNumber(null);
        transaction.setInstallmentTotal(null);
    }

    private boolean isTemplateActiveForMonth(FixedExpenseTemplate template, LocalDate referenceMonth) {
        if (template.getCreatedInMonth().isAfter(referenceMonth)) {
            return false;
        }
        return template.getArchivedFromMonth() == null
                || template.getArchivedFromMonth().isAfter(referenceMonth);
    }

    private LocalDate resolveTransactionDate(LocalDate referenceMonth, Short dueDay) {
        YearMonth yearMonth = YearMonth.from(referenceMonth);
        int day = Math.min(dueDay.intValue(), yearMonth.lengthOfMonth());
        return yearMonth.atDay(day);
    }

    private UUID projectedTransactionId(UUID templateId, LocalDate referenceMonth) {
        return UUID.nameUUIDFromBytes((templateId + ":" + referenceMonth).getBytes(StandardCharsets.UTF_8));
    }

    private LocalDate currentReferenceMonth() {
        return YearMonth.now().atDay(1);
    }

    public static final class EffectiveTransaction {

        private final Transaction transaction;
        private final boolean projected;

        public EffectiveTransaction(Transaction transaction, boolean projected) {
            this.transaction = Objects.requireNonNull(transaction);
            this.projected = projected;
        }

        public Transaction transaction() {
            return transaction;
        }

        public boolean projected() {
            return projected;
        }
    }
}
