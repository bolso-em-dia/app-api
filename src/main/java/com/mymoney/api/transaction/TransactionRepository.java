package com.mymoney.api.transaction;

import com.mymoney.api.fixedexpense.FixedExpenseTemplate;
import com.mymoney.api.transaction.api.response.TransactionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query(
            """
            select new com.mymoney.api.transaction.api.response.TransactionResponse(
                t.id,
                t.type,
                t.ownershipType,
                t.sourceType,
                t.description,
                t.amount,
                t.convertedAmount,
                t.exchangeRate,
                t.currency,
                t.transactionDate,
                t.referenceMonth,
                a.id,
                a.name,
                c.id,
                c.name,
                m.id,
                m.name,
                t.installmentGroupId,
                t.installmentNumber,
                t.installmentTotal,
                ft.id,
                false,
                t.createdAt,
                t.updatedAt
            )
            from Transaction t
            join t.account a
            join t.category c
            left join t.member m
            left join t.fixedExpenseTemplate ft
            where t.referenceMonth = :referenceMonth
              and (:type is null or t.type = :type)
              and (:ownershipType is null or t.ownershipType = :ownershipType)
              and (:accountId is null or t.account.id = :accountId)
              and (:categoryIds is null or t.category.id in :categoryIds)
              and (:memberId is null or t.member.id = :memberId)
              and (:search is null or :search = '' or f_unaccent_lower(t.description) like concat('%', f_unaccent_lower(:search), '%'))
            """)
    Page<TransactionResponse> findResponseByFilters(
            LocalDate referenceMonth,
            TransactionType type,
            OwnershipType ownershipType,
            UUID accountId,
            List<UUID> categoryIds,
            UUID memberId,
            String search,
            Pageable pageable);

    @Query(
            """
            select new com.mymoney.api.transaction.api.response.TransactionResponse(
                t.id,
                t.type,
                t.ownershipType,
                t.sourceType,
                t.description,
                t.amount,
                t.convertedAmount,
                t.exchangeRate,
                t.currency,
                t.transactionDate,
                t.referenceMonth,
                a.id,
                a.name,
                c.id,
                c.name,
                m.id,
                m.name,
                t.installmentGroupId,
                t.installmentNumber,
                t.installmentTotal,
                ft.id,
                false,
                t.createdAt,
                t.updatedAt
            )
            from Transaction t
            join t.account a
            join t.category c
            left join t.member m
            left join t.fixedExpenseTemplate ft
            where t.id = :id
            """)
    Optional<TransactionResponse> findResponseById(UUID id);

    @Query(
            """
            select t.description
            from Transaction t
            where t.referenceMonth >= :since
              and (:query = '' or f_unaccent_lower(t.description) like concat('%', f_unaccent_lower(:query), '%'))
            group by t.description
            order by
                case when :query <> '' and f_unaccent_lower(t.description) like concat(f_unaccent_lower(:query), '%') then 0 else 1 end,
                count(t) desc,
                max(t.updatedAt) desc
            """)
    List<String> findDescriptionSuggestions(String query, LocalDate since, Pageable pageable);

    @EntityGraph(attributePaths = {"account", "category", "member"})
    @Query(
            """
            select t
            from Transaction t
            where t.referenceMonth = :referenceMonth
              and (:type is null or t.type = :type)
              and (:ownershipType is null or t.ownershipType = :ownershipType)
              and (:accountId is null or t.account.id = :accountId)
              and (:categoryIds is null or t.category.id in :categoryIds)
              and (:memberId is null or t.member.id = :memberId)
              and (:search is null or :search = '' or f_unaccent_lower(t.description) like concat('%', f_unaccent_lower(:search), '%'))
            """)
    Page<Transaction> findByFilters(
            LocalDate referenceMonth,
            TransactionType type,
            OwnershipType ownershipType,
            UUID accountId,
            List<UUID> categoryIds,
            UUID memberId,
            String search,
            Pageable pageable);

    @EntityGraph(attributePaths = {"account", "category", "member"})
    @Query(
            """
            select t
            from Transaction t
            where t.referenceMonth = :referenceMonth
              and (:type is null or t.type = :type)
              and (:ownershipType is null or t.ownershipType = :ownershipType)
              and (:accountId is null or t.account.id = :accountId)
              and (:categoryIds is null or t.category.id in :categoryIds)
              and (:memberId is null or t.member.id = :memberId)
              and (:search is null or :search = '' or lower(t.description) like concat('%', lower(:search), '%'))
            order by t.transactionDate, t.createdAt
            """)
    List<Transaction> findByFilters(
            LocalDate referenceMonth,
            TransactionType type,
            OwnershipType ownershipType,
            UUID accountId,
            List<UUID> categoryIds,
            UUID memberId,
            String search);

    @EntityGraph(attributePaths = {"account", "category", "member", "fixedExpenseTemplate"})
    List<Transaction> findByReferenceMonthOrderByTransactionDateAscCreatedAtAsc(LocalDate referenceMonth);

    Optional<Transaction> findByFixedExpenseTemplateIdAndReferenceMonth(
            UUID fixedExpenseTemplateId, LocalDate referenceMonth);

    void deleteByFixedExpenseTemplateIdAndReferenceMonthGreaterThan(
            UUID fixedExpenseTemplateId, LocalDate referenceMonth);

    void deleteByFixedExpenseTemplateIdAndReferenceMonthGreaterThanEqual(
            UUID fixedExpenseTemplateId, LocalDate referenceMonth);

    @Modifying
    @Query(
            "UPDATE Transaction t SET t.fixedExpenseTemplate = null WHERE t.fixedExpenseTemplate = :template AND t.referenceMonth < :beforeMonth")
    void detachFixedExpenseTemplateBeforeMonth(FixedExpenseTemplate template, LocalDate beforeMonth);

    void deleteByInstallmentGroupIdAndInstallmentNumberGreaterThanEqual(
            UUID installmentGroupId, Short installmentNumber);

    void deleteByInstallmentGroupId(UUID installmentGroupId);

    @Modifying
    @Query(
            """
            update Transaction t
            set t.convertedAmount = t.amount * :rate,
                t.exchangeRate = :rate
            where t.currency = :currency
              and t.referenceMonth >= :since
            """)
    void updateAmountsForCurrency(String currency, BigDecimal rate, LocalDate since);

    @Modifying
    @Query(
            """
            update Transaction t
            set t.convertedAmount = t.amount * :rate,
                t.exchangeRate = :rate
            where t.currency = :currency
              and t.referenceMonth = :referenceMonth
            """)
    void freezeAmountsForMonth(String currency, BigDecimal rate, LocalDate referenceMonth);
}
