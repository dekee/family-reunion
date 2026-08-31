-- Backfill schema changes that were applied manually on OCI prod but never
-- captured as migrations, leaving other environments (homelab) unable to pass
-- Hibernate schema validation. Everything here is idempotent.

-- Added by "Add exclude-from-RSVP toggle for family members" (970bdc9)
ALTER TABLE family_members ADD COLUMN IF NOT EXISTS exclude_from_rsvp BOOLEAN NOT NULL DEFAULT FALSE;

-- Added by "Add T-Shirt Design vote page with two design candidates" (4a2a073);
-- design rows are seeded at startup by TshirtDesignInitializer.
CREATE TABLE IF NOT EXISTS tshirt_designs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    image_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tshirt_design_votes (
    id BIGSERIAL PRIMARY KEY,
    design_id BIGINT NOT NULL REFERENCES tshirt_designs(id) ON DELETE CASCADE,
    family_member_id BIGINT NOT NULL REFERENCES family_members(id) ON DELETE CASCADE,
    voted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(family_member_id)
);
