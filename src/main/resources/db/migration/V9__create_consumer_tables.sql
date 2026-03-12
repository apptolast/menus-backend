CREATE TABLE consumer_allergen_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    allergen_codes TEXT[] NOT NULL DEFAULT '{}',
    severity_notes TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_favorite_restaurants (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, restaurant_id)
);

CREATE INDEX idx_consumer_profiles_user ON consumer_allergen_profiles(user_id);
CREATE INDEX idx_favorites_user ON user_favorite_restaurants(user_id);
