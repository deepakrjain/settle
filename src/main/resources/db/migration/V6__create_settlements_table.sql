CREATE TABLE settlements (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    from_user_id UUID NOT NULL,
    to_user_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_settlements_group FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_settlements_from_user FOREIGN KEY (from_user_id) REFERENCES users(id),
    CONSTRAINT fk_settlements_to_user FOREIGN KEY (to_user_id) REFERENCES users(id)
);

CREATE INDEX idx_settlements_idempotency ON settlements(idempotency_key);
