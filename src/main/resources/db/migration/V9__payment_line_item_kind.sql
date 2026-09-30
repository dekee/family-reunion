-- Line items now carry what kind of money they are, replacing the guest_name = 'Angel Contribution'
-- string sentinel. Donation checkouts put a donation row and attendee rows on the same payment, so
-- "is this a person?" can no longer be derived from the name, and fee/shirt/donation revenue has to
-- be reportable separately.
-- Idempotent in the style of V8: safe to re-run, and the backfill only touches rows with no kind yet.
ALTER TABLE payment_line_items ADD COLUMN IF NOT EXISTS kind VARCHAR(20);

-- Every pre-existing row is either an angel gift or somebody who paid their full fee.
UPDATE payment_line_items SET kind = 'ANGEL' WHERE kind IS NULL AND guest_name = 'Angel Contribution';
UPDATE payment_line_items SET kind = 'FEE'   WHERE kind IS NULL;

ALTER TABLE payment_line_items ALTER COLUMN kind SET DEFAULT 'FEE';
ALTER TABLE payment_line_items ALTER COLUMN kind SET NOT NULL;
