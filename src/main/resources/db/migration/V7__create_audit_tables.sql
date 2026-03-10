-- Audit log for allergen changes (GDPR compliance)
-- Uses profile_uuid (NOT user_id) to maintain pseudonymization
CREATE TABLE allergen_audit_log (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dish_id         UUID NOT NULL,
    allergen_id     INTEGER NOT NULL,
    tenant_id       UUID NOT NULL,
    changed_by_uuid UUID NOT NULL,
    action          VARCHAR(20) NOT NULL CHECK (action IN ('ADD', 'REMOVE', 'UPDATE')),
    old_level       VARCHAR(20),
    new_level       VARCHAR(20),
    changed_at      TIMESTAMPTZ DEFAULT NOW()
);
