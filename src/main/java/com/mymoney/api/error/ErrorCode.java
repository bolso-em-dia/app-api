package com.mymoney.api.error;

public enum ErrorCode {
    VALIDATION_FAILED(40001, "Request validation failed."),
    INVALID_REQUEST_BODY(40002, "Request body is invalid."),
    UNRECOGNIZED_FIELD(40003, "Request body contains unsupported field."),
    INVALID_PARAMETER(40004, "Invalid value for parameter."),
    INVALID_PAGE_SIZE(40005, "Limit must be positive."),
    DUE_DAY_REQUIRED(40006, "Due day is required."),
    FIELD_CANNOT_BE_EMPTY(40007, "Field cannot be empty."),

    NOT_AUTHENTICATED(40101, "Authentication is required."),
    INVALID_CREDENTIALS(40102, "Invalid email or password."),
    REFRESH_TOKEN_MISSING(40103, "Refresh token is missing."),
    REFRESH_TOKEN_INVALID(40104, "Refresh token is invalid."),
    ACCOUNT_DEACTIVATED(40105, "Account is no longer active."),
    AUTHENTICATION_REQUIRED(40106, "Authentication is required."),

    METHOD_ACCESS_DENIED(40301, "Access denied."),
    FILTER_ACCESS_DENIED(40302, "Access denied."),

    ACCOUNT_NOT_FOUND(40401, "Account was not found."),
    BUDGET_NOT_FOUND(40402, "Budget was not found."),
    CATEGORY_NOT_FOUND(40403, "Category was not found."),
    FAMILY_MEMBER_NOT_FOUND(40404, "Family member was not found."),
    FIXED_EXPENSE_TEMPLATE_NOT_FOUND(40405, "Fixed expense template was not found."),
    TRANSACTION_NOT_FOUND(40406, "Transaction was not found."),
    NO_EXCHANGE_RATE_DATA(40407, "No exchange rate data available."),

    EMAIL_ALREADY_IN_USE(40901, "Email is already in use."),
    DUPLICATE_ALLOWANCE_BUDGET(40902, "An allowance budget already exists for this member."),
    CONCURRENT_MODIFICATION(40903, "Resource was modified by another user."),

    ARCHIVE_BEFORE_ACCOUNT_CREATION(42201, "Archive month cannot be before the account creation month."),
    CREDIT_CARD_REQUIRES_DAYS(42202, "Credit cards require both closing day and due day."),
    NON_CREDIT_CARD_DAYS(42203, "Only credit cards accept closing day and due day."),
    REPLACEMENT_CATEGORY_SAME(42204, "Replacement category must be different from the archived one."),
    REPLACEMENT_CATEGORY_INACTIVE(42205, "Replacement category must be active."),
    ARCHIVE_BEFORE_CATEGORY_CREATION(42206, "Archive month cannot be before the category creation month."),
    ARCHIVE_BEFORE_BUDGET_CREATION(42207, "Archive month cannot be before the budget creation month."),
    ALLOWANCE_REQUIRES_OWNER(42208, "Allowance budgets require an owner member."),
    GLOBAL_BUDGET_REQUIRES_CATEGORY(42209, "Global budgets require at least one category."),
    INDIVIDUAL_TRANSACTION_REQUIRES_MEMBER(42210, "Individual transactions require a member."),
    INDIVIDUAL_TRANSACTION_REQUIRES_ALLOWANCE(
            42211, "Individual transactions require a valid allowance budget for the selected member."),
    INSTALLMENT_COUNT_RANGE(42212, "Installment count must be between 1 and 120."),
    INSTALLMENT_PLAN_TOO_LONG(42213, "Installment plan cannot exceed 2 years."),
    EXCHANGE_RATE_NOT_AVAILABLE(42214, "Exchange rate not available."),
    FOREIGN_CURRENCY_DISABLED(42215, "Foreign currency support is disabled."),
    INCORRECT_CURRENT_PASSWORD(42216, "Current password is incorrect."),
    PASSWORD_CONFIRMATION_MISMATCH(42217, "Password confirmation does not match."),
    UNSUPPORTED_LOCALE(42218, "Locale is not supported."),
    DEFAULT_ACCOUNT_INACTIVE(42219, "Default account must be active for the current month."),
    DAY_OUT_OF_RANGE(42220, "Day must be between 1 and 31."),

    INTERNAL_ERROR(50001, "Unexpected server error."),

    EXCHANGE_RATE_API_INVALID(50201, "Exchange rate API returned an invalid rate."),
    EXCHANGE_RATE_API_UNAVAILABLE(50202, "Exchange rate API unavailable.");

    private final int code;
    private final String description;

    ErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int code() {
        return code;
    }

    public String description() {
        return description;
    }
}
