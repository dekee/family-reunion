CREATE TABLE IF NOT EXISTS tshirt_slogans (
    id BIGSERIAL PRIMARY KEY,
    slogan VARCHAR(500) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tshirt_votes (
    id BIGSERIAL PRIMARY KEY,
    slogan_id BIGINT NOT NULL REFERENCES tshirt_slogans(id) ON DELETE CASCADE,
    voter_name VARCHAR(255) NOT NULL UNIQUE,
    voted_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed the slogans
INSERT INTO tshirt_slogans (slogan, created_at) VALUES
    ('Together We Tumblin, Together We Rise', NOW()),
    ('Tumblin Through Time, Stronger Every Year', NOW()),
    ('Rooted in Love, Growing as Tumblins', NOW()),
    ('One Name, One Love: The Tumblin Way', NOW()),
    ('Tumblin Together, Forever and Always', NOW()),
    ('Generations of Greatness: The Tumblin Legacy', NOW()),
    ('Family First, Tumblin Always', NOW()),
    ('Where Love Runs Deep: The Tumblin Family', NOW()),
    ('Stronger Bonds, Proud Tumblins', NOW()),
    ('The Tumblin Tradition: Love, Laughter, Legacy', NOW()),
    ('Where Them Tumblins At', NOW()),
    ('One Tumblin, One Team', NOW()),
    ('Tumblin Family, Many Branches', NOW()),
    ('One Family, Many Branches', NOW()),
    ('One Tumblin Family, Many Branches', NOW());
