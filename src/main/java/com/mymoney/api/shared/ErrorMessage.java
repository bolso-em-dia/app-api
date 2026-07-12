package com.mymoney.api.shared;

public enum ErrorMessage {
    ACCOUNT_NOT_FOUND("Account was not found."),
    BUDGET_NOT_FOUND("Budget was not found."),
    CATEGORY_NOT_FOUND("Category was not found."),
    EMAIL_ALREADY_IN_USE("Email is already in use."),
    FAMILY_MEMBER_NOT_FOUND("Family member was not found."),
    FIXED_EXPENSE_TEMPLATE_NOT_FOUND("Fixed expense template was not found."),
    TRANSACTION_NOT_FOUND("Transaction was not found.");

    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
