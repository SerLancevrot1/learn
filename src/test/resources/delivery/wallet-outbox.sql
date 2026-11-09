CREATE TABLE lab_credit_request (
    event_id VARCHAR(100) PRIMARY KEY,
    wallet_id VARCHAR(100) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount > 0)
);

CREATE TABLE lab_credit_outbox (
    position BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE REFERENCES lab_credit_request(event_id),
    published BOOLEAN NOT NULL DEFAULT FALSE
);
