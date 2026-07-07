package com.mymoney.api.preference.api.response;

public record UserPreferencesResponse(String defaultAccountId, String locale, boolean showBalanceWithBudgets) {}
