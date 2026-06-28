package com.mymoney.api.fixedexpense;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedExpenseTemplateRepository extends JpaRepository<FixedExpenseTemplate, UUID> {}
