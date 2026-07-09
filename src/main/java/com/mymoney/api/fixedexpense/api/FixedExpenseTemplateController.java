package com.mymoney.api.fixedexpense.api;

import com.mymoney.api.PageResponse;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateListStatus;
import com.mymoney.api.fixedexpense.FixedExpenseTemplateService;
import com.mymoney.api.fixedexpense.api.request.CreateFixedExpenseTemplateRequest;
import com.mymoney.api.fixedexpense.api.request.UpdateFixedExpenseTemplateRequest;
import com.mymoney.api.fixedexpense.api.response.FixedExpenseTemplateResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/fixed-transactions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class FixedExpenseTemplateController {

    private final FixedExpenseTemplateService fixedExpenseTemplateService;

    @GetMapping
    public ResponseEntity<PageResponse<FixedExpenseTemplateResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ACTIVE") FixedExpenseTemplateListStatus status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(fixedExpenseTemplateService.listAllResponses(search, status, pageable)));
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

    @PatchMapping("/{id}/archive")
    public ResponseEntity<FixedExpenseTemplateResponse> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(fixedExpenseTemplateService.archive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fixedExpenseTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
