create table user_roles (
  user_id uuid not null references user_account(id) on delete cascade,
  role user_role not null constraint fk_user_roles_role references roles(name),
  primary key (user_id, role)
);
