CREATE SEQUENCE hibernate_sequence INCREMENT BY 1 START WITH 1;

ALTER TABLE vk_groups ADD groupId BIGINT NOT NULL;

DROP TABLE hibernate_sequence;

DROP TABLE vk_group_subscribers_SEQ;