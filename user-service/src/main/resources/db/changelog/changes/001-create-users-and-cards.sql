--liquibase formatted sql

--changeset vitkvsk:001-create-users-and-cards
CREATE SEQUENCE user_id_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE payment_card_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE users (
    id BIGINT NOT NULL DEFAULT nextval('user_id_seq') PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    surname VARCHAR(50) NOT NULL,
    birth_date DATE NOT NULL,
    email VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_user_email UNIQUE (email)
);

CREATE INDEX idx_users_surname_name ON users (surname, name);

CREATE TABLE payment_cards (
    id BIGINT NOT NULL DEFAULT nextval('payment_card_id_seq') PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    number VARCHAR(32) NOT NULL,
    holder VARCHAR(100) NOT NULL,
    expiration_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_card_number UNIQUE (number)
);

CREATE INDEX idx_cards_user_id ON payment_cards (user_id);