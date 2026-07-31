package com.mymoney.api.budget;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BudgetRepository extends JpaRepository<Budget, UUID>, BudgetRepositoryCustom {

    @EntityGraph(attributePaths = {"categories", "ownerMember"})
    @Query("""
            select distinct e
            from Budget e
            where e.id in :ids
            """)
    List<Budget> findAllWithAssociationsByIdIn(Collection<UUID> ids);

    @Query(
            """
            select distinct e
            from Budget e
            left join fetch e.categories
            left join fetch e.ownerMember
            where e.id = :id
            """)
    Optional<Budget> findWithAssociationsById(UUID id);

    @Query(
            """
            select count(e) > 0
            from Budget e
            where e.ownerMember.id = :ownerMemberId
              and e.type = :type
              and (:budgetId is null or e.id <> :budgetId)
            """)
    boolean existsAnotherByOwnerMemberIdAndType(UUID ownerMemberId, BudgetType type, UUID budgetId);

    @Query(
            """
            select count(e) > 0
            from Budget e
            where e.ownerMember.id = :ownerMemberId
              and e.type = com.mymoney.api.budget.BudgetType.ALLOWANCE
              and e.createdInMonth <= :referenceMonth
              and (e.archivedFromMonth is null or e.archivedFromMonth > :referenceMonth)
            """)
    boolean existsActiveAllowanceByOwnerMemberIdAndReferenceMonth(UUID ownerMemberId, LocalDate referenceMonth);
}
