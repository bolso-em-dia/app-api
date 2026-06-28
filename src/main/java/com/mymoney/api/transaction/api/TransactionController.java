package com.mymoney.api.transaction.api;

import com.mymoney.api.transaction.DeleteScope;
import com.mymoney.api.transaction.OwnershipType;
import com.mymoney.api.transaction.TransactionService;
import com.mymoney.api.transaction.TransactionType;
import com.mymoney.api.transaction.api.request.CreateTransactionRequest;
import com.mymoney.api.transaction.api.request.UpdateTransactionRequest;
import com.mymoney.api.transaction.api.response.TransactionResponse;
import com.mymoney.api.transaction.mapper.TransactionMapper;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
    private final TransactionMapper transactionMapper;

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceMonth,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) OwnershipType ownershipType,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID memberId) {
        return ResponseEntity.ok(
                transactionService
                        .listByFilters(referenceMonth, type, ownershipType, accountId, categoryId, memberId)
                        .stream()
                        .map(transactionMapper::toResponse)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionMapper.toResponse(transactionService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<List<TransactionResponse>> create(@Valid @RequestBody CreateTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.create(request).stream()
                        .map(transactionMapper::toResponse)
                        .toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateTransactionRequest request) {
        return ResponseEntity.ok(transactionMapper.toResponse(transactionService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id, @RequestParam(defaultValue = "SINGLE") DeleteScope scope) {
        transactionService.delete(id, scope);
        return ResponseEntity.noContent().build();
    }
}
