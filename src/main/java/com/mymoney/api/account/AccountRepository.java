package com.mymoney.api.account;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, UUID> {

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
