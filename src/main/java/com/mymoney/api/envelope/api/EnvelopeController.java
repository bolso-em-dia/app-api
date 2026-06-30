package com.mymoney.api.envelope.api;

import com.mymoney.api.PageResponse;
import com.mymoney.api.envelope.EnvelopeListStatus;
import com.mymoney.api.envelope.EnvelopeService;
import com.mymoney.api.envelope.EnvelopeType;
import com.mymoney.api.envelope.api.request.ArchiveEnvelopeRequest;
import com.mymoney.api.envelope.api.request.CreateEnvelopeRequest;
import com.mymoney.api.envelope.api.request.UpdateEnvelopeRequest;
import com.mymoney.api.envelope.api.response.EnvelopeCategoryBreakdownResponse;
import com.mymoney.api.envelope.api.response.EnvelopeResponse;
import com.mymoney.api.envelope.mapper.EnvelopeMapper;
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
@RequestMapping("/api/envelopes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class EnvelopeController {

    private final EnvelopeService envelopeService;
    private final EnvelopeMapper envelopeMapper;

    @GetMapping
    public ResponseEntity<PageResponse<EnvelopeResponse>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceMonth,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ACTIVE") EnvelopeListStatus status,
            @RequestParam(required = false) EnvelopeType type,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(envelopeService
                .listForMonth(referenceMonth, search, status, type, pageable)
                .map(view -> envelopeMapper.toResponse(view, List.of()))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnvelopeResponse> getById(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceMonth) {
        return ResponseEntity.ok(envelopeMapper.toResponse(
                envelopeService.getViewById(id, referenceMonth),
                envelopeMapper.toTransactionResponses(envelopeService.listTransactions(id, referenceMonth))));
    }

    @PostMapping
    public ResponseEntity<EnvelopeResponse> create(@Valid @RequestBody CreateEnvelopeRequest request) {
        var envelope = envelopeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(envelopeMapper.toResponse(
                        envelopeService.getViewById(envelope.getId(), envelope.getCreatedInMonth()), List.of()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EnvelopeResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateEnvelopeRequest request) {
        var envelope = envelopeService.update(id, request);
        return ResponseEntity.ok(envelopeMapper.toResponse(
                envelopeService.getViewById(envelope.getId(), envelope.getCreatedInMonth()), List.of()));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<EnvelopeResponse> archive(
            @PathVariable UUID id, @Valid @RequestBody ArchiveEnvelopeRequest request) {
        var envelope = envelopeService.archive(id, request);
        return ResponseEntity.ok(envelopeMapper.toResponse(
                envelopeService.getViewById(envelope.getId(), envelope.getCreatedInMonth()), List.of()));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<com.mymoney.api.transaction.api.response.TransactionResponse>> transactions(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceMonth) {
        return ResponseEntity.ok(
                envelopeMapper.toTransactionResponses(envelopeService.listTransactions(id, referenceMonth)));
    }

    @GetMapping("/{id}/category-breakdown")
    public ResponseEntity<List<EnvelopeCategoryBreakdownResponse>> categoryBreakdown(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceMonth) {
        return ResponseEntity.ok(
                envelopeMapper.toCategoryBreakdownResponses(envelopeService.categoryBreakdown(id, referenceMonth)));
    }
}
