ALTER TABLE payments ALTER COLUMN checkout_url TYPE VARCHAR(1000);
ALTER TABLE payments ADD COLUMN external_reference VARCHAR(120);