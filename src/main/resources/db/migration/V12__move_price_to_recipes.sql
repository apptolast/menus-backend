-- Move price from dishes to recipes
ALTER TABLE recipes ADD COLUMN price NUMERIC(10,2);

-- Migrate existing prices from dishes to their associated recipes
UPDATE recipes r
SET price = d.price
FROM dishes d
WHERE d.recipe_id = r.id
  AND d.price IS NOT NULL;

-- Remove price from dishes (now managed via recipes)
ALTER TABLE dishes DROP COLUMN price;
