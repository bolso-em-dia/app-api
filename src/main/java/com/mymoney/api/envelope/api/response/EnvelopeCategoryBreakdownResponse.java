package com.mymoney.api.envelope.api.response;

import java.math.BigDecimal;

public record EnvelopeCategoryBreakdownResponse(String categoryId, String categoryName, BigDecimal amount) {}
