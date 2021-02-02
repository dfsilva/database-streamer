--SEQUENCE
CREATE SEQUENCE sq_database_stream_events
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 642
    CACHE 1
    NO CYCLE;

--Events table
CREATE TABLE tb_database_stream_events (
      id bigserial NOT NULL,
      event_timestamp timestamp NOT NULL,
      topic varchar(100) NULL,
      body text NULL,
      CONSTRAINT tb_database_stream_events_key PRIMARY KEY (id)
);

--Akka tables
DROP TABLE IF EXISTS journal;
CREATE TABLE IF NOT EXISTS journal
(
    ordering        BIGSERIAL,
    persistence_id  VARCHAR(255)               NOT NULL,
    sequence_number BIGINT                     NOT NULL,
    deleted         BOOLEAN      DEFAULT FALSE NOT NULL,
    tags            VARCHAR(255) DEFAULT NULL,
    message         BYTEA                      NOT NULL,
    PRIMARY KEY (persistence_id, sequence_number)
);

CREATE UNIQUE INDEX journal_ordering_idx ON journal (ordering);
DROP TABLE IF EXISTS snapshot;
CREATE TABLE IF NOT EXISTS snapshot
(
    persistence_id  VARCHAR(255) NOT NULL,
    sequence_number BIGINT       NOT NULL,
    created         BIGINT       NOT NULL,
    snapshot        BYTEA        NOT NULL,
    PRIMARY KEY (persistence_id, sequence_number)
);
