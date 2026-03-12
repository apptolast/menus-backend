-- Add optional recipe_id to dish table
-- If recipe_id IS NOT NULL, allergens are derived from recipe ingredients
-- If recipe_id IS NULL, dish_allergen rows are used (manual mode)
ALTER TABLE dish ADD COLUMN recipe_id UUID REFERENCES recipes(id) ON DELETE SET NULL;

CREATE INDEX idx_dish_recipe ON dish(recipe_id) WHERE recipe_id IS NOT NULL;
