package com.mymoney.api.category.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 120) String name, @Size(max = 80) String icon, @Size(max = 20) String color) {}
