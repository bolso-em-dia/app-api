alter table transactions
    add column if not exists reference_month_policy varchar(20);

update transactions
set reference_month_policy = case
    when reference_month = date_trunc('month', transaction_date)::date then 'FORCE_CURRENT'
    when reference_month = (date_trunc('month', transaction_date) + interval '1 month')::date then 'FORCE_NEXT'
    else 'FORCE_CURRENT'
end
where reference_month_policy is null;

alter table transactions
    alter column reference_month_policy set not null;

alter table transactions
    alter column reference_month_policy set default 'AUTO';
