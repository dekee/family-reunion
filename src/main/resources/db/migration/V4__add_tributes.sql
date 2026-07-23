CREATE TABLE IF NOT EXISTS tributes (
    id BIGSERIAL PRIMARY KEY,
    sibling_id BIGINT NOT NULL REFERENCES family_members(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES family_members(id) ON DELETE CASCADE,
    story VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (sibling_id, author_id)
);
