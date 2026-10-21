create table stage_format (
  id integer primary key,
  code varchar(40) not null unique,
  name varchar(120) not null,
  description text,
  position integer not null check (position >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
