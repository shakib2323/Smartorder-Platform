CREATE TABLE outbox_events (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id       CHAR(36)     NOT NULL,
    aggregate_id   BIGINT       NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    topic          VARCHAR(100) NOT NULL,
    payload        JSON         NOT NULL,
    correlation_id VARCHAR(64),
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts       INT          NOT NULL DEFAULT 0,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sent_at        DATETIME(6),
    CONSTRAINT uq_outbox_event_id UNIQUE (event_id),
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'SENT'))
) ENGINE=InnoDB;

CREATE INDEX idx_outbox_status_id ON outbox_events (status, id);

CREATE TABLE processed_events (
    event_id     CHAR(36)    NOT NULL PRIMARY KEY,
    processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;
