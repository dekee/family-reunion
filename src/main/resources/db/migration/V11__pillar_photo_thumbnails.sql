-- Pillar photos now keep the full-size original in `data`; a small display copy for the page goes
-- here. Nullable because photos uploaded before this were already shrunk and have no separate copy.
ALTER TABLE pillar_photos ADD COLUMN IF NOT EXISTS thumbnail BYTEA;
