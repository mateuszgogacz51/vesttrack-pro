-- Slownik instytucji (biur maklerskich / bankow / TFI), w ktorych uzytkownik moze
-- prowadzic rachunek inwestycyjny. Cel: przy zakladaniu konta uzytkownik/pracownik
-- wybiera instytucje z listy zamiast wpisywac jej nazwe recznie za kazdym razem.
CREATE TABLE brokerage_firms (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL UNIQUE,
    category        VARCHAR(20) NOT NULL DEFAULT 'BROKER'
                        CHECK (category IN ('BANK', 'BROKER', 'TFI')),
    website         VARCHAR(255),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_brokerage_firms_name ON brokerage_firms(name);

INSERT INTO brokerage_firms (name, category, website) VALUES
    ('XTB', 'BROKER', 'https://www.xtb.com'),
    ('DEGIRO', 'BROKER', 'https://www.degiro.pl'),
    ('Interactive Brokers', 'BROKER', 'https://www.interactivebrokers.com'),
    ('BOŚ Dom Maklerski (BOSSA)', 'BROKER', 'https://www.bossa.pl'),
    ('mBank (mDM)', 'BANK', 'https://www.mbank.pl'),
    ('PKO BP Dom Maklerski (PKO BM)', 'BANK', 'https://www.dm.pkobp.pl'),
    ('Santander Biuro Maklerskie', 'BANK', 'https://www.santander.pl'),
    ('Pekao Dom Maklerski', 'BANK', 'https://www.dompekao.com.pl'),
    ('ING Dom Maklerski', 'BANK', 'https://www.ing.pl'),
    ('Alior Bank Biuro Maklerskie', 'BANK', 'https://www.aliorbank.pl'),
    ('Trading 212', 'BROKER', 'https://www.trading212.com'),
    ('eToro', 'BROKER', 'https://www.etoro.com'),
    ('Saxo Bank', 'BROKER', 'https://www.home.saxo'),
    ('BNP Paribas Biuro Maklerskie', 'BANK', 'https://www.bnpparibas.pl'),
    ('PZU TFI', 'TFI', 'https://www.pzu.pl'),
    ('NN Investment Partners TFI', 'TFI', 'https://www.nn.pl'),
    ('Generali Investments TFI', 'TFI', 'https://www.generali-investments.pl'),
    ('Allianz Polska TFI', 'TFI', 'https://www.allianz.pl'),
    ('Aegon TFI', 'TFI', 'https://www.aegon.pl'),
    ('Nationale-Nederlanden PTE', 'TFI', 'https://www.nn.pl'),
    ('Inna instytucja (wpisz nazwę ręcznie)', 'BROKER', NULL);

-- Powiazanie konta z instytucja z powyzszego slownika (opcjonalne - pozostawiamy
-- mozliwosc wlasnej nazwy dla instytucji spoza slownika, patrz wiersz "Inna instytucja").
ALTER TABLE investment_accounts
    ADD COLUMN brokerage_firm_id BIGINT REFERENCES brokerage_firms(id);

CREATE INDEX idx_accounts_brokerage_firm ON investment_accounts(brokerage_firm_id);
