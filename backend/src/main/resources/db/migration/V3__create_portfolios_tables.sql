-- ============================================================
-- V3: Portföy Yönetimi Tabloları
-- ============================================================

-- Portföyler
CREATE TABLE portfolios (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         VARCHAR(255)    NOT NULL,       -- Keycloak user ID
    name            VARCHAR(100)    NOT NULL,
    description     VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE INDEX idx_portfolio_user_id ON portfolios (user_id);

-- Portföy Kalemleri
CREATE TABLE portfolio_items (
    id                  BIGSERIAL       PRIMARY KEY,
    portfolio_id        BIGINT          NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    instrument_type     VARCHAR(30)     NOT NULL,       -- HISSE, DOVIZ, FON, TAHVIL, KRIPTO
    symbol              VARCHAR(20)     NOT NULL,       -- USD, THYAO, vb.
    instrument_name     VARCHAR(200),
    quantity            NUMERIC(18, 6)  NOT NULL,
    purchase_price      NUMERIC(18, 6)  NOT NULL,
    purchase_date       DATE,
    notes               VARCHAR(500),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP
);

CREATE INDEX idx_portfolio_item_portfolio ON portfolio_items (portfolio_id);
CREATE INDEX idx_portfolio_item_symbol    ON portfolio_items (symbol);

-- Tarihsel Fiyat Verileri (grafik ve analiz için)
CREATE TABLE historical_prices (
    id                  BIGSERIAL       PRIMARY KEY,
    symbol              VARCHAR(20)     NOT NULL,
    instrument_type     VARCHAR(30)     NOT NULL,
    date                DATE            NOT NULL,
    open_price          NUMERIC(18, 6),
    close_price         NUMERIC(18, 6),
    high_price          NUMERIC(18, 6),
    low_price           NUMERIC(18, 6),
    volume              BIGINT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_hist_symbol_type_date UNIQUE (symbol, instrument_type, date)
);

CREATE INDEX idx_hist_symbol ON historical_prices (symbol);
CREATE INDEX idx_hist_date   ON historical_prices (date);

-- Kullanıcı bilgileri (Keycloak'tan senkronize)
CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    keycloak_id     VARCHAR(255)    NOT NULL UNIQUE,
    email           VARCHAR(255),
    full_name       VARCHAR(200),
    role            VARCHAR(30)     NOT NULL DEFAULT 'ROLE_USER',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE INDEX idx_users_keycloak_id ON users (keycloak_id);
