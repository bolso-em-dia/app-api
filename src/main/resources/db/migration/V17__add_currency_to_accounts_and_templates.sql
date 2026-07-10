alter table accounts
    add column if not exists currency varchar(3) not null default 'BRL';

alter table fixed_expense_templates
    add column if not exists currency varchar(3) not null default 'BRL';
