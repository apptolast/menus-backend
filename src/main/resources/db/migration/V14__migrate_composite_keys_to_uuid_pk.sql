-- Migrate composite primary keys to UUID PKs with unique constraints.
-- This eliminates JPA first-level cache bugs where merge() is called
-- instead of persist() on entities with pre-set composite keys.

-- 1. recipe_ingredients: (recipe_id, ingredient_id) -> UUID id
ALTER TABLE recipe_ingredients DROP CONSTRAINT recipe_ingredients_pkey;
ALTER TABLE recipe_ingredients ADD COLUMN id UUID DEFAULT uuid_generate_v4() NOT NULL;
ALTER TABLE recipe_ingredients ADD CONSTRAINT recipe_ingredients_pkey PRIMARY KEY (id);
ALTER TABLE recipe_ingredients ADD CONSTRAINT uq_recipe_ingredient UNIQUE (recipe_id, ingredient_id);

-- 2. dish_allergens: (dish_id, allergen_id) -> UUID id
ALTER TABLE dish_allergens DROP CONSTRAINT dish_allergens_pkey;
ALTER TABLE dish_allergens ADD COLUMN id UUID DEFAULT uuid_generate_v4() NOT NULL;
ALTER TABLE dish_allergens ADD CONSTRAINT dish_allergens_pkey PRIMARY KEY (id);
ALTER TABLE dish_allergens ADD CONSTRAINT uq_dish_allergen UNIQUE (dish_id, allergen_id);

-- 3. ingredient_allergens: (ingredient_id, allergen_id) -> UUID id
ALTER TABLE ingredient_allergens DROP CONSTRAINT ingredient_allergens_pkey;
ALTER TABLE ingredient_allergens ADD COLUMN id UUID DEFAULT uuid_generate_v4() NOT NULL;
ALTER TABLE ingredient_allergens ADD CONSTRAINT ingredient_allergens_pkey PRIMARY KEY (id);
ALTER TABLE ingredient_allergens ADD CONSTRAINT uq_ingredient_allergen UNIQUE (ingredient_id, allergen_id);
