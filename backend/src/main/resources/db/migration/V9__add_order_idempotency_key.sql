ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(100);

CREATE UNIQUE INDEX uk_orders_customer_idempotency
    ON orders(customer_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;