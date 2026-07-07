create table if not exists member_preferences (
    id uuid primary key,
    member_id uuid not null unique references family_members(id),
    default_account_id uuid references accounts(id),
    locale varchar(10) not null,
    show_balance_with_budgets boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index if not exists idx_member_preferences_default_account_id
    on member_preferences(default_account_id);
