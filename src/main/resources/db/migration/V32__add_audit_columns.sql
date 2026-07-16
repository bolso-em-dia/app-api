alter table accounts
    add column created_by uuid references family_members(id),
    add column updated_by uuid references family_members(id);

alter table transactions
    add column created_by uuid references family_members(id),
    add column updated_by uuid references family_members(id);

alter table categories
    add column created_by uuid references family_members(id),
    add column updated_by uuid references family_members(id);

alter table budget_models
    add column created_by uuid references family_members(id),
    add column updated_by uuid references family_members(id);

alter table fixed_expense_templates
    add column created_by uuid references family_members(id),
    add column updated_by uuid references family_members(id);

alter table family_members
    add column created_by uuid references family_members(id),
    add column updated_by uuid references family_members(id);

alter table member_preferences
    add column created_by uuid references family_members(id),
    add column updated_by uuid references family_members(id);
