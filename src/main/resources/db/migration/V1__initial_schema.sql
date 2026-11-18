CREATE TABLE clubs
(
    id               BIGSERIAL PRIMARY KEY,
    telegram_chat_id BIGINT       NOT NULL UNIQUE,
    name             VARCHAR(255) NOT NULL,
    timezone         VARCHAR(100) NOT NULL DEFAULT 'Europe/London',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users
(
    id               BIGSERIAL PRIMARY KEY,
    telegram_user_id BIGINT       NOT NULL UNIQUE,
    username         VARCHAR(255),
    first_name       VARCHAR(255) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE club_members
(
    id        BIGSERIAL PRIMARY KEY,
    club_id   BIGINT      NOT NULL REFERENCES clubs (id),
    user_id   BIGINT      NOT NULL REFERENCES users (id),
    role      VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (club_id, user_id)
);

CREATE TABLE books
(
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    author          VARCHAR(500),
    description     TEXT,
    page_count      INTEGER,
    external_id     VARCHAR(255),
    external_source VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (page_count IS NULL OR page_count > 0)
);

CREATE TABLE submissions
(
    id            BIGSERIAL PRIMARY KEY,
    club_id       BIGINT      NOT NULL REFERENCES clubs (id),
    submitted_by  BIGINT      NOT NULL REFERENCES users (id),
    reading_month DATE        NOT NULL,
    status        VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (EXTRACT(DAY FROM reading_month) = 1),
    UNIQUE (club_id, reading_month)
);

CREATE TABLE submission_books
(
    id            BIGSERIAL PRIMARY KEY,
    submission_id BIGINT  NOT NULL REFERENCES submissions (id) ON DELETE CASCADE,
    book_id       BIGINT  NOT NULL REFERENCES books (id),
    position      INTEGER NOT NULL,
    UNIQUE (submission_id, position),
    UNIQUE (submission_id, book_id),
    CHECK (position BETWEEN 1 AND 10)
);
