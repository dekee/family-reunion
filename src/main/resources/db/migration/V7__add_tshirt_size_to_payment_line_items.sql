-- T-shirt size per paid person, stored on the payment line item.
-- Nullable: line items created before this feature have no size and are backfilled by attendees themselves.
ALTER TABLE payment_line_items ADD COLUMN IF NOT EXISTS tshirt_size VARCHAR(20);
