package com.mymoney.api.category.mapper;

import com.mymoney.api.category.Category;
import com.mymoney.api.category.api.response.CategoryOptionResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryOptionResponse toOptionResponse(Category category) {
        return new CategoryOptionResponse(
                category.getId().toString(), category.getName(), category.getIcon(), category.getColor());
    }
}
