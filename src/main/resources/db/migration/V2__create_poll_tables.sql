
CREATE TABLE poll_rounds (
                             id BIGSERIAL PRIMARY KEY,
                             submission_id BIGINT NOT NULL REFERENCES submissions(id),
                             telegram_chat_id BIGINT NOT NULL,
                             telegram_poll_id VARCHAR(255) UNIQUE,
                             telegram_message_id BIGINT,
                             round_number INTEGER NOT NULL,
                             status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                             closes_at TIMESTAMPTZ NOT NULL,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             closed_at TIMESTAMPTZ,

                             UNIQUE (submission_id, round_number),
                             CHECK (round_number IN (1, 2)),
                             CHECK (status IN ('ACTIVE', 'CLOSED', 'WON', 'TIE', 'ADMIN_DECISION'))
);

CREATE TABLE poll_round_options (
                                    id BIGSERIAL PRIMARY KEY,
                                    poll_round_id BIGINT NOT NULL
                                        REFERENCES poll_rounds(id) ON DELETE CASCADE,
                                    book_id BIGINT NOT NULL REFERENCES books(id),
                                    option_index INTEGER NOT NULL,

                                    UNIQUE (poll_round_id, book_id),
                                    UNIQUE (poll_round_id, option_index),
                                    UNIQUE (poll_round_id, id),
                                    CHECK (option_index BETWEEN 0 AND 9)
);

CREATE TABLE poll_eligible_voters (
                                      poll_round_id BIGINT NOT NULL
                                          REFERENCES poll_rounds(id) ON DELETE CASCADE,
                                      telegram_user_id BIGINT NOT NULL,

                                      PRIMARY KEY (poll_round_id, telegram_user_id)
);

CREATE TABLE poll_participants (
                                   poll_round_id BIGINT NOT NULL,
                                   telegram_user_id BIGINT NOT NULL,
                                   answered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   PRIMARY KEY (poll_round_id, telegram_user_id),

                                   FOREIGN KEY (poll_round_id, telegram_user_id)
                                       REFERENCES poll_eligible_voters(poll_round_id, telegram_user_id)
                                       ON DELETE CASCADE
);

CREATE TABLE poll_votes (
                            poll_round_id BIGINT NOT NULL,
                            telegram_user_id BIGINT NOT NULL,
                            poll_option_id BIGINT NOT NULL,

                            PRIMARY KEY (poll_round_id, telegram_user_id, poll_option_id),

                            FOREIGN KEY (poll_round_id, telegram_user_id)
                                REFERENCES poll_participants(poll_round_id, telegram_user_id)
                                ON DELETE CASCADE,

                            FOREIGN KEY (poll_round_id, poll_option_id)
                                REFERENCES poll_round_options(poll_round_id, id)
                                ON DELETE CASCADE
);

CREATE INDEX idx_poll_rounds_status_closes_at
    ON poll_rounds(status, closes_at);
