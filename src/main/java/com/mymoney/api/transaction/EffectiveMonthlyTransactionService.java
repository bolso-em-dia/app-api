package com.mymoney.api.transaction;

import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateRepository;
import com.mymoney.api.shared.DateProvider;
import com.mymoney.api.shared.DayValidator;
import com.mymoney.api.shared.InputNormalizer;
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
    private static final int FUTURE_MATERIALIZATION_MONTHS = 3;

    private final TransactionRepository transactionRepository;
    private final FixedExpenseTemplateRepository fixedExpenseTemplateRepository;
    private final CurrencyConversionService currencyConversionService;
    private final DateProvider dateProvider;

    @Transactional
    public List<EffectiveTransaction> listEffectiveTransactions(
            LocalDate referenceMonth,
            TransactionType type,
            OwnershipType ownershipType,
            UUID accountId,
            List<UUID> categoryIds,
            UUID memberId,
            String search) {
        String normalizedSearch = InputNormalizer.normalizeNullable(search);
        List<Transaction> persistedTransactions = transactionRepository.findByFilters(
                referenceMonth, type, ownershipType, accountId, categoryIds, memberId, normalizedSearch);

        return persistedTransactions.stream()
                .map(t -> new EffectiveTransaction(t, false))
                .sorted(EFFECTIVE_TRANSACTION_ORDER)
                .toList();
    }

    @Transactional
    public void ensureMaterializedForMonth(LocalDate referenceMonth) {
        if (referenceMonth.isAfter(dateProvider.currentReferenceMonth())) {
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
    public void materializeMonth(LocalDate referenceMonth) {
        List<FixedExpenseTemplate> templatesToMaterialize =
                fixedExpenseTemplateRepository.findActiveNotMaterializedForMonth(referenceMonth);

        if (templatesToMaterialize.isEmpty()) {
            return;
        }

        List<Transaction> transactions = new ArrayList<>(templatesToMaterialize.size());
        for (FixedExpenseTemplate template : templatesToMaterialize) {
            transactions.add(materializeTransaction(template, referenceMonth));
        }

        transactionRepository.saveAll(transactions);
    }

    @Transactional
    public void syncCurrentMonthTransaction(FixedExpenseTemplate template) {
        LocalDate horizon = horizonMonth();
        for (LocalDate month = dateProvider.currentReferenceMonth();
                !month.isAfter(horizon);
                month = month.plusMonths(1)) {
            syncMonth(template, month);
        }

        deleteBeyondHorizon(template, horizon);
    }

    private void syncMonth(FixedExpenseTemplate template, LocalDate month) {
        if (isTemplateActiveForMonth(template, month)) {
            upsertTemplateTransaction(template, month);
            return;
        }

        transactionRepository
                .findByFixedExpenseTemplateIdAndReferenceMonth(template.getId(), month)
                .ifPresent(transactionRepository::delete);
    }

    private void upsertTemplateTransaction(FixedExpenseTemplate template, LocalDate month) {
        Transaction transaction = transactionRepository
                .findByFixedExpenseTemplateIdAndReferenceMonth(template.getId(), month)
                .orElseGet(() -> materializeTransaction(template, month));
        applyTemplateValues(transaction, template, month);
        transactionRepository.save(transaction);
    }

    private void deleteBeyondHorizon(FixedExpenseTemplate template, LocalDate horizon) {
        List<Transaction> futureTransactions =
                transactionRepository.findByFixedExpenseTemplateIdAndReferenceMonthGreaterThan(
                        template.getId(), horizon);
        for (Transaction transaction : futureTransactions) {
            transactionRepository.delete(transaction);
        }
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
        transaction.setAmount(template.getAmount());
        CurrencyConversionService.ConvertedAmount converted =
                currencyConversionService.convert(template.getAmount(), template.getCurrency(), false);
        transaction.setCurrency(converted.currency());
        transaction.setConvertedAmount(converted.convertedAmount());
        transaction.setExchangeRate(converted.exchangeRate());
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
        DayValidator.requireDueDay(dueDay);
        YearMonth yearMonth = YearMonth.from(referenceMonth);
        int day = Math.min(dueDay.intValue(), yearMonth.lengthOfMonth());
        return yearMonth.atDay(day);
    }

    private LocalDate horizonMonth() {
        return dateProvider.currentReferenceMonth().plusMonths(FUTURE_MATERIALIZATION_MONTHS);
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
