package com.mymoney.api.category;

import com.mymoney.api.category.api.response.CategoryResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query(
            """
            select c
            from Category c
            where f_unaccent_lower(trim(c.name)) = f_unaccent_lower(trim(:name))
            """)
    Optional<Category> findByNormalizedName(String name);

    @Query(
            """
            select new com.mymoney.api.category.api.response.CategoryResponse(
                c.id,
                c.name,
                c.icon,
                c.color,
                c.createdInMonth,
                c.archivedFromMonth,
                rc.id,
                c.createdAt,
                c.updatedAt
            )
            from Category c
            left join c.replacementCategory rc
            where (:search = '' or f_unaccent_lower(c.name) like concat('%', f_unaccent_lower(:search), '%'))
              and (
                :status = 'ALL'
                or (:status = 'ACTIVE' and c.archivedFromMonth is null)
                or (:status = 'ARCHIVED' and c.archivedFromMonth is not null)
              )
            """)
    Page<CategoryResponse> findResponseByFilters(String search, String status, Pageable pageable);

    @Query(
            """
            select new com.mymoney.api.category.api.response.CategoryResponse(
                c.id,
                c.name,
                c.icon,
                c.color,
                c.createdInMonth,
                c.archivedFromMonth,
                rc.id,
                c.createdAt,
                c.updatedAt
            )
            from Category c
            left join c.replacementCategory rc
            where c.id = :id
            """)
    Optional<CategoryResponse> findResponseById(UUID id);

    @Query(
            """
            select c
            from Category c
            where c.createdInMonth <= :referenceMonth
              and (c.archivedFromMonth is null or c.archivedFromMonth > :referenceMonth)
            order by f_unaccent_lower(c.name)
            """)
    List<Category> findAvailableForMonth(LocalDate referenceMonth);
}
