package com.mymoney.api.budget;

import com.mymoney.api.shared.ApiSortDirection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class BudgetRepositoryImpl implements BudgetRepositoryCustom {

    private static final String BASE_WHERE =
            """
            from Budget b
            where b.createdInMonth <= :referenceMonth
              and (:search = '' or f_unaccent_lower(b.name) like concat('%', f_unaccent_lower(:search), '%'))
              and (
                :status = 'ALL'
                or (:status = 'ACTIVE'
                    and (b.archivedFromMonth is null or b.archivedFromMonth > :referenceMonth))
                or (:status = 'ARCHIVED'
                    and b.archivedFromMonth is not null
                    and b.archivedFromMonth <= :referenceMonth)
              )
              and (:type is null or b.type = :type)
            """;

    private static final String REMAINING_AMOUNT_EXPRESSION =
            """
            (
                b.monthlyLimit - coalesce(
                    (
                        select sum(t.convertedAmount)
                        from Transaction t
                        where t.referenceMonth = :referenceMonth
                          and (
                              (b.type = com.mymoney.api.budget.BudgetType.ALLOWANCE
                                  and b.ownerMember is not null
                                  and t.ownershipType = com.mymoney.api.transaction.OwnershipType.INDIVIDUAL
                                  and t.member.id = b.ownerMember.id)
                              or (b.type = com.mymoney.api.budget.BudgetType.GLOBAL
                                  and t.ownershipType = com.mymoney.api.transaction.OwnershipType.SHARED
                                  and t.category member of b.categories)
                          )
                    ),
                    0
                )
            )
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<UUID> findIdsForMonth(
            LocalDate referenceMonth,
            String search,
            String status,
            BudgetType type,
            BudgetListSortBy sortBy,
            ApiSortDirection sortDir,
            Pageable pageable) {
        var resolvedSortBy = sortBy == null ? BudgetListSortBy.NAME : sortBy;
        var resolvedSortDir = sortDir == null ? resolvedSortBy.defaultDirection() : sortDir;
        var idsQuery = entityManager.createQuery(
                "select b.id\n" + BASE_WHERE + buildOrderBy(resolvedSortBy, resolvedSortDir), UUID.class);
        applyParameters(idsQuery, referenceMonth, search, status, type);

        if (pageable.isPaged()) {
            idsQuery.setFirstResult((int) pageable.getOffset());
            idsQuery.setMaxResults(pageable.getPageSize());
        }

        var content = idsQuery.getResultList();
        if (pageable.isUnpaged()) {
            return new PageImpl<>(content);
        }

        var countQuery = entityManager.createQuery("select count(b)\n" + BASE_WHERE, Long.class);
        applyParameters(countQuery, referenceMonth, search, status, type);
        return new PageImpl<>(content, pageable, countQuery.getSingleResult());
    }

    private String buildOrderBy(BudgetListSortBy sortBy, ApiSortDirection sortDir) {
        var direction = sortDir == ApiSortDirection.ASC ? "asc" : "desc";
        return switch (sortBy) {
            case NAME -> "\norder by f_unaccent_lower(b.name) " + direction + ", b.id asc";
            case MONTHLY_LIMIT -> "\norder by b.monthlyLimit " + direction + ", f_unaccent_lower(b.name) asc, b.id asc";
            case REMAINING_AMOUNT ->
                "\norder by "
                        + REMAINING_AMOUNT_EXPRESSION
                        + " "
                        + direction
                        + ", f_unaccent_lower(b.name) asc, b.id asc";
        };
    }

    private void applyParameters(
            jakarta.persistence.Query query, LocalDate referenceMonth, String search, String status, BudgetType type) {
        query.setParameter("referenceMonth", referenceMonth);
        query.setParameter("search", search);
        query.setParameter("status", status);
        query.setParameter("type", type);
    }
}
