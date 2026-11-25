CREATE TABLE user_daily_quota (
  user_id BIGINT NOT NULL,
  quota_date DATE NOT NULL,
  topics_created INT NOT NULL DEFAULT 0,
  replies_created INT NOT NULL DEFAULT 0,

  CONSTRAINT pk_user_daily_quota
      PRIMARY KEY (user_id, quota_date),

  CONSTRAINT fk_user_daily_quota_user
      FOREIGN KEY (user_id)
          REFERENCES user_account(id)
          ON DELETE CASCADE,

  CONSTRAINT chk_user_daily_quota_topics_nonnegative
      CHECK (topics_created >= 0),

  CONSTRAINT chk_user_daily_quota_replies_nonnegative
      CHECK (replies_created >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;