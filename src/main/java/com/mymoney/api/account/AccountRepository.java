package com.mymoney.api.account;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    @Query(
            """
            select a
            from Account a
            where (:search = '' or lower(a.name) like concat('%', lower(:search), '%'))
              and (
                :status = 'ALL'
                or (:status = 'ACTIVE' and a.archivedFromMonth is null)
                or (:status = 'ARCHIVED' and a.archivedFromMonth is not null)
              )
              and (:type is null or a.type = :type)
            """)
    Page<Account> findByFilters(String search, String status, AccountType type, Pageable pageable);

    @Query(
            """
            select a
            from Account a
            where a.createdInMonth <= :referenceMonth
              and (a.archivedFromMonth is null or a.archivedFromMonth > :referenceMonth)
            order by lower(a.name)
            """)
    List<Account> findAvailableForMonth(LocalDate referenceMonth);
}
