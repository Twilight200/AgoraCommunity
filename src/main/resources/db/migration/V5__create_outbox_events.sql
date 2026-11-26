CREATE TABLE outbox_event (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              event_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
                              aggregate_type VARCHAR(32) NOT NULL,
                              aggregate_id BIGINT NOT NULL,
                              event_type VARCHAR(64) NOT NULL,
                              payload JSON NOT NULL,
                              status VARCHAR(16) NOT NULL DEFAULT 'NEW',
                              attempts INT NOT NULL DEFAULT 0,
                              available_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                              created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                              CONSTRAINT uk_outbox_event_id UNIQUE (event_id),
                              CONSTRAINT chk_outbox_event_status
                                  CHECK (status IN ('NEW', 'SENDING', 'SENT', 'DEAD')),

                              INDEX idx_outbox_claim (status, available_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;