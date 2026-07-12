-- Adiciona colunas de conversão para templates de gastos fixos
alter table fixed_expense_templates
    add column if not exists converted_amount numeric(14, 2),
    add column if not exists exchange_rate numeric(14, 6);

-- Para templates BRL, converted_amount = amount
update fixed_expense_templates
set converted_amount = amount
where (currency = 'BRL' or currency is null)
  and converted_amount is null;
