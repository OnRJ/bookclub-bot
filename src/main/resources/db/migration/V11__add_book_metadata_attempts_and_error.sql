ALTER TABLE books
    ADD COLUMN metadata_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN metadata_error TEXT;
