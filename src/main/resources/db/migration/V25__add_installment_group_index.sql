create index if not exists idx_transactions_installment_group_number
    on transactions(installment_group_id, installment_number);
