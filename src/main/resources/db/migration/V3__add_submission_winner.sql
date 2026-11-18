ALTER TABLE submissions
    ADD COLUMN winner_book_id BIGINT REFERENCES books(id);

ALTER TABLE submissions
    ADD COLUMN winner_selected_at TIMESTAMPTZ;