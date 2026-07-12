create index if not exists idx_transactions_description_trgm
    on transactions using gin (f_unaccent_lower(description) gin_trgm_ops);

create index if not exists idx_family_members_name_trgm
    on family_members using gin (f_unaccent_lower(name) gin_trgm_ops);

create index if not exists idx_family_members_email_trgm
    on family_members using gin (f_unaccent_lower(email) gin_trgm_ops);

create index if not exists idx_budget_models_name_trgm
    on budget_models using gin (f_unaccent_lower(name) gin_trgm_ops);
