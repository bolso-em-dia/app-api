create index if not exists idx_family_members_lower_email
    on family_members (lower(email));
