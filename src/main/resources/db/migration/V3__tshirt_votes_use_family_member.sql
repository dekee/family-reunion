-- Switch from voter_name to family_member_id (no votes exist yet)
DROP TABLE IF EXISTS tshirt_votes;

CREATE TABLE tshirt_votes (
    id BIGSERIAL PRIMARY KEY,
    slogan_id BIGINT NOT NULL REFERENCES tshirt_slogans(id) ON DELETE CASCADE,
    family_member_id BIGINT NOT NULL REFERENCES family_members(id) ON DELETE CASCADE,
    voted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(family_member_id)
);
