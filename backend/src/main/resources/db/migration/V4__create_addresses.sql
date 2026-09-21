CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    cep VARCHAR(8) NOT NULL,
    street VARCHAR(120) NOT NULL,
    number VARCHAR(20) NOT NULL,
    complement VARCHAR(120),
    neighborhood VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_address_user_id ON addresses(user_id);
CREATE UNIQUE INDEX uq_address_user_principal ON addresses(user_id) WHERE principal = TRUE;
