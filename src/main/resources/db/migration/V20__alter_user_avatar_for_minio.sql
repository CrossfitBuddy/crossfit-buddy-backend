ALTER TABLE user_avatar DROP COLUMN content;
ALTER TABLE user_avatar ADD COLUMN object_key VARCHAR(255) NOT NULL;
ALTER TABLE user_avatar ADD COLUMN file_size BIGINT NOT NULL;
