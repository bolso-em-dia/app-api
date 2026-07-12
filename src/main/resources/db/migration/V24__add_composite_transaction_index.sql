create index if not exists idx_transactions_ref_month_date_created
    on transactions(reference_month, transaction_date, created_at);
