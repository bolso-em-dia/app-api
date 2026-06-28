package com.mymoney.api.category.api.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ArchiveCategoryRequest(@NotNull LocalDate archivedFromMonth, @NotNull UUID replacementCategoryId) {}
