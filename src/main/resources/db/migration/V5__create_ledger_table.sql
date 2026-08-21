CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    from_user_id UUID NOT NULL,
    to_user_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    source_id UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_ledger_group FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_ledger_from_user FOREIGN KEY (from_user_id) REFERENCES users(id),
    CONSTRAINT fk_ledger_to_user FOREIGN KEY (to_user_id) REFERENCES users(id)
);

CREATE INDEX idx_ledger_entries_group_id ON ledger_entries(group_id);
