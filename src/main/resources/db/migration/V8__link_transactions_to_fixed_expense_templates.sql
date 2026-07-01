alter table transactions
    add column if not exists fixed_expense_template_id uuid references fixed_expense_templates(id);

create index if not exists idx_transactions_fixed_expense_template_id
    on transactions(fixed_expense_template_id);
