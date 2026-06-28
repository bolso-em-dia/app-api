package com.mymoney.api.envelope;

import java.math.BigDecimal;

public record EnvelopeCategoryBreakdownItem(String categoryId, String categoryName, BigDecimal amount) {}
