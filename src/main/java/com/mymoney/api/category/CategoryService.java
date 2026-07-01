package com.mymoney.api.category;

import com.mymoney.api.category.api.request.ArchiveCategoryRequest;
import com.mymoney.api.category.api.request.CreateCategoryRequest;
import com.mymoney.api.category.api.request.UpdateCategoryRequest;
import java.time.LocalDate;
import java.time.YearMonth;
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

    @Transactional(readOnly = true)
    public Page<Category> listAll(String search, CategoryListStatus status, Pageable pageable) {
        String normalizedSearch = normalizeSearch(search);
        return categoryRepository.findByFilters(normalizedSearch, status.name(), pageable);
    }

    @Transactional(readOnly = true)
    public Category getById(UUID id) {
        return categoryRepository
                .findWithAssociationsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category was not found."));
    }

    @Transactional
    public Category create(CreateCategoryRequest request) {
        Category category = new Category();
        category.setName(request.name().trim());
        category.setIcon(normalizeNullable(request.icon()));
        category.setColor(normalizeNullable(request.color()));
        category.setCreatedInMonth(currentReferenceMonth());
        return getById(categoryRepository.save(category).getId());
    }

    @Transactional
    public Category update(UUID id, UpdateCategoryRequest request) {
        Category category = getById(id);
        category.setName(request.name().trim());
        category.setIcon(normalizeNullable(request.icon()));
        category.setColor(normalizeNullable(request.color()));
        return getById(categoryRepository.save(category).getId());
    }

    @Transactional
    public Category archive(UUID id, ArchiveCategoryRequest request) {
        Category category = getById(id);
        Category replacementCategory = getById(request.replacementCategoryId());
        LocalDate archivedFromMonth = currentReferenceMonth();

        if (category.getId().equals(replacementCategory.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Replacement category must be different from the archived one.");
        }

        if (archivedFromMonth.isBefore(category.getCreatedInMonth())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Archive month cannot be before the category creation month.");
        }

        category.setArchivedFromMonth(archivedFromMonth);
        category.setReplacementCategory(replacementCategory);
        return getById(categoryRepository.save(category).getId());
    }

    @Transactional(readOnly = true)
    public List<Category> listOptions(LocalDate referenceMonth) {
        return categoryRepository.findAvailableForMonth(referenceMonth);
    }

    private LocalDate currentReferenceMonth() {
        return YearMonth.now().atDay(1);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
