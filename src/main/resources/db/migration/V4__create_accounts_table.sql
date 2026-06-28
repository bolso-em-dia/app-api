create table if not exists accounts (
    id uuid primary key,
    name varchar(120) not null,
    type varchar(30) not null,
    brand varchar(40),
    color varchar(20),
    closing_day smallint,
    due_day smallint,
    created_in_month date not null,
    archived_from_month date,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index if not exists idx_accounts_created_in_month on accounts(created_in_month);
create index if not exists idx_accounts_archived_from_month on accounts(archived_from_month);
