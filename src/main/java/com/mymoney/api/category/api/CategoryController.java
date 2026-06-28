package com.mymoney.api.category.api;

import com.mymoney.api.category.CategoryService;
import com.mymoney.api.category.api.request.ArchiveCategoryRequest;
import com.mymoney.api.category.api.request.CreateCategoryRequest;
import com.mymoney.api.category.api.request.UpdateCategoryRequest;
import com.mymoney.api.category.api.response.CategoryOptionResponse;
import com.mymoney.api.category.api.response.CategoryResponse;
import com.mymoney.api.category.mapper.CategoryMapper;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list() {
        return ResponseEntity.ok(categoryService.listAll().stream()
                .map(categoryMapper::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryMapper.toResponse(categoryService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryMapper.toResponse(categoryService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryMapper.toResponse(categoryService.update(id, request)));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<CategoryResponse> archive(
            @PathVariable UUID id, @Valid @RequestBody ArchiveCategoryRequest request) {
        return ResponseEntity.ok(categoryMapper.toResponse(categoryService.archive(id, request)));
    }

    @GetMapping("/options")
    public ResponseEntity<List<CategoryOptionResponse>> options(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceMonth) {
        return ResponseEntity.ok(categoryService.listOptions(referenceMonth).stream()
                .map(categoryMapper::toOptionResponse)
                .toList());
    }
}
