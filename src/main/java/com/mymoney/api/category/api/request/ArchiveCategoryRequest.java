package com.mymoney.api.category.api.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ArchiveCategoryRequest(@NotNull UUID replacementCategoryId) {}
