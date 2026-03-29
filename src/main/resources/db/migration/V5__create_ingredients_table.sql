CREATE TABLE ingredients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    brand VARCHAR(255),
    label_info TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ingredient_allergens (
    ingredient_id UUID NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
    allergen_id INT NOT NULL REFERENCES allergens(id),
    containment_level VARCHAR(20) NOT NULL CHECK (containment_level IN ('CONTAINS', 'MAY_CONTAIN')),
    PRIMARY KEY (ingredient_id, allergen_id)
);

CREATE INDEX idx_ingredients_name ON ingredients(name);
CREATE INDEX idx_ingredient_allergens_ingredient ON ingredient_allergens(ingredient_id);
