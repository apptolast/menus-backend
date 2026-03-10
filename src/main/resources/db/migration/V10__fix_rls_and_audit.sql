-- V10: Fix audit table and RLS policies

-- 1. Fix changed_at to be NOT NULL (it has a DEFAULT so no existing rows will be affected)
ALTER TABLE allergen_audit_log
    ALTER COLUMN changed_at SET NOT NULL;

-- 2. Add indexes for common queries on audit log
CREATE INDEX IF NOT EXISTS idx_allergen_audit_tenant_changed
    ON allergen_audit_log (tenant_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_allergen_audit_dish_changed
    ON allergen_audit_log (dish_id, changed_at DESC);

-- 3. Enable and force RLS on allergen_audit_log (was missing in V8)
ALTER TABLE allergen_audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE allergen_audit_log FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_allergen_audit ON allergen_audit_log
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- 4. Remove FORCE RLS from restaurant table so the app user (table owner) can query
--    it without a tenant set (needed to bootstrap the tenant context at login time).
--    The ENABLE RLS policy remains for non-owner connections.
ALTER TABLE restaurant NO FORCE ROW LEVEL SECURITY;

-- 5. Update RLS policies on public-browsing tables (menu, menu_section, dish, dish_allergen)
--    to allow unrestricted SELECT (public catalogue), while keeping write isolation.

-- menu: public SELECT, tenant-isolated writes
DROP POLICY IF EXISTS tenant_isolation_menu ON menu;
CREATE POLICY menu_public_read ON menu FOR SELECT USING (true);
CREATE POLICY menu_tenant_write ON menu FOR INSERT
    WITH CHECK (tenant_id = current_setting('app.current_tenant', false)::UUID);
CREATE POLICY menu_tenant_update ON menu FOR UPDATE
    USING (tenant_id = current_setting('app.current_tenant', false)::UUID);
CREATE POLICY menu_tenant_delete ON menu FOR DELETE
    USING (tenant_id = current_setting('app.current_tenant', false)::UUID);

-- menu_section: public SELECT, tenant-isolated writes
DROP POLICY IF EXISTS tenant_isolation_menu_section ON menu_section;
CREATE POLICY menu_section_public_read ON menu_section FOR SELECT USING (true);
CREATE POLICY menu_section_tenant_write ON menu_section FOR INSERT
    WITH CHECK (tenant_id = current_setting('app.current_tenant', false)::UUID);
CREATE POLICY menu_section_tenant_update ON menu_section FOR UPDATE
    USING (tenant_id = current_setting('app.current_tenant', false)::UUID);
CREATE POLICY menu_section_tenant_delete ON menu_section FOR DELETE
    USING (tenant_id = current_setting('app.current_tenant', false)::UUID);

-- dish: public SELECT, tenant-isolated writes
DROP POLICY IF EXISTS tenant_isolation_dish ON dish;
CREATE POLICY dish_public_read ON dish FOR SELECT USING (true);
CREATE POLICY dish_tenant_write ON dish FOR INSERT
    WITH CHECK (tenant_id = current_setting('app.current_tenant', false)::UUID);
CREATE POLICY dish_tenant_update ON dish FOR UPDATE
    USING (tenant_id = current_setting('app.current_tenant', false)::UUID);
CREATE POLICY dish_tenant_delete ON dish FOR DELETE
    USING (tenant_id = current_setting('app.current_tenant', false)::UUID);

-- dish_allergen: public SELECT, tenant-isolated writes
DROP POLICY IF EXISTS tenant_isolation_dish_allergen ON dish_allergen;
CREATE POLICY dish_allergen_public_read ON dish_allergen FOR SELECT USING (true);
CREATE POLICY dish_allergen_tenant_write ON dish_allergen FOR INSERT
    WITH CHECK (tenant_id = current_setting('app.current_tenant', false)::UUID);
CREATE POLICY dish_allergen_tenant_update ON dish_allergen FOR UPDATE
    USING (tenant_id = current_setting('app.current_tenant', false)::UUID);
CREATE POLICY dish_allergen_tenant_delete ON dish_allergen FOR DELETE
    USING (tenant_id = current_setting('app.current_tenant', false)::UUID);
