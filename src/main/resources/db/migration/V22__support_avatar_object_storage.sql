-- The deployed user_avatar table is empty; avatar content is now stored in MinIO.
alter table user_avatar
    drop column content,
    add column object_key varchar(255) not null,
    add column file_size bigint not null check (file_size between 1 and 5242880);
