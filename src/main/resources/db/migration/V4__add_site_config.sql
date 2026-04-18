CREATE TABLE IF NOT EXISTS site_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS site_images (
    image_key VARCHAR(50) PRIMARY KEY,
    content_type VARCHAR(100) NOT NULL,
    data BYTEA NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
