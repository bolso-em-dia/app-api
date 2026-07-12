package com.mymoney.api.fixedexpense;

import com.mymoney.api.fixedexpense.api.response.FixedExpenseTemplateResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FixedExpenseTemplateRepository extends JpaRepository<FixedExpenseTemplate, UUID> {

    @Query(
            """
            select new com.mymoney.api.fixedexpense.api.response.FixedExpenseTemplateResponse(
                t.id,
                t.name,
                t.type,
                t.amount,
                t.convertedAmount,
                t.exchangeRate,
                cast(t.currency as string),
                c.id,
                c.name,
                a.id,
                a.name,
                t.dueDay,
                t.createdInMonth,
                t.archivedFromMonth,
                t.active,
                t.createdAt,
                t.updatedAt
            )
            from FixedExpenseTemplate t
            join t.category c
            join t.account a
            where (:search = '' or f_unaccent_lower(t.name) like concat('%', f_unaccent_lower(:search), '%'))
              and (
                :status = 'ALL'
                or (:status = 'ACTIVE' and t.archivedFromMonth is null)
                or (:status = 'ARCHIVED' and t.archivedFromMonth is not null)
              )
            """)
    Page<FixedExpenseTemplateResponse> findResponseByFilters(String search, String status, Pageable pageable);

    @Query(
            """
            select new com.mymoney.api.fixedexpense.api.response.FixedExpenseTemplateResponse(
                t.id,
                t.name,
                t.type,
                t.amount,
                t.convertedAmount,
                t.exchangeRate,
                cast(t.currency as string),
                c.id,
                c.name,
                a.id,
                a.name,
                t.dueDay,
                t.createdInMonth,
                t.archivedFromMonth,
                t.active,
                t.createdAt,
                t.updatedAt
            )
            from FixedExpenseTemplate t
            join t.category c
            join t.account a
            where t.id = :id
            """)
    Optional<FixedExpenseTemplateResponse> findResponseById(UUID id);

    @EntityGraph(attributePaths = {"category", "account"})
    @Query(
            """
            select t
            from FixedExpenseTemplate t
            where t.createdInMonth <= :referenceMonth
              and (t.archivedFromMonth is null or t.archivedFromMonth > :referenceMonth)
            """)
    List<FixedExpenseTemplate> findActiveForMonth(LocalDate referenceMonth);

    @EntityGraph(attributePaths = {"category", "account"})
    @Query(
            """
            select t
            from FixedExpenseTemplate t
            where t.createdInMonth <= :referenceMonth
              and (t.archivedFromMonth is null or t.archivedFromMonth > :referenceMonth)
              and not exists (
                  select 1 from Transaction tr
                  where tr.fixedExpenseTemplate.id = t.id
                    and tr.referenceMonth = :referenceMonth
              )
            """)
    List<FixedExpenseTemplate> findActiveNotMaterializedForMonth(LocalDate referenceMonth);
}
