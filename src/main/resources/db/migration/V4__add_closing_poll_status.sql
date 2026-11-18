ALTER TABLE poll_rounds
    DROP CONSTRAINT IF EXISTS poll_rounds_status_check;

ALTER TABLE poll_rounds
    ADD CONSTRAINT poll_rounds_status_check
        CHECK (
            status IN (
                       'ACTIVE',
                       'CLOSING',
                       'CLOSED',
                       'WON',
                       'TIE',
                       'ADMIN_DECISION'
                )
            );