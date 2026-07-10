alter table transactions
    add column if not exists original_amount numeric(14, 2),
    add column if not exists currency varchar(3);
