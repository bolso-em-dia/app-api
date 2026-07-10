create table if not exists exchange_rates (
    id uuid primary key,
    currency varchar(3) not null,
    rate numeric(14, 6) not null,
    fetched_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    version bigint not null default 0
);

create index if not exists idx_exchange_rates_currency_fetched_at
    on exchange_rates (currency, fetched_at desc);
