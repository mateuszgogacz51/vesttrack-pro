-- Rejestr wykorzystania zewnetrznych API (do panelu Rate-Limiting Dashboard dla admina)
CREATE TABLE api_usage_daily (
    id              BIGSERIAL PRIMARY KEY,
    provider        VARCHAR(50) NOT NULL,     -- np. 'YAHOO_FINANCE', 'ALPHA_VANTAGE', 'NBP'
    usage_date      DATE NOT NULL,
    call_count      INT NOT NULL DEFAULT 0,
    error_count     INT NOT NULL DEFAULT 0,
    UNIQUE (provider, usage_date)
);

CREATE INDEX idx_api_usage_provider_date ON api_usage_daily(provider, usage_date);

-- Konfiguracja dostawcow API mozliwa do zmiany z panelu admina "na zywo",
-- bez potrzeby restartu aplikacji (np. przelaczenie na zapasowy klucz API po wyczerpaniu limitu).
CREATE TABLE api_provider_config (
    id              BIGSERIAL PRIMARY KEY,
    provider        VARCHAR(50) NOT NULL UNIQUE,
    api_key         VARCHAR(255),
    daily_limit     INT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO api_provider_config (provider, api_key, daily_limit, is_active) VALUES
    ('YAHOO_FINANCE', NULL, 2000, TRUE),
    ('ALPHA_VANTAGE', 'demo', 25, TRUE),
    ('NBP', NULL, NULL, TRUE);
