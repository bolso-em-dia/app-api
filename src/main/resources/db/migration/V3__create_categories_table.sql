create table if not exists categories (
    id uuid primary key,
    name varchar(120) not null,
    icon varchar(80),
    color varchar(20),
    created_in_month date not null,
    archived_from_month date,
    replacement_category_id uuid references categories(id),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index if not exists idx_categories_created_in_month on categories(created_in_month);
create index if not exists idx_categories_archived_from_month on categories(archived_from_month);
