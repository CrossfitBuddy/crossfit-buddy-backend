CREATE TABLE user_avatar (
    user_id UUID PRIMARY KEY REFERENCES user_profile(user_id) ON DELETE CASCADE,
    object_key VARCHAR(255) NOT NULL,
    content_type VARCHAR(20) NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    file_size BIGINT NOT NULL CHECK (file_size BETWEEN 1 AND 5242880),
    version BIGINT NOT NULL DEFAULT 0
);
