package com.mymoney.api.transaction.api;

import com.mymoney.api.PageResponse;
import com.mymoney.api.transaction.DeleteScope;
import com.mymoney.api.transaction.OwnershipType;
import com.mymoney.api.transaction.TransactionService;
import com.mymoney.api.transaction.TransactionType;
import com.mymoney.api.transaction.api.request.CreateTransactionRequest;
import com.mymoney.api.transaction.api.request.UpdateTransactionRequest;
import com.mymoney.api.transaction.api.response.TransactionResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/transactions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<PageResponse<TransactionResponse>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceMonth,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) OwnershipType ownershipType,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) List<UUID> categoryIds,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID memberId,
            @PageableDefault(size = 20)
                    @SortDefault.SortDefaults({@SortDefault(sort = "transactionDate"), @SortDefault(sort = "createdAt")
                    })
                    Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(transactionService.listResponseByFilters(
                referenceMonth,
                type,
                ownershipType,
                accountId,
                normalizeCategoryIds(categoryIds, categoryId),
                memberId,
                pageable)));
    }

    private List<UUID> normalizeCategoryIds(List<UUID> categoryIds, UUID categoryId) {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            return categoryIds;
        }

        return categoryId == null ? null : List.of(categoryId);
    }

    @GetMapping("/descriptions")
    public ResponseEntity<List<String>> listDescriptionSuggestions(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(transactionService.listDescriptionSuggestions(query, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getResponseById(id));
    }

    @PostMapping
    public ResponseEntity<List<TransactionResponse>> create(@Valid @RequestBody CreateTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateTransactionRequest request) {
        return ResponseEntity.ok(transactionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id, @RequestParam(defaultValue = "SINGLE") DeleteScope scope) {
        transactionService.delete(id, scope);
        return ResponseEntity.noContent().build();
    }
}
