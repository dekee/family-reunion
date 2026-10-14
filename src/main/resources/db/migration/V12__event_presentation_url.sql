-- Optional link to the presenter's Google Slides (or other) deck for each event.
ALTER TABLE events ADD COLUMN IF NOT EXISTS presentation_url VARCHAR(500);
