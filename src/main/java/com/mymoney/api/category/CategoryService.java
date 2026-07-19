package com.mymoney.api.category;

import com.mymoney.api.audit.AuditorResolver;
import com.mymoney.api.category.api.request.ArchiveCategoryRequest;
import com.mymoney.api.category.api.request.CreateCategoryRequest;
import com.mymoney.api.category.api.request.UpdateCategoryRequest;
import com.mymoney.api.category.api.response.CategoryResponse;
import com.mymoney.api.error.CodedResponseStatusException;
import com.mymoney.api.error.ErrorCode;
import com.mymoney.api.shared.DateProvider;
import com.mymoney.api.shared.EntityResolver;
import com.mymoney.api.shared.InputNormalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuditorResolver auditorResolver;
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
                .orElseThrow(
                        () -> new CodedResponseStatusException(HttpStatus.NOT_FOUND, ErrorCode.CATEGORY_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Category getById(UUID id) {
        return EntityResolver.resolveOrThrow(() -> categoryRepository.findById(id), ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        var category = new Category();
        category.setName(InputNormalizer.requireNonBlank(request.name(), "Name"));
        category.setIcon(InputNormalizer.normalizeNullable(request.icon()));
        category.setColor(InputNormalizer.normalizeNullable(request.color()));
        category.setCreatedInMonth(dateProvider.currentReferenceMonth());
        var saved = categoryRepository.save(category);
        log.info(
                "Category created: id={}, name={}, memberId={}",
                saved.getId(),
                saved.getName(),
                auditorResolver.resolveMemberId());
        return getResponseById(saved.getId());
    }

    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        var category = getById(id);
        category.setName(InputNormalizer.requireNonBlank(request.name(), "Name"));
        category.setIcon(InputNormalizer.normalizeNullable(request.icon()));
        category.setColor(InputNormalizer.normalizeNullable(request.color()));
        var saved = categoryRepository.save(category);
        log.info(
                "Category updated: id={}, name={}, memberId={}",
                saved.getId(),
                saved.getName(),
                auditorResolver.resolveMemberId());
        return getResponseById(saved.getId());
    }

    @Transactional
    public CategoryResponse archive(UUID id, ArchiveCategoryRequest request) {
        var category = getById(id);
        var replacementCategory = getById(request.replacementCategoryId());
        var archivedFromMonth = dateProvider.currentReferenceMonth();

        if (category.getId().equals(replacementCategory.getId())) {
            throw new CodedResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.REPLACEMENT_CATEGORY_SAME);
        }

        if (replacementCategory.getArchivedFromMonth() != null) {
            throw new CodedResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.REPLACEMENT_CATEGORY_INACTIVE);
        }

        if (archivedFromMonth.isBefore(category.getCreatedInMonth())) {
            throw new CodedResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.ARCHIVE_BEFORE_CATEGORY_CREATION);
        }

        category.setArchivedFromMonth(archivedFromMonth);
        category.setReplacementCategory(replacementCategory);
        var saved = categoryRepository.save(category);
        log.info(
                "Category archived: id={}, replacementCategoryId={}, archivedFromMonth={}, memberId={}",
                saved.getId(),
                replacementCategory.getId(),
                saved.getArchivedFromMonth(),
                auditorResolver.resolveMemberId());
        return getResponseById(saved.getId());
    }

    @Transactional(readOnly = true)
    public List<Category> listOptions(LocalDate referenceMonth) {
        return categoryRepository.findAvailableForMonth(referenceMonth);
    }
}
