create index if not exists idx_accounts_lower_name
    on accounts (lower(trim(name)));

create index if not exists idx_categories_lower_name
    on categories (lower(trim(name)));

alter table accounts
    add constraint uq_accounts_name unique (name);

alter table categories
    add constraint uq_categories_name unique (name);
