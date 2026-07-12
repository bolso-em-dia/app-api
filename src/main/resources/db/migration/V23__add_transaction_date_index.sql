create index if not exists idx_transactions_transaction_date_created_at
    on transactions(transaction_date, created_at);
