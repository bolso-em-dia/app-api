package com.mymoney.api.fixedexpense.api.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ArchiveFixedExpenseTemplateRequest(@NotNull LocalDate archivedFromMonth) {}
