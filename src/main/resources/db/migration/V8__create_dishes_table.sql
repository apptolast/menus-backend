CREATE TABLE dishes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    section_id UUID NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    recipe_id UUID REFERENCES recipes(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(8,2),
    image_url VARCHAR(500),
    available BOOLEAN NOT NULL DEFAULT true,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE dish_allergens (
    dish_id UUID NOT NULL REFERENCES dishes(id) ON DELETE CASCADE,
    allergen_id INT NOT NULL REFERENCES allergens(id),
    containment_level VARCHAR(20) NOT NULL CHECK (containment_level IN ('CONTAINS', 'MAY_CONTAIN')),
    notes TEXT,
    PRIMARY KEY (dish_id, allergen_id)
);

CREATE INDEX idx_dishes_section ON dishes(section_id);
CREATE INDEX idx_dishes_recipe ON dishes(recipe_id);
CREATE INDEX idx_dish_allergens_dish ON dish_allergens(dish_id);
