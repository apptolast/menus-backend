-- Row-Level Security: only access own tenant's data
-- app.current_tenant is set by Spring's TenantConfig filter before each request

ALTER TABLE restaurant ENABLE ROW LEVEL SECURITY;
ALTER TABLE menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE menu_section ENABLE ROW LEVEL SECURITY;
ALTER TABLE dish ENABLE ROW LEVEL SECURITY;
ALTER TABLE dish_allergen ENABLE ROW LEVEL SECURITY;

-- NOTE: FORCE ROW LEVEL SECURITY is intentionally omitted so that the table owner
-- (the application DB user used by Flyway) can bypass RLS for migrations.
-- RLS policies below enforce tenant isolation for all non-owner connections.

CREATE POLICY tenant_isolation_restaurant ON restaurant
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);

CREATE POLICY tenant_isolation_menu ON menu
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);

CREATE POLICY tenant_isolation_menu_section ON menu_section
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);

CREATE POLICY tenant_isolation_dish ON dish
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);

CREATE POLICY tenant_isolation_dish_allergen ON dish_allergen
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);
