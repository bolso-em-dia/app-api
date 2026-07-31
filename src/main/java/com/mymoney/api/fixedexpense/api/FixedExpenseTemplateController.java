package com.mymoney.api.fixedexpense.api;

import com.mymoney.api.PageResponse;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateListSortBy;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateListStatus;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateService;
import com.mymoney.api.fixedexpense.api.request.CreateFixedExpenseTemplateRequest;
import com.mymoney.api.fixedexpense.api.request.UpdateFixedExpenseTemplateRequest;
import com.mymoney.api.fixedexpense.api.response.FixedExpenseTemplateResponse;
import com.mymoney.api.shared.ApiSortDirection;
import com.mymoney.api.shared.PageableSortResolver;
import com.mymoney.api.transaction.TransactionType;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fixed-transactions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class FixedExpenseTemplateController {

    private final FixedExpenseTemplateService fixedExpenseTemplateService;

    @GetMapping
    public ResponseEntity<PageResponse<FixedExpenseTemplateResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ACTIVE") FixedExpenseTemplateListStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) List<UUID> categoryIds,
            @RequestParam(required = false) FixedExpenseTemplateListSortBy sortBy,
            @RequestParam(required = false) ApiSortDirection sortDir,
            @PageableDefault(size = 20) Pageable pageable) {
        var sortedPageable =
                PageableSortResolver.resolve(pageable, sortBy, FixedExpenseTemplateListSortBy.NAME, sortDir);
        return ResponseEntity.ok(PageResponse.from(fixedExpenseTemplateService.listAllResponses(
                search, status, type, accountId, normalizeCategoryIds(categoryIds), sortedPageable)));
    }

    private List<UUID> normalizeCategoryIds(List<UUID> categoryIds) {
        return categoryIds == null || categoryIds.isEmpty() ? null : categoryIds;
    }

    @GetMapping("/{id}")
    public ResponseEntity<FixedExpenseTemplateResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(fixedExpenseTemplateService.getResponseById(id));
    }

    @PostMapping
    public ResponseEntity<FixedExpenseTemplateResponse> create(
            @Valid @RequestBody CreateFixedExpenseTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fixedExpenseTemplateService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FixedExpenseTemplateResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateFixedExpenseTemplateRequest request) {
        return ResponseEntity.ok(fixedExpenseTemplateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fixedExpenseTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
