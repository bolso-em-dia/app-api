package com.mymoney.api.support;

import com.mymoney.api.category.Category;
import java.time.LocalDate;
import java.util.function.Consumer;

public final class CategoryTestFactory {

    private CategoryTestFactory() {}

    public static Category create() {
        return create(category -> {});
    }

    public static Category create(Consumer<Category> customizer) {
        var category = Category.builder()
                .name("Test Category")
                .createdInMonth(LocalDate.of(2026, 1, 1))
                .build();
        customizer.accept(category);
        return category;
    }
}
