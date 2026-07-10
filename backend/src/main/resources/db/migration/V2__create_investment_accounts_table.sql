CREATE TABLE investment_accounts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(150) NOT NULL,
    account_type    VARCHAR(20) NOT NULL DEFAULT 'REGULAR'
                        CHECK (account_type IN ('REGULAR', 'IKE', 'IKZE')),
    currency        VARCHAR(3) NOT NULL DEFAULT 'PLN',
    -- limit wplat na dany rok kalendarzowy dla kont IKE/IKZE
    annual_contribution_limit NUMERIC(18,2),
    contributed_this_year      NUMERIC(18,2) NOT NULL DEFAULT 0,
    contribution_year          INT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_user_id ON investment_accounts(user_id);
CREATE INDEX idx_accounts_type ON investment_accounts(account_type);
