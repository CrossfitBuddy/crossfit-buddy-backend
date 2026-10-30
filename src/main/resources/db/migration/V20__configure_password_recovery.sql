delete from password_reset_code older
using password_reset_code newer
where older.user_id = newer.user_id
  and (older.created_at, older.id) < (newer.created_at, newer.id);

alter table password_reset_code
    add constraint uq_password_reset_code_user unique (user_id),
    drop constraint password_reset_code_failed_attempts_check,
    drop constraint password_reset_code_check,
    add constraint password_reset_code_failed_attempts_check check (failed_attempts >= 0),
    add constraint password_reset_code_expiration_check check (expires_at > created_at);
