update transactions
set currency = 'BRL'
where currency is null;

alter table transactions
    alter column currency set not null;
