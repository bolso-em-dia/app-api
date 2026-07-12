-- Adiciona colunas do novo modelo de moeda
alter table transactions
    add column if not exists converted_amount numeric(14, 2),
    add column if not exists exchange_rate numeric(14, 6);

-- Popula converted_amount com o valor atual de amount (que é BRL)
update transactions
set converted_amount = amount
where converted_amount is null;

-- Calcula exchange_rate para transações USD existentes
update transactions
set exchange_rate = amount / original_amount
where currency = 'USD'
  and original_amount is not null
  and original_amount > 0
  and exchange_rate is null;

-- Move original_amount para amount (agora amount = valor na moeda original)
update transactions
set amount = original_amount
where currency = 'USD'
  and original_amount is not null;

-- Preenche currency como BRL onde estava null
update transactions
set currency = 'BRL'
where currency is null;

-- Remove coluna original_amount
alter table transactions
    drop column if exists original_amount;
