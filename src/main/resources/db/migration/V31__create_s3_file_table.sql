CREATE TABLE s3_file (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255),
    type VARCHAR(255),
    size BIGINT,
    object_key VARCHAR(255),
    status VARCHAR(255),
    expire_at TIMESTAMP WITH TIME ZONE
);
