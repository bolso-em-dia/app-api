create table if not exists fixed_expense_templates (
    id uuid primary key,
    name varchar(120) not null,
    amount numeric(14,2) not null,
    category_id uuid not null references categories(id),
    account_id uuid not null references accounts(id),
    due_day smallint not null,
    created_in_month date not null,
    archived_from_month date,
    active boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index if not exists idx_fixed_expense_templates_category_id on fixed_expense_templates(category_id);
create index if not exists idx_fixed_expense_templates_account_id on fixed_expense_templates(account_id);
create index if not exists idx_fixed_expense_templates_created_in_month on fixed_expense_templates(created_in_month);
create index if not exists idx_fixed_expense_templates_archived_from_month on fixed_expense_templates(archived_from_month);
