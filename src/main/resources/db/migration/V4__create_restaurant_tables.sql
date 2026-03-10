CREATE TABLE restaurant (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    owner_id        UUID NOT NULL REFERENCES user_account(id),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) UNIQUE NOT NULL,
    description     TEXT,
    address         TEXT,
    phone           VARCHAR(30),
    logo_url        VARCHAR(500),
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE subscription (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    restaurant_id   UUID UNIQUE NOT NULL REFERENCES restaurant(id),
    tier            VARCHAR(20) NOT NULL DEFAULT 'BASIC',
    max_menus       INTEGER NOT NULL DEFAULT 1,
    max_dishes      INTEGER NOT NULL DEFAULT 50,
    starts_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,
    is_active       BOOLEAN DEFAULT TRUE
);
