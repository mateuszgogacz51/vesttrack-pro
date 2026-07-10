CREATE TABLE transactions (
    id                  BIGSERIAL PRIMARY KEY,
    account_id          BIGINT NOT NULL REFERENCES investment_accounts(id) ON DELETE CASCADE,
    instrument_id       BIGINT NOT NULL REFERENCES financial_instruments(id),
    transaction_type    VARCHAR(20) NOT NULL CHECK (transaction_type IN ('BUY', 'SELL', 'DIVIDEND')),
    quantity            NUMERIC(18,6) NOT NULL CHECK (quantity > 0),
    price               NUMERIC(18,6) NOT NULL CHECK (price >= 0),
    fee                 NUMERIC(18,6) NOT NULL DEFAULT 0,
    instrument_currency VARCHAR(3) NOT NULL,
    exchange_rate       NUMERIC(18,6) NOT NULL DEFAULT 1, -- kurs do PLN (lub waluty bazowej konta) w dniu transakcji
    transaction_date    DATE NOT NULL,
    -- wypelniane przez silnik FIFO przy transakcjach SELL:
    realized_gain       NUMERIC(18,2),
    realized_gain_currency VARCHAR(3),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_instrument ON transactions(instrument_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
CREATE INDEX idx_transactions_account_instrument_date
    ON transactions(account_id, instrument_id, transaction_date);
