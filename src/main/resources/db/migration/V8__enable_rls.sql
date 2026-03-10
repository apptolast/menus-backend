-- Row-Level Security: only access own tenant's data
-- app.current_tenant is set by Spring's TenantConfig filter before each request

ALTER TABLE restaurant ENABLE ROW LEVEL SECURITY;
ALTER TABLE menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE menu_section ENABLE ROW LEVEL SECURITY;
ALTER TABLE dish ENABLE ROW LEVEL SECURITY;
ALTER TABLE dish_allergen ENABLE ROW LEVEL SECURITY;

-- Only superuser/postgres can bypass RLS (for migrations)
ALTER TABLE restaurant FORCE ROW LEVEL SECURITY;
ALTER TABLE menu FORCE ROW LEVEL SECURITY;
ALTER TABLE menu_section FORCE ROW LEVEL SECURITY;
ALTER TABLE dish FORCE ROW LEVEL SECURITY;
ALTER TABLE dish_allergen FORCE ROW LEVEL SECURITY;

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
