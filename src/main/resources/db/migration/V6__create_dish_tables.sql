CREATE TABLE dish (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    section_id      UUID NOT NULL REFERENCES menu_section(id),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    price           DECIMAL(10,2),
    image_url       VARCHAR(500),
    is_available    BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- CRITICAL table: allergen containment per dish
CREATE TABLE dish_allergen (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dish_id             UUID NOT NULL REFERENCES dish(id),
    allergen_id         INTEGER NOT NULL REFERENCES allergen(id),
    tenant_id           UUID NOT NULL,
    containment_level   VARCHAR(20) NOT NULL CHECK (containment_level IN ('CONTAINS', 'MAY_CONTAIN', 'FREE_OF')),
    notes               TEXT,
    UNIQUE (dish_id, allergen_id)
);
