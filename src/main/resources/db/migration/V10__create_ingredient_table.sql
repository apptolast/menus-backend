-- Tenant-scoped ingredient catalog with JSONB allergens
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE ingredients (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    brand           VARCHAR(255),
    supplier        VARCHAR(255),
    allergens       JSONB NOT NULL DEFAULT '[]',
    traces          JSONB DEFAULT '[]',
    ocr_raw_text    TEXT,
    notes           TEXT,
    created_by      UUID REFERENCES user_account(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ingredients_tenant ON ingredients(tenant_id);
CREATE INDEX idx_ingredients_allergens ON ingredients USING GIN(allergens);
CREATE INDEX idx_ingredients_name_trgm ON ingredients USING gin(name gin_trgm_ops);

ALTER TABLE ingredients ENABLE ROW LEVEL SECURITY;
ALTER TABLE ingredients FORCE ROW LEVEL SECURITY;

CREATE POLICY ingredients_tenant_isolation ON ingredients
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);
