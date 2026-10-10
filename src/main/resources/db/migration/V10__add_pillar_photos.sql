CREATE TABLE IF NOT EXISTS pillar_photos (
    sibling_id BIGINT PRIMARY KEY REFERENCES family_members(id) ON DELETE CASCADE,
    content_type VARCHAR(100) NOT NULL,
    data BYTEA NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
