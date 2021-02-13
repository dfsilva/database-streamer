CREATE TABLE database_streamer.event_journal
(
    "ordering"         bigserial    NOT NULL,
    persistence_id     varchar(255) NOT NULL,
    sequence_number    int8         NOT NULL,
    deleted            bool         NOT NULL DEFAULT false,
    writer             varchar(255) NOT NULL,
    write_timestamp    int8         NULL,
    adapter_manifest   varchar(255) NULL,
    event_ser_id       int4         NOT NULL,
    event_ser_manifest varchar(255) NOT NULL,
    event_payload      bytea        NOT NULL,
    meta_ser_id        int4         NULL,
    meta_ser_manifest  varchar(255) NULL,
    meta_payload       bytea        NULL,
    CONSTRAINT event_journal_pkey PRIMARY KEY (persistence_id, sequence_number)
);
CREATE UNIQUE INDEX event_journal_ordering_idx ON database_streamer.event_journal USING btree (ordering);


CREATE TABLE database_streamer.snapshot
(
    persistence_id        varchar(255) NOT NULL,
    sequence_number       int8         NOT NULL,
    created               int8         NOT NULL,
    snapshot_ser_id       int4         NOT NULL,
    snapshot_ser_manifest varchar(255) NOT NULL,
    snapshot_payload      bytea        NOT NULL,
    meta_ser_id           int4         NULL,
    meta_ser_manifest     varchar(255) NULL,
    meta_payload          bytea        NULL,
    CONSTRAINT snapshot_pkey PRIMARY KEY (persistence_id, sequence_number)
);

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
    topic       VARCHAR   NOT NULL,
    old         TEXT      NULL,
    current     TEXT      NULL
);

CREATE TABLE database_streamer.streams
(
    topic         VARCHAR PRIMARY KEY,
    stream_table  VARCHAR NOT NULL,
    stream_schema VARCHAR NOT NULL,
    title         VARCHAR NOT NULL,
    description   VARCHAR NULL,
    delete        boolean NOT NULL,
    insert        boolean NOT NULL,
    update        boolean NOT NULL
);


