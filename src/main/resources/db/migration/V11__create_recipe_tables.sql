-- Recipes with sub-elaboration support (tenant-scoped)
CREATE TABLE recipes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    restaurant_id       UUID NOT NULL REFERENCES restaurant(id),
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    category            VARCHAR(100),
    is_sub_elaboration  BOOLEAN NOT NULL DEFAULT false,
    price               DECIMAL(10,2),
    image_url           VARCHAR(500),
    created_by          UUID REFERENCES user_account(id),
    is_active           BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recipes_tenant ON recipes(tenant_id);
CREATE INDEX idx_recipes_restaurant ON recipes(restaurant_id);

ALTER TABLE recipes ENABLE ROW LEVEL SECURITY;
ALTER TABLE recipes FORCE ROW LEVEL SECURITY;

CREATE POLICY recipes_tenant_isolation ON recipes
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- Recipe ingredients: links to either an ingredient OR a sub-recipe (XOR)
CREATE TABLE recipe_ingredients (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id       UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    ingredient_id   UUID REFERENCES ingredients(id) ON DELETE SET NULL,
    sub_recipe_id   UUID REFERENCES recipes(id) ON DELETE SET NULL,
    tenant_id       UUID NOT NULL,
    quantity        DECIMAL(10,3),
    unit            VARCHAR(50),
    notes           VARCHAR(500),
    sort_order      INT NOT NULL DEFAULT 0,
    CONSTRAINT check_ingredient_or_subrecipe
        CHECK (
            (ingredient_id IS NOT NULL AND sub_recipe_id IS NULL) OR
            (ingredient_id IS NULL AND sub_recipe_id IS NOT NULL)
        )
);

CREATE INDEX idx_recipe_ingredients_recipe ON recipe_ingredients(recipe_id);
CREATE INDEX idx_recipe_ingredients_ingredient ON recipe_ingredients(ingredient_id);
CREATE INDEX idx_recipe_ingredients_subrecipe ON recipe_ingredients(sub_recipe_id);

ALTER TABLE recipe_ingredients ENABLE ROW LEVEL SECURITY;
ALTER TABLE recipe_ingredients FORCE ROW LEVEL SECURITY;

CREATE POLICY recipe_ingredients_tenant_isolation ON recipe_ingredients
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);
