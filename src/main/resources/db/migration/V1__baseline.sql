-- Baseline migration: captures existing schema as of 2026-03-13

CREATE TABLE IF NOT EXISTS family_members (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    age_group VARCHAR(20) NOT NULL DEFAULT 'ADULT',
    parent_id BIGINT REFERENCES family_members(id),
    generation INTEGER,
    is_founder BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS rsvps (
    id BIGSERIAL PRIMARY KEY,
    family_name VARCHAR(255) NOT NULL,
    head_of_household_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(255),
    needs_lodging BOOLEAN NOT NULL DEFAULT FALSE,
    arrival_date DATE,
    departure_date DATE,
    notes TEXT
);

CREATE TABLE IF NOT EXISTS attendees (
    id BIGSERIAL PRIMARY KEY,
    rsvp_id BIGINT NOT NULL REFERENCES rsvps(id),
    family_member_id BIGINT REFERENCES family_members(id),
    guest_name VARCHAR(255),
    guest_age_group VARCHAR(20),
    dietary_needs VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    rsvp_id BIGINT NOT NULL REFERENCES rsvps(id),
    amount NUMERIC(10,2) NOT NULL,
    stripe_session_id VARCHAR(255) NOT NULL UNIQUE,
    stripe_payment_intent_id VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    checkin_token VARCHAR(255) NOT NULL UNIQUE,
    checked_in BOOLEAN NOT NULL DEFAULT FALSE,
    checked_in_at TIMESTAMP,
    payer_name VARCHAR(255),
    payer_email VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS payment_line_items (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id),
    family_member_id BIGINT,
    family_member_name VARCHAR(255),
    guest_name VARCHAR(255),
    age_group VARCHAR(20) NOT NULL DEFAULT 'ADULT',
    amount NUMERIC(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    event_date_time TIMESTAMP NOT NULL,
    address VARCHAR(255) NOT NULL,
    host_name VARCHAR(255),
    notes TEXT
);

CREATE TABLE IF NOT EXISTS event_registrations (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id),
    family_member_id BIGINT NOT NULL REFERENCES family_members(id),
    UNIQUE(event_id, family_member_id)
);

CREATE TABLE IF NOT EXISTS meetings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    meeting_date_time TIMESTAMP NOT NULL,
    zoom_link VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255),
    meeting_id VARCHAR(255),
    passcode VARCHAR(255),
    notes TEXT
);

CREATE TABLE IF NOT EXISTS admin_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
