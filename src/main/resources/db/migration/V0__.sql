CREATE SEQUENCE hibernate_sequence INCREMENT BY 1 START WITH 1;

CREATE TABLE vk_groups (
   id BIGINT NOT NULL AUTO_INCREMENT,
   avatarUrl VARCHAR(255),
   lastWallPostId BIGINT NOT NULL,
   name VARCHAR(255) NOT NULL,
   originalLink VARCHAR(255) NOT NULL,
   CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

CREATE TABLE vk_group_subscribers (
   id BIGINT NOT NULL AUTO_INCREMENT,
   channelId BIGINT NOT NULL,
   guildId BIGINT NOT NULL,
   group_id BIGINT NOT NULL,
   CONSTRAINT `PRIMARY` PRIMARY KEY (id),
   CONSTRAINT FK1omoifr49clsugl7km38kj1av FOREIGN KEY (group_id) REFERENCES vk_groups (id) ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX FK1omoifr49clsugl7km38kj1av ON vk_group_subscribers(group_id);
