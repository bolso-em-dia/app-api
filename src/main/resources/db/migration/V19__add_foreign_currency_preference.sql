alter table member_preferences
    add column if not exists show_foreign_currency boolean not null default false;
