package com.mymoney.api.envelope;

import java.math.BigDecimal;

public record EnvelopeView(EnvelopeModel envelopeModel, BigDecimal consumedAmount, BigDecimal remainingAmount) {}
