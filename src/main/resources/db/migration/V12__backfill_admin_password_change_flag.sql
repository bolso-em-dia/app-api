update family_members
set must_change_password = true
where role = 'ADMIN';
