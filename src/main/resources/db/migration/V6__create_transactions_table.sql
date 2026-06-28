create table if not exists transactions (
    id uuid primary key,
    type varchar(20) not null,
    ownership_type varchar(20) not null,
    source_type varchar(20) not null,
    description varchar(160) not null,
    amount numeric(14,2) not null,
    transaction_date date not null,
    reference_month date not null,
    account_id uuid not null references accounts(id),
    category_id uuid not null references categories(id),
    member_id uuid references family_members(id),
    installment_group_id uuid,
    installment_number smallint,
    installment_total smallint,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index if not exists idx_transactions_reference_month on transactions(reference_month);
create index if not exists idx_transactions_account_id on transactions(account_id);
create index if not exists idx_transactions_category_id on transactions(category_id);
create index if not exists idx_transactions_member_id on transactions(member_id);
create index if not exists idx_transactions_installment_group_id on transactions(installment_group_id);
