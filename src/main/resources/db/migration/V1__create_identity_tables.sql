create table user_account(
    id bigint primary key auto_increment comment '用户ID',
    username varchar(64) not null unique comment '用户名',
    password_hash varchar(255) not null comment '用户密码',
    trust_level tinyint not null default 0 comment '信任等级',
    status tinyint not null default 1 comment '用户状态',
    created_at datetime(6) not null default current_timestamp(6) comment '创建时间',
    last_active_at datetime(6) not null default current_timestamp(6) comment '更新时间'
) engine=InnoDB default charset = utf8mb4;


CREATE TABLE user_stat (
   user_id BIGINT PRIMARY KEY COMMENT '用户ID',
   topics_read BIGINT NOT NULL DEFAULT 0 COMMENT '阅读主题数',
   replies_read BIGINT NOT NULL DEFAULT 0 COMMENT '阅读回复数',
   likes_given BIGINT NOT NULL DEFAULT 0 COMMENT '送出点赞数',
   likes_received BIGINT NOT NULL DEFAULT 0 COMMENT '收到点赞数',
   days_visited BIGINT NOT NULL DEFAULT 0 COMMENT '访问天数',
   CONSTRAINT fk_user_stat_user
       FOREIGN KEY (user_id)
           REFERENCES user_account(id)
           ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;