package com.mymoney.api.exchangerate.api;

import com.mymoney.api.exchangerate.ExchangeRateResponse;
import com.mymoney.api.exchangerate.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchange-rate")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/latest")
    public ResponseEntity<ExchangeRateResponse> latest() {
        return ResponseEntity.ok(exchangeRateService.getLatest());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ExchangeRateResponse> refresh() {
        return ResponseEntity.ok(exchangeRateService.refreshManually());
    }
}
