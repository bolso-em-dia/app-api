package com.mymoney.api.fixedexpense;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedExpenseTemplateRepository extends JpaRepository<FixedExpenseTemplate, UUID> {

    Page<FixedExpenseTemplate> findAllBy(Pageable pageable);
}
