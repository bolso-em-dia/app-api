package com.mymoney.api.category;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query(
            """
            select c
            from Category c
            where c.createdInMonth <= :referenceMonth
              and (c.archivedFromMonth is null or c.archivedFromMonth > :referenceMonth)
            order by lower(c.name)
            """)
    List<Category> findAvailableForMonth(LocalDate referenceMonth);
}
