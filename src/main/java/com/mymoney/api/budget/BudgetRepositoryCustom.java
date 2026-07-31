package com.mymoney.api.budget;

import com.mymoney.api.shared.ApiSortDirection;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BudgetRepositoryCustom {

    Page<UUID> findIdsForMonth(
            LocalDate referenceMonth,
            String search,
            String status,
            BudgetType type,
            BudgetListSortBy sortBy,
            ApiSortDirection sortDir,
            Pageable pageable);
}
