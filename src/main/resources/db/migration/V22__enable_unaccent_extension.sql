-- Enable unaccent extension for accent-insensitive search
create extension if not exists unaccent;

-- Combines lower() and unaccent() for accent-and-case-insensitive search
create or replace function f_unaccent_lower(text) returns text
    language sql immutable strict parallel safe
    return lower(unaccent($1));
