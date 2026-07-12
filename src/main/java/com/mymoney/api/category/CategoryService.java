package com.mymoney.api.category;

import com.mymoney.api.category.api.request.ArchiveCategoryRequest;
import com.mymoney.api.category.api.request.CreateCategoryRequest;
import com.mymoney.api.category.api.request.UpdateCategoryRequest;
import com.mymoney.api.category.api.response.CategoryResponse;
import com.mymoney.api.shared.DateProvider;
import com.mymoney.api.shared.EntityResolver;
import com.mymoney.api.shared.ErrorMessage;
import com.mymoney.api.shared.InputNormalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final DateProvider dateProvider;

    @Transactional(readOnly = true)
    public Page<CategoryResponse> listAllResponses(String search, CategoryListStatus status, Pageable pageable) {
        String normalizedSearch = InputNormalizer.normalizeSearch(search);
        return categoryRepository.findResponseByFilters(normalizedSearch, status.name(), pageable);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getResponseById(UUID id) {
        return categoryRepository
                .findResponseById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessage.CATEGORY_NOT_FOUND.message()));
    }

    @Transactional(readOnly = true)
    public Category getById(UUID id) {
        return EntityResolver.resolveOrThrow(
                () -> categoryRepository.findById(id), ErrorMessage.CATEGORY_NOT_FOUND.message());
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        Category category = new Category();
        category.setName(InputNormalizer.requireNonBlank(request.name(), "Name"));
        category.setIcon(InputNormalizer.normalizeNullable(request.icon()));
        category.setColor(InputNormalizer.normalizeNullable(request.color()));
        category.setCreatedInMonth(dateProvider.currentReferenceMonth());
        Category saved = categoryRepository.save(category);
        return getResponseById(saved.getId());
    }

    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        Category category = getById(id);
        category.setName(InputNormalizer.requireNonBlank(request.name(), "Name"));
        category.setIcon(InputNormalizer.normalizeNullable(request.icon()));
        category.setColor(InputNormalizer.normalizeNullable(request.color()));
        Category saved = categoryRepository.save(category);
        return getResponseById(saved.getId());
    }

    @Transactional
    public CategoryResponse archive(UUID id, ArchiveCategoryRequest request) {
        Category category = getById(id);
        Category replacementCategory = getById(request.replacementCategoryId());
        LocalDate archivedFromMonth = dateProvider.currentReferenceMonth();

        if (category.getId().equals(replacementCategory.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Replacement category must be different from the archived one.");
        }

        if (replacementCategory.getArchivedFromMonth() != null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Replacement category must be active.");
        }

        if (archivedFromMonth.isBefore(category.getCreatedInMonth())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Archive month cannot be before the category creation month.");
        }

        category.setArchivedFromMonth(archivedFromMonth);
        category.setReplacementCategory(replacementCategory);
        Category saved = categoryRepository.save(category);
        return getResponseById(saved.getId());
    }

    @Transactional(readOnly = true)
    public List<Category> listOptions(LocalDate referenceMonth) {
        return categoryRepository.findAvailableForMonth(referenceMonth);
    }
}
