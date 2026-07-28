--liquibase formatted sql

--changeset vitkvsk:002-refactor-users-to-uuid
ALTER TABLE payment_cards DROP CONSTRAINT IF EXISTS payment_cards_user_id_fkey;

DROP INDEX IF EXISTS idx_cards_user_id;

ALTER TABLE users ALTER COLUMN id DROP DEFAULT;

ALTER TABLE payment_cards ALTER COLUMN user_id TYPE UUID USING NULL;

DROP SEQUENCE IF EXISTS user_id_seq;

ALTER TABLE users ALTER COLUMN id TYPE UUID USING NULL;

ALTER TABLE users ALTER COLUMN id SET NOT NULL;

ALTER TABLE payment_cards ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE payment_cards ADD CONSTRAINT payment_cards_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_cards_user_id ON payment_cards (user_id);