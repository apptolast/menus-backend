-- Digital cards with public slug and QR
CREATE TABLE digital_cards (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id   UUID NOT NULL REFERENCES restaurant(id),
    menu_id         UUID NOT NULL REFERENCES menu(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    qr_code_url     VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    custom_css      JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_digital_cards_tenant ON digital_cards(tenant_id);
CREATE INDEX idx_digital_cards_slug ON digital_cards(slug);
CREATE INDEX idx_digital_cards_restaurant ON digital_cards(restaurant_id);

ALTER TABLE digital_cards ENABLE ROW LEVEL SECURITY;
ALTER TABLE digital_cards FORCE ROW LEVEL SECURITY;

CREATE POLICY digital_cards_tenant_isolation ON digital_cards
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);
