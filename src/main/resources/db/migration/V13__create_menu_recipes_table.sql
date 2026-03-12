-- Pivot table linking menus to recipes with section grouping
CREATE TABLE menu_recipes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_id         UUID NOT NULL REFERENCES menu(id) ON DELETE CASCADE,
    recipe_id       UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    section_name    VARCHAR(255) DEFAULT 'General',
    sort_order      INT NOT NULL DEFAULT 0,
    tenant_id       UUID NOT NULL,
    UNIQUE(menu_id, recipe_id)
);

CREATE INDEX idx_menu_recipes_menu ON menu_recipes(menu_id);
CREATE INDEX idx_menu_recipes_recipe ON menu_recipes(recipe_id);

ALTER TABLE menu_recipes ENABLE ROW LEVEL SECURITY;
ALTER TABLE menu_recipes FORCE ROW LEVEL SECURITY;

CREATE POLICY menu_recipes_tenant_isolation ON menu_recipes
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);
