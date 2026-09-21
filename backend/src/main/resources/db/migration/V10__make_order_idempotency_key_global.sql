DROP INDEX IF EXISTS uk_orders_customer_idempotency;

CREATE UNIQUE INDEX uq_orders_idempotency_key
    ON orders(idempotency_key)
    WHERE idempotency_key IS NOT NULL;