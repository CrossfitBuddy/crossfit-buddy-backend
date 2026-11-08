create table user_avatar (
    user_id uuid primary key references user_profile(user_id) on delete cascade,
    content_type varchar(20) not null check (content_type in ('image/jpeg', 'image/png', 'image/webp')),
    content bytea not null check (octet_length(content) between 1 and 5242880),
    version bigint not null default 0
);
