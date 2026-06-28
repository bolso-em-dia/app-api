package com.mymoney.api.transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

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

    List<Transaction> findByInstallmentGroupIdOrderByInstallmentNumber(UUID installmentGroupId);

    void deleteByInstallmentGroupIdAndInstallmentNumberGreaterThanEqual(
            UUID installmentGroupId, Short installmentNumber);

    void deleteByInstallmentGroupId(UUID installmentGroupId);
}
