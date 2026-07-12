create index if not exists idx_budget_models_active
    on budget_models(created_in_month, archived_from_month)
    where archived_from_month is null;

create index if not exists idx_fixed_expense_templates_active
    on fixed_expense_templates(created_in_month, archived_from_month)
    where archived_from_month is null;
