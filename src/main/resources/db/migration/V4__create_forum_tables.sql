CREATE TABLE category (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          name VARCHAR(100) NOT NULL,
                          slug VARCHAR(100) NOT NULL,
                          sort INT NOT NULL DEFAULT 0,
                          created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                          updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                          CONSTRAINT uk_category_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE topic (
                       id BIGINT PRIMARY KEY,
                       category_id BIGINT NOT NULL,
                       user_id BIGINT NOT NULL,
                       title VARCHAR(200) NOT NULL,
                       status VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
                       pinned BOOLEAN NOT NULL DEFAULT FALSE,
                       max_floor INT NOT NULL DEFAULT 1,
                       last_reply_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       last_reply_user_id BIGINT NOT NULL,
                       version INT NOT NULL DEFAULT 0,
                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                       CONSTRAINT fk_topic_category
                           FOREIGN KEY (category_id) REFERENCES category(id),

                       CONSTRAINT chk_topic_status
                           CHECK (status IN ('NORMAL', 'DELETED')),

                       CONSTRAINT chk_topic_max_floor
                           CHECK (max_floor >= 1),

                       INDEX idx_topic_category_bump (category_id, last_reply_at, id),
                       INDEX idx_topic_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE reply (
                       id BIGINT PRIMARY KEY,
                       topic_id BIGINT NOT NULL,
                       floor_no INT NOT NULL,
                       user_id BIGINT NOT NULL,
                       reply_to_floor INT NULL,
                       content MEDIUMTEXT NOT NULL,
                       status VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
                       version INT NOT NULL DEFAULT 0,
                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                       CONSTRAINT fk_reply_topic
                           FOREIGN KEY (topic_id) REFERENCES topic(id),

                       CONSTRAINT uk_reply_topic_floor
                           UNIQUE (topic_id, floor_no),

                       CONSTRAINT chk_reply_floor_no
                           CHECK (floor_no >= 1),

                       CONSTRAINT chk_reply_status
                           CHECK (status IN ('NORMAL', 'DELETED')),

                       INDEX idx_reply_topic_user_floor (topic_id, user_id, floor_no),
                       INDEX idx_reply_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;