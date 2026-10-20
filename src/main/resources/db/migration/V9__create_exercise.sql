create table exercise (
  id uuid primary key default gen_random_uuid(),
  name varchar(160) not null,
  description text,
  created_by uuid references user_account(id),
  is_common boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
