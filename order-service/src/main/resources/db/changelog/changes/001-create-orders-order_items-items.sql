--liquibase formatted sql

--changeset vitkvsk:001-create-items
CREATE TABLE items (
    id          BIGINT                   NOT NULL,
    name        VARCHAR(100)             NOT NULL,
    price       NUMERIC(12, 2)           NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_items PRIMARY KEY (id),
    CONSTRAINT uq_items_name UNIQUE (name),
    CONSTRAINT chk_items_price CHECK (price >= 0)
);

CREATE SEQUENCE items_id_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO CACHE;

CREATE INDEX idx_items_name ON items (name);
--rollback DROP TABLE items;
--rollback DROP SEQUENCE items_id_seq;

--changeset vitkvsk:002-create-orders
CREATE TABLE orders (
    id          UUID                     NOT NULL,
    user_id     UUID                     NOT NULL,
    status      VARCHAR(32)              NOT NULL,
    total_price NUMERIC(12, 2)           NOT NULL,
    deleted     BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT chk_orders_total CHECK (total_price >= 0)
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status) WHERE deleted = FALSE;
--rollback DROP TABLE orders;

--changeset vitkvsk:003-create-order-items
CREATE TABLE order_items (
    id          BIGINT                   NOT NULL,
    order_id    UUID                     NOT NULL,
    item_id     BIGINT                   NOT NULL,
    quantity    INTEGER                  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_item
        FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE RESTRICT,
    CONSTRAINT chk_order_items_qty CHECK (quantity > 0),
    CONSTRAINT uq_order_items_order_item UNIQUE (order_id, item_id)
);

CREATE SEQUENCE order_items_id_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO CACHE;

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_item_id ON order_items (item_id);
--rollback DROP TABLE order_items;
--rollback DROP SEQUENCE order_items_id_seq;