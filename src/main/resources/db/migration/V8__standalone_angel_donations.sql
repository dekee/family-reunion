-- Standalone Angel Fund gifts: a payment can now exist with no RSVP behind it.
-- Idempotent: DROP NOT NULL on an already-nullable column is a no-op in Postgres.
ALTER TABLE payments ALTER COLUMN rsvp_id DROP NOT NULL;

-- Donor-supplied attribution for the public Angel Fund leaderboard. Deliberately separate from
-- payer_name/payer_email: the Stripe webhook overwrites those from customer_details on every
-- completed checkout, so they cannot hold a donor's chosen display name. Nullable because
-- in-branch angel gifts and all pre-existing payments have no donor-supplied attribution.
ALTER TABLE payments ADD COLUMN IF NOT EXISTS donor_name VARCHAR(80);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS donor_family_label VARCHAR(80);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS donor_anonymous BOOLEAN NOT NULL DEFAULT FALSE;
-- Moderation escape hatch for the public page (admin UI is a follow-up; the column is free now
-- and avoids a second migration under pressure if someone posts something abusive).
ALTER TABLE payments ADD COLUMN IF NOT EXISTS donor_hidden BOOLEAN NOT NULL DEFAULT FALSE;
