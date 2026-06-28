create table if not exists envelope_models (
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

create table if not exists envelope_model_categories (
    envelope_model_id uuid not null references envelope_models(id),
    category_id uuid not null references categories(id),
    primary key (envelope_model_id, category_id)
);

create index if not exists idx_envelope_models_owner_member_id on envelope_models(owner_member_id);
create index if not exists idx_envelope_models_created_in_month on envelope_models(created_in_month);
create index if not exists idx_envelope_models_archived_from_month on envelope_models(archived_from_month);
