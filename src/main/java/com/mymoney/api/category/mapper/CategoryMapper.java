package com.mymoney.api.category.mapper;

import com.mymoney.api.category.Category;
import com.mymoney.api.category.api.response.CategoryOptionResponse;
import com.mymoney.api.category.api.response.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId().toString(),
                category.getName(),
                category.getIcon(),
                category.getColor(),
                category.getCreatedInMonth(),
                category.getArchivedFromMonth(),
                category.getReplacementCategory() == null
                        ? null
                        : category.getReplacementCategory().getId().toString(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    public CategoryOptionResponse toOptionResponse(Category category) {
        return new CategoryOptionResponse(
                category.getId().toString(), category.getName(), category.getIcon(), category.getColor());
    }
}
