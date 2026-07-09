alter table fixed_expense_templates
    add column if not exists type varchar(20) not null default 'EXPENSE';
