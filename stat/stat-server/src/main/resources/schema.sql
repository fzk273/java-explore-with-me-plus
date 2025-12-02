CREATE TABLE IF NOT EXISTS users (
    id      BIGSERIAL PRIMARY KEY,
    email   VARCHAR(254) NOT NULL,
    name    VARCHAR(250) NOT NULL,
    CONSTRAINT uq_user_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS categories (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(50) NOT NULL,
    CONSTRAINT uq_category_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS events (
    id                  BIGSERIAL PRIMARY KEY,
    annotation          VARCHAR(2000) NOT NULL,
    description         VARCHAR(7000) NOT NULL,
    event_date          TIMESTAMP NOT NULL,
    created_on          TIMESTAMP NOT NULL DEFAULT NOW(),
    published_on        TIMESTAMP NULL,
    category_id         BIGINT NOT NULL,
    initiator_id        BIGINT NOT NULL,
    lat                 DOUBLE PRECISION NOT NULL,
    lon                 DOUBLE PRECISION NOT NULL,
    paid                BOOLEAN NOT NULL DEFAULT FALSE,
    participant_limit   INT NOT NULL DEFAULT 0,
    request_moderation  BOOLEAN NOT NULL DEFAULT TRUE,
    state               VARCHAR(10) NOT NULL,
    CONSTRAINT fk_events_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_events_initiator FOREIGN KEY (initiator_id) REFERENCES users(id),
    CONSTRAINT chk_events_state CHECK (state IN ('PENDING', 'PUBLISHED', 'CANCELED'))
);

CREATE TABLE IF NOT EXISTS participation_requests (
    id            BIGSERIAL PRIMARY KEY,
    created       TIMESTAMP NOT NULL DEFAULT NOW(),
    event_id      BIGINT NOT NULL,
    requester_id  BIGINT NOT NULL,
    status        VARCHAR(20) NOT NULL,
    CONSTRAINT fk_pr_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_pr_requester FOREIGN KEY (requester_id) REFERENCES users(id),
    CONSTRAINT chk_pr_status CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELED')),
    CONSTRAINT uq_request UNIQUE (event_id, requester_id)
);

CREATE TABLE IF NOT EXISTS compilations (
    id      BIGSERIAL PRIMARY KEY,
    title   VARCHAR(50) NOT NULL,
    pinned  BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_compilation_name UNIQUE (title)
);

CREATE TABLE IF NOT EXISTS compilation_events (
    compilation_id  BIGINT NOT NULL,
    event_id        BIGINT NOT NULL,
    CONSTRAINT pk_compilation_events PRIMARY KEY (compilation_id, event_id),
    CONSTRAINT fk_ce_compilation FOREIGN KEY (compilation_id) REFERENCES compilations(id) ON DELETE CASCADE,
    CONSTRAINT fk_ce_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);