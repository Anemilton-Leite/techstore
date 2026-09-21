ALTER TABLE orders
    ADD COLUMN shipping_cep VARCHAR(8),
    ADD COLUMN shipping_street VARCHAR(120),
    ADD COLUMN shipping_number VARCHAR(20),
    ADD COLUMN shipping_complement VARCHAR(120),
    ADD COLUMN shipping_neighborhood VARCHAR(100),
    ADD COLUMN shipping_city VARCHAR(100),
    ADD COLUMN shipping_state VARCHAR(2);