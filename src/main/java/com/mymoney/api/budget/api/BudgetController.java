package com.mymoney.api.budget.api;

import com.mymoney.api.PageResponse;
import com.mymoney.api.budget.BudgetListStatus;
import com.mymoney.api.budget.BudgetService;
import com.mymoney.api.budget.BudgetType;
import com.mymoney.api.budget.api.request.ArchiveBudgetRequest;
import com.mymoney.api.budget.api.request.CreateBudgetRequest;
import com.mymoney.api.budget.api.request.UpdateBudgetRequest;
import com.mymoney.api.budget.api.response.BudgetCategoryBreakdownResponse;
import com.mymoney.api.budget.api.response.BudgetResponse;
import com.mymoney.api.budget.mapper.BudgetMapper;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/budgets")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final BudgetMapper budgetMapper;

    @GetMapping
    public ResponseEntity<PageResponse<BudgetResponse>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceMonth,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ACTIVE") BudgetListStatus status,
            @RequestParam(required = false) BudgetType type,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(budgetService
                .listForMonth(referenceMonth, search, status, type, pageable)
                .map(view -> budgetMapper.toResponse(view, List.of()))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getById(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceMonth) {
        return ResponseEntity.ok(budgetMapper.toResponse(
                budgetService.getViewById(id, referenceMonth),
                budgetMapper.toTransactionResponses(budgetService.listTransactions(id, referenceMonth))));
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> create(@Valid @RequestBody CreateBudgetRequest request) {
        var budget = budgetService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetMapper.toResponse(
                        budgetService.getViewById(budget.getId(), budget.getCreatedInMonth()), List.of()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateBudgetRequest request) {
        var budget = budgetService.update(id, request);
        return ResponseEntity.ok(budgetMapper.toResponse(
                budgetService.getViewById(budget.getId(), budget.getCreatedInMonth()), List.of()));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<BudgetResponse> archive(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ArchiveBudgetRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceMonth) {
        var budget = budgetService.archive(id, request == null ? new ArchiveBudgetRequest() : request, referenceMonth);
        return ResponseEntity.ok(
                budgetMapper.toResponse(budgetService.getViewById(budget.getId(), referenceMonth), List.of()));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<com.mymoney.api.transaction.api.response.TransactionResponse>> transactions(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceMonth) {
        return ResponseEntity.ok(
                budgetMapper.toTransactionResponses(budgetService.listTransactions(id, referenceMonth)));
    }

    @GetMapping("/{id}/category-breakdown")
    public ResponseEntity<List<BudgetCategoryBreakdownResponse>> categoryBreakdown(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceMonth) {
        return ResponseEntity.ok(
                budgetMapper.toCategoryBreakdownResponses(budgetService.categoryBreakdown(id, referenceMonth)));
    }
}
