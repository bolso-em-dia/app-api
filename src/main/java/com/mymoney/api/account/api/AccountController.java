package com.mymoney.api.account.api;

import com.mymoney.api.PageResponse;
import com.mymoney.api.account.AccountListStatus;
import com.mymoney.api.account.AccountService;
import com.mymoney.api.account.AccountType;
import com.mymoney.api.account.api.request.CreateAccountRequest;
import com.mymoney.api.account.api.request.UpdateAccountRequest;
import com.mymoney.api.account.api.response.AccountOptionResponse;
import com.mymoney.api.account.api.response.AccountResponse;
import com.mymoney.api.account.mapper.AccountMapper;
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
@RequestMapping("/api/accounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    @GetMapping
    public ResponseEntity<PageResponse<AccountResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ACTIVE") AccountListStatus status,
            @RequestParam(required = false) AccountType type,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                accountService.listAll(search, status, type, pageable).map(accountMapper::toResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(accountMapper.toResponse(accountService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountMapper.toResponse(accountService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(accountMapper.toResponse(accountService.update(id, request)));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<AccountResponse> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(accountMapper.toResponse(accountService.archive(id)));
    }

    @GetMapping("/options")
    public ResponseEntity<List<AccountOptionResponse>> options(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceMonth) {
        return ResponseEntity.ok(accountService.listOptions(referenceMonth).stream()
                .map(accountMapper::toOptionResponse)
                .toList());
    }
}
