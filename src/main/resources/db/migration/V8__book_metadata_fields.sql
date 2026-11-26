ALTER TABLE books
    ADD COLUMN isbn VARCHAR(20),
    ADD COLUMN metadata_status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    ADD COLUMN description_source TEXT,
    ADD COLUMN metadata_updated_at TIMESTAMPTZ;