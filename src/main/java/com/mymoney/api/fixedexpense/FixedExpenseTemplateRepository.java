package com.mymoney.api.fixedexpense;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FixedExpenseTemplateRepository extends JpaRepository<FixedExpenseTemplate, UUID> {

    @Query(
            """
            select t
            from FixedExpenseTemplate t
            where (:search = '' or lower(t.name) like concat('%', lower(:search), '%'))
              and (
                :status = 'ALL'
                or (:status = 'ACTIVE' and t.archivedFromMonth is null)
                or (:status = 'ARCHIVED' and t.archivedFromMonth is not null)
              )
            """)
    Page<FixedExpenseTemplate> findByFilters(String search, String status, Pageable pageable);
}
