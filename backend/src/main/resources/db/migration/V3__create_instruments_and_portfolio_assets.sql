CREATE TABLE financial_instruments (
    id              BIGSERIAL PRIMARY KEY,
    ticker          VARCHAR(20) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    asset_type      VARCHAR(20) NOT NULL CHECK (asset_type IN ('STOCK', 'ETF', 'BOND', 'OTHER')),
    isin            VARCHAR(20) UNIQUE,
    exchange        VARCHAR(50),
    quote_currency  VARCHAR(3) NOT NULL DEFAULT 'PLN',
    is_accumulating BOOLEAN,            -- dla ETF: Acc vs Dist (NULL dla akcji)
    is_blocked      BOOLEAN NOT NULL DEFAULT FALSE,
    last_price      NUMERIC(18,6),
    last_price_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (ticker, exchange)
);

CREATE TABLE portfolio_assets (
    id              BIGSERIAL PRIMARY KEY,
    account_id      BIGINT NOT NULL REFERENCES investment_accounts(id) ON DELETE CASCADE,
    instrument_id   BIGINT NOT NULL REFERENCES financial_instruments(id),
    strategy_role   VARCHAR(20) NOT NULL DEFAULT 'CORE' CHECK (strategy_role IN ('CORE', 'SATELLITE')),
    target_weight   NUMERIC(5,2) CHECK (target_weight >= 0 AND target_weight <= 100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (account_id, instrument_id)
);

CREATE INDEX idx_portfolio_assets_account ON portfolio_assets(account_id);
CREATE INDEX idx_instruments_ticker ON financial_instruments(ticker);
