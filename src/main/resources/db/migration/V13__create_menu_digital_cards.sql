CREATE TABLE menu_digital_cards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    menu_id UUID NOT NULL REFERENCES menus(id) ON DELETE CASCADE,
    dish_id UUID NOT NULL REFERENCES dishes(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (menu_id, dish_id)
);

CREATE INDEX idx_menu_digital_cards_menu ON menu_digital_cards(menu_id);
CREATE INDEX idx_menu_digital_cards_dish ON menu_digital_cards(dish_id);
