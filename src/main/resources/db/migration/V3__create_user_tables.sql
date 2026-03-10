-- User tables: personal data + health data SEPARATED (GDPR Art. 9)
CREATE TABLE user_account (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           BYTEA NOT NULL UNIQUE,
    email_hash      VARCHAR(64) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    profile_uuid    UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    role            VARCHAR(30) NOT NULL DEFAULT 'CONSUMER',
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Health data table — NO explicit FK to user_account (GDPR pseudonymization)
CREATE TABLE user_allergen_profile (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_uuid    UUID UNIQUE NOT NULL,
    allergen_codes  TEXT[] NOT NULL DEFAULT '{}',
    severity_notes  TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE consent_record (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_uuid    UUID NOT NULL,
    consent_type    VARCHAR(50) NOT NULL,
    granted         BOOLEAN NOT NULL,
    ip_address      INET,
    user_agent      TEXT,
    granted_at      TIMESTAMPTZ DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ
);

CREATE TABLE oauth_account (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES user_account(id),
    provider        VARCHAR(20) NOT NULL,
    provider_id     VARCHAR(255) NOT NULL,
    email           BYTEA NOT NULL,
    UNIQUE (provider, provider_id)
);
