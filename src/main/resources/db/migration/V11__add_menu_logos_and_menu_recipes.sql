ALTER TABLE menus ADD COLUMN restaurant_logo_url VARCHAR(500);
ALTER TABLE menus ADD COLUMN company_logo_url VARCHAR(500);

CREATE TABLE menu_recipes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    menu_id UUID NOT NULL REFERENCES menus(id) ON DELETE CASCADE,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    UNIQUE (menu_id, recipe_id)
);
CREATE INDEX idx_menu_recipes_menu ON menu_recipes(menu_id);
CREATE INDEX idx_menu_recipes_recipe ON menu_recipes(recipe_id);
