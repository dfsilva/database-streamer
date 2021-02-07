CREATE SEQUENCE database_streamer.sq_events
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 642
    CACHE 1
    NO CYCLE;

CREATE TABLE database_streamer.events
(
    id          SERIAL PRIMARY KEY,
    create_time TIMESTAMP NOT NULL,
    topic       VARCHAR   NULL,
    old_data    TEXT      NULL,
    new_data    TEXT      NULL
);

CREATE TABLE database_streamer.streams
(
    id          SERIAL PRIMARY KEY,
    title       VARCHAR NOT NULL,
    description VARCHAR NULL,
    table_name  VARCHAR NOT NULL,
    topic       VARCHAR NOT NULL
);

DROP TABLE IF EXISTS database_streamer.journal;
CREATE TABLE IF NOT EXISTS database_streamer.journal
(
    ordering        BIGSERIAL,
    persistence_id  VARCHAR(255)               NOT NULL,
    sequence_number BIGINT                     NOT NULL,
    deleted         BOOLEAN      DEFAULT FALSE NOT NULL,
    tags            VARCHAR(255) DEFAULT NULL,
    message         BYTEA                      NOT NULL,
    PRIMARY KEY (persistence_id, sequence_number)
);
CREATE UNIQUE INDEX journal_ordering_idx ON database_streamer.journal (ordering);

DROP TABLE IF EXISTS database_streamer.snapshot;
CREATE TABLE IF NOT EXISTS database_streamer.snapshot
(
    persistence_id  VARCHAR(255) NOT NULL,
    sequence_number BIGINT       NOT NULL,
    created         BIGINT       NOT NULL,
    snapshot        BYTEA        NOT NULL,
    PRIMARY KEY (persistence_id, sequence_number)
);


