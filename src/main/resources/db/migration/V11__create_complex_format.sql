create table complex_format (
  id integer primary key,
  code varchar(40) not null unique,
  name varchar(120) not null,
  description text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
