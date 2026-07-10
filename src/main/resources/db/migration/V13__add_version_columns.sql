alter table accounts
    add column if not exists version bigint not null default 0;

alter table budget_models
    add column if not exists version bigint not null default 0;

alter table categories
    add column if not exists version bigint not null default 0;

alter table family_members
    add column if not exists version bigint not null default 0;

alter table fixed_expense_templates
    add column if not exists version bigint not null default 0;

alter table member_preferences
    add column if not exists version bigint not null default 0;

alter table refresh_tokens
    add column if not exists version bigint not null default 0;

alter table transactions
    add column if not exists version bigint not null default 0;
