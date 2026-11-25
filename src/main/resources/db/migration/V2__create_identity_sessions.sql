CREATE TABLE user_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  expires_at DATETIME(6) NOT NULL,
  revoked_at DATETIME(6) NULL,

  CONSTRAINT uk_user_session_token_hash UNIQUE (token_hash),
  CONSTRAINT fk_user_session_user
      FOREIGN KEY (user_id)
          REFERENCES user_account(id)
          ON DELETE CASCADE,

  INDEX idx_user_session_user_expiry (user_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;