package com.mymoney.api.transaction;

import com.mymoney.api.transaction.api.response.TransactionResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
                t.createdAt,
                t.updatedAt
            )
            from Transaction t
            join t.account a
            join t.category c
            left join t.member m
            where t.referenceMonth = :referenceMonth
              and (:type is null or t.type = :type)
              and (:ownershipType is null or t.ownershipType = :ownershipType)
              and (:accountId is null or t.account.id = :accountId)
              and (:categoryId is null or t.category.id = :categoryId)
              and (:memberId is null or t.member.id = :memberId)
            """)
    Page<TransactionResponse> findResponseByFilters(
            LocalDate referenceMonth,
            TransactionType type,
            OwnershipType ownershipType,
            UUID accountId,
            UUID categoryId,
            UUID memberId,
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
                t.createdAt,
                t.updatedAt
            )
            from Transaction t
            join t.account a
            join t.category c
            left join t.member m
            where t.id = :id
            """)
    Optional<TransactionResponse> findResponseById(UUID id);

    @EntityGraph(attributePaths = {"account", "category", "member"})
    @Query(
            """
            select t
            from Transaction t
            where t.referenceMonth = :referenceMonth
              and (:type is null or t.type = :type)
              and (:ownershipType is null or t.ownershipType = :ownershipType)
              and (:accountId is null or t.account.id = :accountId)
              and (:categoryId is null or t.category.id = :categoryId)
              and (:memberId is null or t.member.id = :memberId)
            """)
    Page<Transaction> findByFilters(
            LocalDate referenceMonth,
            TransactionType type,
            OwnershipType ownershipType,
            UUID accountId,
            UUID categoryId,
            UUID memberId,
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
              and (:categoryId is null or t.category.id = :categoryId)
              and (:memberId is null or t.member.id = :memberId)
            order by t.transactionDate, t.createdAt
            """)
    List<Transaction> findByFilters(
            LocalDate referenceMonth,
            TransactionType type,
            OwnershipType ownershipType,
            UUID accountId,
            UUID categoryId,
            UUID memberId);

    boolean existsByFixedExpenseTemplateIdAndReferenceMonth(UUID fixedExpenseTemplateId, LocalDate referenceMonth);

    List<Transaction> findByInstallmentGroupIdOrderByInstallmentNumber(UUID installmentGroupId);

    void deleteByInstallmentGroupIdAndInstallmentNumberGreaterThanEqual(
            UUID installmentGroupId, Short installmentNumber);

    void deleteByInstallmentGroupId(UUID installmentGroupId);
}
