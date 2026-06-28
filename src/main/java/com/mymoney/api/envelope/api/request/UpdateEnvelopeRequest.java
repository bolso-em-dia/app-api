package com.mymoney.api.envelope.api.request;

import com.mymoney.api.envelope.EnvelopeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateEnvelopeRequest(
        @NotBlank String name,
        @NotNull EnvelopeType type,
        UUID ownerMemberId,
        List<UUID> categoryIds,
        @NotNull @DecimalMin("0.01") BigDecimal monthlyLimit) {}
