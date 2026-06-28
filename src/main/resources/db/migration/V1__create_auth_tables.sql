create table if not exists family_members (
    id uuid primary key,
    name varchar(120) not null,
    email varchar(160) not null unique,
    password_hash varchar(120) not null,
    role varchar(20) not null,
    active boolean not null default true,
    allowance_enabled boolean not null default false,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table if not exists refresh_tokens (
    id uuid primary key,
    member_id uuid not null references family_members(id),
    token_hash varchar(64) not null unique,
    expires_at timestamp with time zone not null,
    revoked_at timestamp with time zone,
    created_at timestamp with time zone not null
);

create index if not exists idx_refresh_tokens_member_id on refresh_tokens(member_id);
