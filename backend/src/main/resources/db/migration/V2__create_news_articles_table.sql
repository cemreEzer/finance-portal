-- ============================================================
-- V2: Haber Makaleleri Tablosu
-- ============================================================

CREATE TABLE news_articles (
    id              BIGSERIAL       PRIMARY KEY,
    title           VARCHAR(500)    NOT NULL,
    summary         TEXT,
    content         TEXT,
    source          VARCHAR(100),
    url             VARCHAR(1000),
    image_url       VARCHAR(1000),
    category        VARCHAR(30)     NOT NULL DEFAULT 'GENEL_EKONOMI',
    published_at    TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    external_id     VARCHAR(500)    UNIQUE
);

CREATE INDEX idx_news_category     ON news_articles (category);
CREATE INDEX idx_news_published_at ON news_articles (published_at);
CREATE INDEX idx_news_source       ON news_articles (source);
CREATE INDEX idx_news_external_id  ON news_articles (external_id);
