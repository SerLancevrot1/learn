CREATE TABLE lab_wallet (
    wallet_id VARCHAR(100) PRIMARY KEY,
    balance BIGINT NOT NULL
);

-- В данном сценарии event_id глобально уникален для одного логического обработчика.
-- Независимые обработчики обычно используют составной ключ (handler_id, event_id).
CREATE TABLE lab_inbox (
    event_id VARCHAR(100) PRIMARY KEY
);
