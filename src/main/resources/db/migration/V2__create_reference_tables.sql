-- Allergen reference tables (NO RLS — shared data)
CREATE TABLE allergen (
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(20) UNIQUE NOT NULL,
    icon_url    VARCHAR(500),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE allergen_translation (
    id          SERIAL PRIMARY KEY,
    allergen_id INTEGER NOT NULL REFERENCES allergen(id),
    locale      VARCHAR(5) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    UNIQUE (allergen_id, locale)
);
