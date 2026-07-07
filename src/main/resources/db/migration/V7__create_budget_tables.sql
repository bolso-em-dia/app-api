create table if not exists budget_models (
    id uuid primary key,
    name varchar(120) not null,
    type varchar(20) not null,
    owner_member_id uuid references family_members(id),
    monthly_limit numeric(14,2) not null,
    created_in_month date not null,
    archived_from_month date,
    active boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table if not exists budget_model_categories (
    budget_model_id uuid not null references budget_models(id),
    category_id uuid not null references categories(id),
    primary key (budget_model_id, category_id)
);

create index if not exists idx_budget_models_owner_member_id on budget_models(owner_member_id);
create index if not exists idx_budget_models_created_in_month on budget_models(created_in_month);
create index if not exists idx_budget_models_archived_from_month on budget_models(archived_from_month);
