-- The known-good set: the 5,000 most-downloaded npm packages.
-- New packages are matched against this.
CREATE TABLE reference_package (
    name             TEXT PRIMARY KEY,
    normalized_name  TEXT   NOT NULL,
    weekly_downloads BIGINT NOT NULL,
    loaded_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The trigram index. Without it, a similarity query reads all 5,000 rows.
-- With it, Postgres jumps straight to the rows sharing trigrams with the query.
--
-- GIN (Generalised Inverted Index) maps each trigram to the rows containing it,
-- the same shape as a book index mapping words to page numbers.
CREATE INDEX idx_reference_package_trgm
    ON reference_package
    USING gin (normalized_name gin_trgm_ops);

-- Packages seen on the registry. One row per package observed.
CREATE TABLE observed_package (
    name             TEXT PRIMARY KEY,
    normalized_name  TEXT   NOT NULL,
    weekly_downloads BIGINT NOT NULL DEFAULT 0,
    publisher        TEXT,
    repository_url   TEXT,
    published_at     TIMESTAMPTZ,
    observed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One row per suspicion raised. The evidence column stores the full per-clue
-- breakdown, so the dashboard can explain the decision rather than just
-- reporting a number.
CREATE TABLE alert (
    id             BIGSERIAL PRIMARY KEY,
    candidate_name TEXT   NOT NULL REFERENCES observed_package(name),
    reference_name TEXT   NOT NULL REFERENCES reference_package(name),
    total_weight   DOUBLE PRECISION NOT NULL,
    threshold      DOUBLE PRECISION NOT NULL,
    evidence       JSONB  NOT NULL,
    verdict        TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_alert_pair UNIQUE (candidate_name, reference_name),
    CONSTRAINT ck_alert_verdict CHECK (verdict IN ('REAL', 'NOISE') OR verdict IS NULL)
);

-- The dashboard's main query: highest risk first.
CREATE INDEX idx_alert_weight ON alert (total_weight DESC);