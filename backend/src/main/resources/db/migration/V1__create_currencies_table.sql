-- ============================================================
-- V1: Döviz Kurları Tablosu
-- ============================================================

CREATE TABLE currencies (
    id                  BIGSERIAL       PRIMARY KEY,
    currency_code       VARCHAR(10)     NOT NULL,
    currency_name       VARCHAR(100),
    forex_buying        NUMERIC(18, 6),
    forex_selling       NUMERIC(18, 6),
    banknote_buying     NUMERIC(18, 6),
    banknote_selling    NUMERIC(18, 6),
    unit                INTEGER         NOT NULL DEFAULT 1,
    date                DATE            NOT NULL,
    source              VARCHAR(50)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,

    CONSTRAINT uk_currency_code_date UNIQUE (currency_code, date)
);

CREATE INDEX idx_currency_code ON currencies (currency_code);
CREATE INDEX idx_currency_date ON currencies (date);
CREATE INDEX idx_currency_source ON currencies (source);
