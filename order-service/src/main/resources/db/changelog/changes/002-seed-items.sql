--liquibase formatted sql

--changeset vitkvsk:002-seed-items
INSERT INTO items (id, name, price, created_at, updated_at)
VALUES
    (101, 'Wireless Mouse', 19.99, now(), now()),
    (102, 'Mechanical Keyboard', 79.90, now(), now()),
    (103, 'USB-C Hub', 45.00, now(), now())
ON CONFLICT (id) DO NOTHING;
--rollback DELETE FROM items WHERE id IN (101, 102, 103);
