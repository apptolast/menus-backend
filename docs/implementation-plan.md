# Menus Backend — Implementation Plan: Ingredients, Recipes & Menu Publishing

> Generado por agente architect. Fecha: 2026-03-11.
> Basado en: codebase actual (commit d4e612d), technical-spec.md, architecture.md.

---

## 0. Executive Summary

This document defines the implementation plan for extending menus-backend with:

1. **ingredient** module — Global ingredient catalog with allergen mappings
2. **recipe** module — Per-restaurant recipes composed of ingredients, with computed allergens
3. **dish modification** — Optional `recipe_id` FK so dish allergens can be derived from recipes
4. **menu modification** — `published` boolean for digital card functionality
5. **dashboard** module — Read-only analytics (deferred to a later phase, out of scope for this plan)

The plan covers Flyway migrations (V10-V15), DDL, JPA entities, repository/service/controller files, and architectural decisions with justifications.

---

## 1. Architectural Decisions

### ADR-06: Ingredients Are Global (No tenant_id, No RLS)

**Context**: Ingredients represent universal food items (flour, milk, eggs). Multiple restaurants use the same ingredients. The prompt specifies ingredients as global.

**Decision**: The `ingredient` table has NO `tenant_id` column and NO RLS policy. It follows the same pattern as `allergen` and `allergen_translation` — shared reference data accessible to all tenants.

**No `created_by` column for MVP**. Rationale:
- For MVP, ingredients are seeded or created by any authenticated RESTAURANT_OWNER
- Adding `created_by` implies ownership semantics (who can edit/delete?) which adds RBAC complexity disproportionate to MVP value
- If needed later, a V16 migration can add `created_by UUID` as a nullable column
- The `allergen_audit_log` pattern already exists for tracking changes; we can extend it to ingredients if needed

**Consequences**:
- Any RESTAURANT_OWNER can create ingredients (they benefit everyone)
- Any RESTAURANT_OWNER can update ingredients (risk: one restaurant changes an ingredient another uses)
- Mitigation: in the service layer, only ADMIN role can UPDATE/DELETE ingredients; RESTAURANT_OWNER can only CREATE and READ
- `ingredient_allergen` also has no tenant_id (same rationale as the parent ingredient)

**References**:
- Existing pattern: `allergen` table (SERIAL PK, no tenant_id, no RLS) — see V2__create_reference_tables.sql
- [AWS Multi-tenant RLS best practices](https://docs.aws.amazon.com/prescriptive-guidance/latest/saas-multitenant-managed-postgresql/rls.html): "shared reference tables have no tenant_id and RLS is disabled"
- [Crunchy Data RLS for Tenants](https://www.crunchydata.com/blog/row-level-security-for-tenants-in-postgres): "global tables should not have tenant policies"

---

### ADR-07: recipe_ingredient Uses UUID PK (Not Composite)

**Context**: A recipe can use the same ingredient multiple times in different sub-preparations (e.g., "flour in dough" + "flour in coating"). The question is whether `recipe_ingredient` should have a composite PK `(recipe_id, ingredient_id)` or a UUID PK allowing duplicates.

**Decision**: Use a **UUID primary key** with a **non-unique** combination of `(recipe_id, ingredient_id)`. This allows the same ingredient to appear multiple times in a recipe with different quantities, units, or notes.

**Rationale**:
- Real-world recipes frequently reuse ingredients in different steps/sub-preparations
- The existing codebase uses UUID PKs for all join-like tables (`dish_allergen` has UUID PK with UNIQUE constraint on dish+allergen)
- Unlike dish_allergen (where a dish either contains an allergen or doesn't), recipe_ingredient represents a quantity relationship that can repeat
- Composite PKs add complexity with `@EmbeddedId` or `@IdClass` in JPA — the existing codebase consistently uses `val id: UUID = UUID.randomUUID()` for all entities

**Consequences**:
- Allergen computation must aggregate across ALL rows for a recipe_id (not just one per ingredient)
- Slightly more storage than composite PK (one extra UUID column)
- Consistent with existing codebase patterns

**Alternative considered**: Composite PK with `@EmbeddedId`. Rejected because it breaks the pattern established by all existing entities and prevents duplicate ingredients per recipe.

**References**:
- [Baeldung: Composite Primary Keys in JPA](https://www.baeldung.com/jpa-composite-primary-keys)
- [Vlad Mihalcea: Best way to map composite key](https://vladmihalcea.com/the-best-way-to-map-a-composite-primary-key-with-jpa-and-hibernate/)

---

### ADR-08: ingredient_allergen Uses Relational Table (Not JSONB)

**Context**: Each ingredient maps to one or more allergens with a containment level. Two options: (A) relational `ingredient_allergen` table, or (B) JSONB column in `ingredient`.

**Decision**: Option A — relational `ingredient_allergen` table with `(ingredient_id, allergen_id, containment_level)`.

**Rationale**:
- **Consistency**: The existing `dish_allergen` table uses exactly this pattern (relational join with `containment_level`)
- **Queryability**: SQL JOINs and WHERE clauses on allergen_id are straightforward; JSONB requires `@>` operators and GIN indexes
- **Referential integrity**: FK to `allergen(id)` ensures only valid allergen codes are used
- **Reuse**: The `ContainmentLevel` enum (`CONTAINS`, `MAY_CONTAIN`, `FREE_OF`) already exists in the codebase
- **Allergen computation for recipes**: Computing UNION of allergens across recipe ingredients is a simple SQL JOIN, not a JSONB unnest/aggregate

**JSONB trade-offs (rejected)**:
- Pro: fewer tables, simpler reads for a single ingredient
- Con: no FK integrity, harder to query across ingredients, no index on individual allergens without GIN, breaks the established pattern

**References**:
- Existing pattern: `dish_allergen` table — see V6__create_dish_tables.sql
- `ContainmentLevel` enum — see `com.apptolast.menus.dish.model.enum.ContainmentLevel`

---

### ADR-09: Recipe Allergens Are Computed (Not Stored)

**Context**: A recipe's allergens are the union of all its ingredient allergens. Should we store them redundantly in a `recipe_allergen` table, or compute them on read?

**Decision**: **Compute on read** for MVP. No `recipe_allergen` materialized table.

**Rationale**:
- A recipe typically has 5-20 ingredients, each with 0-3 allergens. The JOIN is trivial for PostgreSQL
- Storing computed allergens creates a synchronization problem: every ingredient allergen change must cascade to all recipes using that ingredient
- The existing `AllergenFilterService` already works with `List<DishAllergen>` — we can adapt it to work with computed allergens from recipe ingredients
- For MVP traffic levels, the performance difference is negligible

**Future optimization** (if needed): Add a materialized view `recipe_computed_allergens` or a cache invalidation strategy when ingredient allergens change.

**Query pattern**:
```sql
SELECT DISTINCT ia.allergen_id, ia.containment_level
FROM recipe_ingredient ri
JOIN ingredient_allergen ia ON ri.ingredient_id = ia.ingredient_id
WHERE ri.recipe_id = :recipeId
```

The highest containment level per allergen wins (CONTAINS > MAY_CONTAIN > FREE_OF).

---

### ADR-10: Dish recipe_id Is Nullable — Dual Allergen Source

**Context**: Currently, dish allergens are manually assigned via `dish_allergen`. With recipes, allergens can be derived. Both modes must coexist.

**Decision**: Add `recipe_id UUID NULL` to `dish` table. Allergen resolution logic:

1. If `dish.recipe_id IS NOT NULL` → compute allergens from recipe ingredients (ignore `dish_allergen` rows)
2. If `dish.recipe_id IS NULL` → use `dish_allergen` rows as before (manual mode)

**No hybrid mode** for MVP. A dish is either recipe-based OR manual. This avoids confusion about which source of truth applies.

**Consequences**:
- `AllergenFilterService` interface stays the same (it receives `List<DishAllergen>`)
- `DishService` must resolve allergens differently based on `recipe_id` presence
- The resolution logic lives in `DishService.resolveDishAllergens(dish)` — a new private method
- Existing API contracts for dish responses remain unchanged (consumers see allergens regardless of source)

---

### ADR-11: Repository Pattern — Follow Existing Convention (JpaRepository Directly)

**Context**: A suggestion was made to use Ports & Adapters with a custom repository interface + JpaRepository adapter. The existing codebase uses `JpaRepository` directly (e.g., `RestaurantRepository : JpaRepository<Restaurant, UUID>`).

**Decision**: Follow the existing convention. All new repositories extend `JpaRepository` directly.

**Rationale**:
- 13 existing repositories all use this pattern — consistency is more important than theoretical purity
- Ports & Adapters adds boilerplate (double interfaces, adapter classes) with no practical benefit for a monolith
- If we ever need to swap persistence (unlikely), we refactor all repositories at once, not module by module
- Spring Data JPA's derived query methods and `@Query` annotations are sufficient

---

## 2. Flyway Migrations (V10-V15)

All new migrations append to the existing V1-V9 sequence. Each migration is atomic and can be rolled back by Flyway (PostgreSQL supports transactional DDL).

### Migration Order and Dependencies

| Order | File | Description | Depends On |
|---|---|---|---|
| V10 | `V10__create_ingredient_tables.sql` | `ingredient` + `ingredient_allergen` tables | V2 (FK allergen_id) |
| V11 | `V11__create_recipe_tables.sql` | `recipe` + `recipe_ingredient` tables (with RLS) | V4 (FK restaurant_id), V10 (FK ingredient_id) |
| V12 | `V12__enable_rls_recipe_tables.sql` | RLS policies for `recipe` and `recipe_ingredient` | V11 |
| V13 | `V13__alter_dish_add_recipe_id.sql` | ALTER dish ADD COLUMN recipe_id | V6 (dish exists), V11 (FK recipe_id) |
| V14 | `V14__alter_menu_add_published.sql` | ALTER menu ADD COLUMN published | V5 (menu exists) |
| V15 | `V15__seed_common_ingredients.sql` | Seed common Spanish ingredients with allergens | V10 |

---

### V10__create_ingredient_tables.sql

```sql
-- Global ingredient catalog (NO tenant_id, NO RLS)
-- Follows the same pattern as allergen/allergen_translation (shared reference data)

CREATE TABLE ingredient (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,     -- lowercase, no accents, for search/dedup
    description     TEXT,
    category        VARCHAR(50),               -- 'DAIRY', 'GRAIN', 'MEAT', 'SEAFOOD', 'VEGETABLE', etc.
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Unique on normalized_name to prevent near-duplicates
CREATE UNIQUE INDEX idx_ingredient_normalized_name ON ingredient(normalized_name);

-- Index for text search
CREATE INDEX idx_ingredient_name_trgm ON ingredient USING gin(normalized_name gin_trgm_ops);

-- Allergen mapping for each ingredient (same pattern as dish_allergen)
CREATE TABLE ingredient_allergen (
    id                  SERIAL PRIMARY KEY,
    ingredient_id       INTEGER NOT NULL REFERENCES ingredient(id) ON DELETE CASCADE,
    allergen_id         INTEGER NOT NULL REFERENCES allergen(id),
    containment_level   VARCHAR(20) NOT NULL CHECK (containment_level IN ('CONTAINS', 'MAY_CONTAIN', 'FREE_OF')),
    notes               TEXT,
    UNIQUE (ingredient_id, allergen_id)
);
```

**Design notes**:
- `SERIAL PRIMARY KEY` for ingredient (not UUID) — follows `allergen` table pattern for reference data. Small table, autoincrement is efficient.
- `SERIAL PRIMARY KEY` for ingredient_allergen — same rationale. This is a lookup table, not a tenant entity.
- `normalized_name` enables deduplication: "Harina de trigo" and "harina de trigo" map to the same normalized form.
- `category` is a simple VARCHAR (not an enum table) for MVP flexibility.
- `ON DELETE CASCADE` on ingredient_allergen: deleting an ingredient removes its allergen mappings.
- The `gin_trgm_ops` index requires the `pg_trgm` extension. We need to add it to V10 or to V1 (see note below).

**pg_trgm extension**: We need `CREATE EXTENSION IF NOT EXISTS pg_trgm;` at the top of V10. This is safe to add in a new migration (extensions are schema-level, idempotent with IF NOT EXISTS).

**Revised V10 header**:
```sql
-- Enable trigram extension for fuzzy text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- (rest of the DDL above)
```

---

### V11__create_recipe_tables.sql

```sql
-- Recipes belong to a restaurant (tenant-scoped, with RLS)

CREATE TABLE recipe (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    restaurant_id   UUID NOT NULL REFERENCES restaurant(id),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    servings        INTEGER DEFAULT 1,
    prep_time_min   INTEGER,                   -- preparation time in minutes
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_recipe_restaurant ON recipe(restaurant_id);
CREATE INDEX idx_recipe_tenant ON recipe(tenant_id);

-- Recipe ingredients: links recipe to global ingredients with quantity
-- UUID PK allows same ingredient multiple times per recipe (ADR-07)
CREATE TABLE recipe_ingredient (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipe_id       UUID NOT NULL REFERENCES recipe(id) ON DELETE CASCADE,
    ingredient_id   INTEGER NOT NULL REFERENCES ingredient(id),
    tenant_id       UUID NOT NULL,
    quantity        DECIMAL(10,3),              -- nullable: some ingredients are "to taste"
    unit            VARCHAR(30),               -- 'g', 'kg', 'ml', 'l', 'units', 'tbsp', 'tsp', etc.
    notes           TEXT,                      -- e.g., "for the dough", "for the coating"
    display_order   INTEGER DEFAULT 0
);

CREATE INDEX idx_recipe_ingredient_recipe ON recipe_ingredient(recipe_id);
CREATE INDEX idx_recipe_ingredient_ingredient ON recipe_ingredient(ingredient_id);
```

**Design notes**:
- `recipe` has `tenant_id` and `restaurant_id` — follows the exact same pattern as `menu` table
- `recipe_ingredient` has `tenant_id` — follows the same pattern as `menu_section` (child of a tenant-scoped parent)
- `ON DELETE CASCADE` on recipe_ingredient: deleting a recipe removes all its ingredients
- No cascade from ingredient deletion to recipe_ingredient: deleting a global ingredient should fail if recipes reference it (FK constraint protects data integrity)
- `servings` and `prep_time_min` are optional metadata fields useful for the dashboard module later

---

### V12__enable_rls_recipe_tables.sql

```sql
-- Enable RLS on recipe tables (same pattern as V8)

ALTER TABLE recipe ENABLE ROW LEVEL SECURITY;
ALTER TABLE recipe_ingredient ENABLE ROW LEVEL SECURITY;

ALTER TABLE recipe FORCE ROW LEVEL SECURITY;
ALTER TABLE recipe_ingredient FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_recipe ON recipe
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);

CREATE POLICY tenant_isolation_recipe_ingredient ON recipe_ingredient
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);
```

**Note**: Separated from V11 to follow the existing convention (V6 creates tables, V8 enables RLS). This separation makes it easier to debug RLS issues independently of table creation.

---

### V13__alter_dish_add_recipe_id.sql

```sql
-- Add optional recipe_id to dish table
-- If recipe_id IS NOT NULL, allergens are derived from recipe ingredients
-- If recipe_id IS NULL, dish_allergen rows are used (manual mode, existing behavior)

ALTER TABLE dish ADD COLUMN recipe_id UUID REFERENCES recipe(id) ON DELETE SET NULL;

CREATE INDEX idx_dish_recipe ON dish(recipe_id) WHERE recipe_id IS NOT NULL;

COMMENT ON COLUMN dish.recipe_id IS
    'Optional FK to recipe. When set, dish allergens are computed from recipe ingredients. When NULL, manual dish_allergen entries apply.';
```

**Design notes**:
- `ON DELETE SET NULL`: if a recipe is deleted, the dish reverts to manual allergen mode (preserving the dish itself)
- Partial index `WHERE recipe_id IS NOT NULL`: only indexes rows that actually have a recipe, saving space
- This is a non-breaking change: all existing dishes have `recipe_id = NULL` and continue to work as before
- No DEFAULT needed: NULL is the implicit default for new nullable columns

---

### V14__alter_menu_add_published.sql

```sql
-- Add published flag to menu table for "Digital Card" functionality
-- published = true means the menu is visible as the restaurant's digital card

ALTER TABLE menu ADD COLUMN published BOOLEAN DEFAULT FALSE NOT NULL;

COMMENT ON COLUMN menu.published IS
    'When true, this menu serves as the restaurant digital card. Only one menu per restaurant should be published at a time (enforced in application layer).';
```

**Design notes**:
- `DEFAULT FALSE NOT NULL`: existing menus default to unpublished (non-breaking)
- The constraint "only one published menu per restaurant" is enforced in the application layer, not via a partial unique index, because:
  - A partial unique index `CREATE UNIQUE INDEX ... ON menu(restaurant_id) WHERE published = true AND is_archived = false` works but is fragile across RLS boundaries
  - The service layer already validates business rules for menu operations
  - If strict DB-level enforcement is needed later, the partial unique index can be added in a subsequent migration

---

### V15__seed_common_ingredients.sql

```sql
-- Seed common Spanish ingredients with their allergen mappings
-- This provides a starter catalog so restaurants don't start from zero

-- Helper function for normalized names (lowercase, no accents)
-- Note: unaccent extension needed
CREATE EXTENSION IF NOT EXISTS unaccent;

INSERT INTO ingredient (name, normalized_name, category) VALUES
    ('Harina de trigo', 'harina de trigo', 'GRAIN'),
    ('Leche entera', 'leche entera', 'DAIRY'),
    ('Huevos de gallina', 'huevos de gallina', 'EGGS'),
    ('Mantequilla', 'mantequilla', 'DAIRY'),
    ('Nata', 'nata', 'DAIRY'),
    ('Queso parmesano', 'queso parmesano', 'DAIRY'),
    ('Gambas', 'gambas', 'SEAFOOD'),
    ('Mejillones', 'mejillones', 'SEAFOOD'),
    ('Salmón', 'salmon', 'SEAFOOD'),
    ('Anchoas', 'anchoas', 'SEAFOOD'),
    ('Cacahuetes', 'cacahuetes', 'NUTS_LEGUMES'),
    ('Nueces', 'nueces', 'NUTS_LEGUMES'),
    ('Almendras', 'almendras', 'NUTS_LEGUMES'),
    ('Soja (salsa)', 'soja salsa', 'CONDIMENT'),
    ('Mostaza', 'mostaza', 'CONDIMENT'),
    ('Sésamo (semillas)', 'sesamo semillas', 'SEEDS'),
    ('Apio', 'apio', 'VEGETABLE'),
    ('Altramuces', 'altramuces', 'LEGUME'),
    ('Pan rallado', 'pan rallado', 'GRAIN'),
    ('Cerveza', 'cerveza', 'BEVERAGE')
ON CONFLICT (normalized_name) DO NOTHING;

-- Map ingredients to allergens
-- Uses subqueries to reference allergen codes (not hardcoded IDs)
INSERT INTO ingredient_allergen (ingredient_id, allergen_id, containment_level) VALUES
    ((SELECT id FROM ingredient WHERE normalized_name = 'harina de trigo'), (SELECT id FROM allergen WHERE code = 'GLUTEN'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'leche entera'), (SELECT id FROM allergen WHERE code = 'MILK'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'huevos de gallina'), (SELECT id FROM allergen WHERE code = 'EGGS'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'mantequilla'), (SELECT id FROM allergen WHERE code = 'MILK'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'nata'), (SELECT id FROM allergen WHERE code = 'MILK'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'queso parmesano'), (SELECT id FROM allergen WHERE code = 'MILK'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'gambas'), (SELECT id FROM allergen WHERE code = 'CRUSTACEANS'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'mejillones'), (SELECT id FROM allergen WHERE code = 'MOLLUSCS'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'salmon'), (SELECT id FROM allergen WHERE code = 'FISH'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'anchoas'), (SELECT id FROM allergen WHERE code = 'FISH'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'cacahuetes'), (SELECT id FROM allergen WHERE code = 'PEANUTS'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'nueces'), (SELECT id FROM allergen WHERE code = 'NUTS'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'almendras'), (SELECT id FROM allergen WHERE code = 'NUTS'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'soja salsa'), (SELECT id FROM allergen WHERE code = 'SOYBEANS'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'mostaza'), (SELECT id FROM allergen WHERE code = 'MUSTARD'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'sesamo semillas'), (SELECT id FROM allergen WHERE code = 'SESAME'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'apio'), (SELECT id FROM allergen WHERE code = 'CELERY'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'altramuces'), (SELECT id FROM allergen WHERE code = 'LUPIN'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'pan rallado'), (SELECT id FROM allergen WHERE code = 'GLUTEN'), 'CONTAINS'),
    ((SELECT id FROM ingredient WHERE normalized_name = 'cerveza'), (SELECT id FROM allergen WHERE code = 'GLUTEN'), 'MAY_CONTAIN')
ON CONFLICT (ingredient_id, allergen_id) DO NOTHING;
```

---

## 3. Files to Create/Modify — By Module

### 3.1 Module: ingredient (NEW)

**Location**: `src/main/kotlin/com/apptolast/menus/ingredient/`

```
ingredient/
├── controller/
│   └── IngredientController.kt           # CRUD + search (public GET, auth POST)
├── dto/
│   ├── request/
│   │   ├── CreateIngredientRequest.kt    # name, category, allergens[]
│   │   └── UpdateIngredientRequest.kt    # name, category, allergens[]
│   └── response/
│       └── IngredientResponse.kt         # id, name, category, allergens[]
├── model/
│   ├── entity/
│   │   ├── Ingredient.kt                # JPA entity (SERIAL PK)
│   │   └── IngredientAllergen.kt        # JPA entity (SERIAL PK)
│   └── enum/
│       └── IngredientCategory.kt        # DAIRY, GRAIN, MEAT, SEAFOOD, etc.
├── repository/
│   ├── IngredientRepository.kt          # extends JpaRepository<Ingredient, Int>
│   └── IngredientAllergenRepository.kt  # extends JpaRepository<IngredientAllergen, Int>
├── service/
│   ├── IngredientService.kt             # interface
│   └── impl/
│       └── IngredientServiceImpl.kt     # CRUD + allergen mapping
├── mapper/
│   └── IngredientMapper.kt              # Extension functions toResponse()
└── exception/
    └── IngredientNotFoundException.kt   # extends ResourceNotFoundException
```

**Total new files: 13**

### 3.2 Module: recipe (NEW)

**Location**: `src/main/kotlin/com/apptolast/menus/recipe/`

```
recipe/
├── controller/
│   └── AdminRecipeController.kt         # /api/v1/admin/recipes — CRUD
├── dto/
│   ├── request/
│   │   ├── CreateRecipeRequest.kt       # name, description, servings, ingredients[]
│   │   ├── UpdateRecipeRequest.kt       # name, description, servings, ingredients[]
│   │   └── RecipeIngredientRequest.kt   # ingredientId, quantity, unit, notes
│   └── response/
│       ├── RecipeResponse.kt            # Full recipe with ingredients and computed allergens
│       ├── RecipeIngredientResponse.kt  # ingredient detail within a recipe
│       └── RecipeSummaryResponse.kt     # Lightweight for lists (no ingredients)
├── model/
│   └── entity/
│       ├── Recipe.kt                    # JPA entity (UUID PK, tenant_id)
│       └── RecipeIngredient.kt          # JPA entity (UUID PK, tenant_id)
├── repository/
│   ├── RecipeRepository.kt             # extends JpaRepository<Recipe, UUID>
│   └── RecipeIngredientRepository.kt   # extends JpaRepository<RecipeIngredient, UUID>
├── service/
│   ├── RecipeService.kt                # interface
│   ├── RecipeAllergenComputeService.kt # interface: compute allergens from recipe ingredients
│   └── impl/
│       ├── RecipeServiceImpl.kt        # CRUD
│       └── RecipeAllergenComputeServiceImpl.kt # UNION allergens with max containment
├── mapper/
│   └── RecipeMapper.kt                 # Extension functions toResponse()
└── exception/
    └── RecipeNotFoundException.kt      # extends ResourceNotFoundException
```

**Total new files: 16**

### 3.3 Module: dish (MODIFY)

**Files to modify**:

| File | Change |
|---|---|
| `dish/model/entity/Dish.kt` | Add `recipeId: UUID?` column mapping and `@ManyToOne recipe: Recipe?` relationship |
| `dish/dto/request/DishRequest.kt` | Add `recipeId: UUID?` field |
| `dish/dto/response/DishResponse.kt` | Add `recipeId: UUID?` and `recipeName: String?` fields |
| `dish/service/DishService.kt` | No interface change (allergen resolution is internal) |
| `dish/service/impl/DishServiceImpl.kt` | Add `resolveDishAllergens(dish)` method that checks `recipe_id` and delegates to `RecipeAllergenComputeService` or reads `dish_allergen` |
| `dish/repository/DishRepository.kt` | Add `findWithRecipeAndAllergensBySectionId()` JPQL query |

**Total modified files: 6**

### 3.4 Module: menu (MODIFY)

**Files to modify**:

| File | Change |
|---|---|
| `menu/model/entity/Menu.kt` | Add `published: Boolean = false` column mapping |
| `menu/dto/request/MenuRequest.kt` | Add `published: Boolean?` field |
| `menu/dto/response/MenuResponse.kt` | Add `published: Boolean` field |
| `menu/service/MenuService.kt` | Add `publish(menuId, tenantId)` and `unpublish(menuId, tenantId)` |
| `menu/service/impl/MenuServiceImpl.kt` | Implement publish/unpublish with "only one published per restaurant" validation |
| `menu/controller/AdminMenuController.kt` | Add `POST /menus/{id}/publish` and `DELETE /menus/{id}/publish` |
| `menu/repository/MenuRepository.kt` | Add `findByRestaurantIdAndPublishedTrue()` query |

**Total modified files: 7**

### 3.5 Module: config (MODIFY)

**Files to modify**:

| File | Change |
|---|---|
| `config/SecurityConfig.kt` | Add authorization rules for `/api/v1/ingredients/**` and `/api/v1/admin/recipes/**` |

**Total modified files: 1**

### 3.6 Flyway Migrations (NEW)

**Location**: `src/main/resources/db/migration/`

| File | Status |
|---|---|
| `V10__create_ingredient_tables.sql` | NEW |
| `V11__create_recipe_tables.sql` | NEW |
| `V12__enable_rls_recipe_tables.sql` | NEW |
| `V13__alter_dish_add_recipe_id.sql` | NEW |
| `V14__alter_menu_add_published.sql` | NEW |
| `V15__seed_common_ingredients.sql` | NEW |

**Total new migration files: 6**

---

## 4. JPA Entity Definitions

### 4.1 Ingredient.kt

```kotlin
package com.apptolast.menus.ingredient.model.entity

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "ingredient")
class Ingredient(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "name", nullable = false, length = 255)
    var name: String = "",

    @Column(name = "normalized_name", nullable = false, unique = true, length = 255)
    var normalizedName: String = "",

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "category", length = 50)
    var category: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @OneToMany(mappedBy = "ingredient", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val allergens: MutableList<IngredientAllergen> = mutableListOf()
)
```

### 4.2 IngredientAllergen.kt

```kotlin
package com.apptolast.menus.ingredient.model.entity

import com.apptolast.menus.allergen.model.entity.Allergen
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import jakarta.persistence.*

@Entity
@Table(
    name = "ingredient_allergen",
    uniqueConstraints = [UniqueConstraint(columnNames = ["ingredient_id", "allergen_id"])]
)
class IngredientAllergen(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    val ingredient: Ingredient = Ingredient(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allergen_id", nullable = false)
    val allergen: Allergen = Allergen(),

    @Enumerated(EnumType.STRING)
    @Column(name = "containment_level", nullable = false, length = 20)
    var containmentLevel: ContainmentLevel = ContainmentLevel.CONTAINS,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null
)
```

### 4.3 Recipe.kt

```kotlin
package com.apptolast.menus.recipe.model.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "recipe")
class Recipe(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "restaurant_id", nullable = false)
    val restaurantId: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false, length = 255)
    var name: String = "",

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "servings")
    var servings: Int = 1,

    @Column(name = "prep_time_min")
    var prepTimeMin: Int? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @OneToMany(mappedBy = "recipe", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val ingredients: MutableList<RecipeIngredient> = mutableListOf()
)
```

### 4.4 RecipeIngredient.kt

```kotlin
package com.apptolast.menus.recipe.model.entity

import com.apptolast.menus.ingredient.model.entity.Ingredient
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "recipe_ingredient")
class RecipeIngredient(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    val recipe: Recipe = Recipe(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    val ingredient: Ingredient = Ingredient(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),

    @Column(name = "quantity", precision = 10, scale = 3)
    var quantity: BigDecimal? = null,

    @Column(name = "unit", length = 30)
    var unit: String? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0
)
```

### 4.5 Dish.kt — Modifications

Add to existing Dish entity:

```kotlin
// New field to add to Dish.kt:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "recipe_id")
var recipe: Recipe? = null,
```

### 4.6 Menu.kt — Modifications

Add to existing Menu entity:

```kotlin
// New field to add to Menu.kt:
@Column(name = "published", nullable = false)
var published: Boolean = false,
```

---

## 5. API Endpoints — New and Modified

### 5.1 Ingredient Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/ingredients` | Public | List/search ingredients (query: ?name=, ?category=) |
| GET | `/api/v1/ingredients/{id}` | Public | Get ingredient with allergens |
| POST | `/api/v1/ingredients` | JWT (RESTAURANT_OWNER, ADMIN) | Create ingredient |
| PUT | `/api/v1/ingredients/{id}` | JWT (ADMIN only) | Update ingredient |
| DELETE | `/api/v1/ingredients/{id}` | JWT (ADMIN only) | Delete ingredient |

**SecurityConfig additions**:
```kotlin
it.requestMatchers(GET, "/api/v1/ingredients/**").permitAll()
it.requestMatchers(POST, "/api/v1/ingredients/**").hasAnyRole("RESTAURANT_OWNER", "ADMIN")
it.requestMatchers(PUT, "/api/v1/ingredients/**").hasRole("ADMIN")
it.requestMatchers(DELETE, "/api/v1/ingredients/**").hasRole("ADMIN")
```

### 5.2 Recipe Endpoints (Admin)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/admin/recipes` | JWT (RESTAURANT_OWNER) | List recipes for own restaurant |
| GET | `/api/v1/admin/recipes/{id}` | JWT (RESTAURANT_OWNER) | Get recipe with ingredients and computed allergens |
| POST | `/api/v1/admin/recipes` | JWT (RESTAURANT_OWNER) | Create recipe with ingredients |
| PUT | `/api/v1/admin/recipes/{id}` | JWT (RESTAURANT_OWNER) | Update recipe |
| DELETE | `/api/v1/admin/recipes/{id}` | JWT (RESTAURANT_OWNER) | Delete recipe (CASCADE to recipe_ingredients; dish.recipe_id SET NULL) |

### 5.3 Menu Publishing Endpoints (Admin, new)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/admin/menus/{id}/publish` | JWT (RESTAURANT_OWNER) | Publish menu as digital card |
| DELETE | `/api/v1/admin/menus/{id}/publish` | JWT (RESTAURANT_OWNER) | Unpublish menu |

### 5.4 Consumer Endpoint (modified)

The existing `GET /api/v1/restaurants/{id}/menu` should prioritize showing the published menu:

```
GET /api/v1/restaurants/{id}/menu
  - If a published menu exists → return it
  - If no published menu → return the first non-archived menu (backward compatibility)
```

---

## 6. Allergen Computation Logic

### 6.1 RecipeAllergenComputeService

```kotlin
interface RecipeAllergenComputeService {
    /**
     * Computes the effective allergens for a recipe by aggregating
     * all ingredient_allergen entries across all recipe_ingredients.
     *
     * For each allergen, the highest containment level wins:
     *   CONTAINS > MAY_CONTAIN > FREE_OF
     *
     * Returns a list suitable for the existing AllergenFilterService.
     */
    fun computeAllergens(recipeId: UUID): List<ComputedAllergen>
}

data class ComputedAllergen(
    val allergenId: Int,
    val allergenCode: String,
    val containmentLevel: ContainmentLevel
)
```

### 6.2 SQL Query for Allergen Computation

```sql
SELECT
    ia.allergen_id,
    a.code AS allergen_code,
    -- Highest containment level wins: CONTAINS > MAY_CONTAIN > FREE_OF
    CASE
        WHEN bool_or(ia.containment_level = 'CONTAINS') THEN 'CONTAINS'
        WHEN bool_or(ia.containment_level = 'MAY_CONTAIN') THEN 'MAY_CONTAIN'
        ELSE 'FREE_OF'
    END AS effective_level
FROM recipe_ingredient ri
JOIN ingredient_allergen ia ON ri.ingredient_id = ia.ingredient_id
JOIN allergen a ON ia.allergen_id = a.id
WHERE ri.recipe_id = :recipeId
GROUP BY ia.allergen_id, a.code;
```

### 6.3 DishService Allergen Resolution

Modified flow in `DishServiceImpl`:

```kotlin
private fun resolveDishAllergens(dish: Dish): List<EffectiveAllergen> {
    return if (dish.recipe != null) {
        // Recipe-based: compute from recipe ingredients
        recipeAllergenComputeService.computeAllergens(dish.recipe!!.id)
    } else {
        // Manual mode: use dish_allergen entries (existing behavior)
        dish.allergens.map { da ->
            EffectiveAllergen(da.allergen.id, da.allergen.code, da.containmentLevel)
        }
    }
}
```

The `EffectiveAllergen` (or `ComputedAllergen`) is then fed into the existing `AllergenFilterService.computeSafetyLevel()` for the semaphore logic. The `AllergenFilterService` interface does NOT change — we adapt the data before passing it in.

---

## 7. build.gradle.kts Changes

**No new dependencies required**. The existing dependencies cover all needs:

- Spring Data JPA: already present (`spring-boot-starter-data-jpa`)
- PostgreSQL driver: already present (`org.postgresql:postgresql`)
- Flyway: already present (`spring-boot-starter-flyway` + `flyway-database-postgresql`)
- Validation: already present (`spring-boot-starter-validation`)
- Jackson Kotlin: already present (even though the group is `tools.jackson.module`, it resolves — see architecture.md E2 for the known issue)

**One known issue**: `tools.jackson.module:jackson-module-kotlin` on line 41 of build.gradle.kts should be `com.fasterxml.jackson.module:jackson-module-kotlin`. This is a pre-existing issue documented in architecture.md (E2) and is NOT part of this plan. The backend-dev-domain agent should fix it separately.

**pg_trgm extension**: Required for ingredient search. No Gradle dependency needed — this is a PostgreSQL server-side extension enabled in V10.

**unaccent extension**: Required for V15 seed normalization. Same as above — PostgreSQL server-side.

---

## 8. Implementation Order (Waves)

The implementation should follow this dependency order:

### Wave 1: Migrations + Entities (backend-dev-domain)
1. Create V10-V15 migration files
2. Create `Ingredient`, `IngredientAllergen` entities
3. Create `Recipe`, `RecipeIngredient` entities
4. Modify `Dish` entity (add `recipe` field)
5. Modify `Menu` entity (add `published` field)
6. Create repositories for all new entities
7. Verify: `./gradlew build -x test` passes

### Wave 2: Services (backend-dev-services)
1. Create `IngredientService` + `IngredientServiceImpl`
2. Create `RecipeService` + `RecipeServiceImpl`
3. Create `RecipeAllergenComputeService` + impl
4. Modify `DishServiceImpl` — add `resolveDishAllergens()` method
5. Modify `MenuServiceImpl` — add publish/unpublish logic
6. Verify: `./gradlew build -x test` passes

### Wave 3: API (backend-dev-api)
1. Create `IngredientController` + DTOs + mapper
2. Create `AdminRecipeController` + DTOs + mapper
3. Modify `AdminMenuController` — add publish/unpublish endpoints
4. Modify `DishRequest`/`DishResponse` DTOs
5. Update `SecurityConfig` with new endpoint rules
6. Verify: `./gradlew build -x test` passes

### Wave 4: Testing (qa-engineer)
1. Unit tests for `RecipeAllergenComputeService` (allergen aggregation logic)
2. Unit tests for modified `AllergenFilterService` with recipe-based dishes
3. Integration tests for ingredient CRUD (no RLS, global)
4. Integration tests for recipe CRUD (with RLS, tenant-scoped)
5. Integration test for dish with recipe_id → computed allergens in semaphore response
6. Integration test for menu publish/unpublish

---

## 9. Risks and Considerations

### 9.1 RLS and Global Ingredients

**Risk**: The `TenantAwareJpaTransactionManager` sets `app.current_tenant` on every transaction. Queries to the `ingredient` table (which has NO RLS) will work fine — RLS only applies to tables where it is enabled. However, queries that JOIN `ingredient` with `recipe_ingredient` (which HAS RLS) need the tenant context set.

**Mitigation**: The existing `TenantFilter` already handles this. Ingredient-only queries (public GET endpoints) work without tenant context. Recipe queries (admin endpoints) require authentication, which provides the tenant context.

### 9.2 Cascade Delete Behavior

**Risk**: Deleting a global ingredient that is used in recipes across multiple restaurants would cascade via FK.

**Mitigation**: The FK from `recipe_ingredient.ingredient_id` to `ingredient.id` does NOT have `ON DELETE CASCADE`. Attempting to delete an ingredient that is referenced by any recipe will fail with a FK constraint violation. This is the desired behavior — global ingredients should not be deletable if they are in use.

### 9.3 Allergen Computation Performance

**Risk**: For a dish with recipe_id, every read requires a JOIN across `recipe_ingredient` + `ingredient_allergen` + `allergen`. For a menu with 50+ dishes all recipe-based, this could become N+1.

**Mitigation**:
- Use a single batch query: load all recipe allergens for all recipe_ids in the menu in one query
- `RecipeAllergenComputeService.computeAllergensForRecipes(recipeIds: List<UUID>)` — batch method
- The typical menu has 20-40 dishes. Even without batch optimization, 40 simple JOINs on indexed FKs are sub-millisecond each

### 9.4 pg_trgm Extension Availability

**Risk**: The `pg_trgm` extension must be available in the PostgreSQL instance. In managed PostgreSQL services (AWS RDS, Google Cloud SQL), it is available by default. In custom deployments, it may need to be installed.

**Mitigation**: `CREATE EXTENSION IF NOT EXISTS pg_trgm` is idempotent. If the extension is not available, the migration will fail clearly, and the DBA can install it. The `gin_trgm_ops` index is optional — ingredient search will work without it (just slower for LIKE queries).

### 9.5 Backward Compatibility

**Risk**: Existing dishes have no recipe_id. Existing menus have no published flag.

**Mitigation**: Both ALTER TABLE migrations add nullable/defaulted columns:
- `dish.recipe_id` defaults to NULL → existing dishes continue in manual allergen mode
- `menu.published` defaults to FALSE → existing menus are unpublished
- No data migration needed. No existing behavior changes.

### 9.6 Jackson Module Group ID

**Pre-existing issue**: `build.gradle.kts` line 41 uses `tools.jackson.module:jackson-module-kotlin` which is the Jackson 3.x coordinate. Spring Boot 4.0.3 may or may not resolve this correctly depending on the BOM. This is documented in architecture.md (E2) and should be fixed independently of this plan.

---

## 10. Summary of All New/Modified Files

### New Files (35 total)

**Migrations (6)**:
- `src/main/resources/db/migration/V10__create_ingredient_tables.sql`
- `src/main/resources/db/migration/V11__create_recipe_tables.sql`
- `src/main/resources/db/migration/V12__enable_rls_recipe_tables.sql`
- `src/main/resources/db/migration/V13__alter_dish_add_recipe_id.sql`
- `src/main/resources/db/migration/V14__alter_menu_add_published.sql`
- `src/main/resources/db/migration/V15__seed_common_ingredients.sql`

**Ingredient module (13)**:
- `src/main/kotlin/com/apptolast/menus/ingredient/controller/IngredientController.kt`
- `src/main/kotlin/com/apptolast/menus/ingredient/dto/request/CreateIngredientRequest.kt`
- `src/main/kotlin/com/apptolast/menus/ingredient/dto/request/UpdateIngredientRequest.kt`
- `src/main/kotlin/com/apptolast/menus/ingredient/dto/response/IngredientResponse.kt`
- `src/main/kotlin/com/apptolast/menus/ingredient/model/entity/Ingredient.kt`
- `src/main/kotlin/com/apptolast/menus/ingredient/model/entity/IngredientAllergen.kt`
- `src/main/kotlin/com/apptolast/menus/ingredient/model/enum/IngredientCategory.kt`
- `src/main/kotlin/com/apptolast/menus/ingredient/repository/IngredientRepository.kt`
- `src/main/kotlin/com/apptolast/menus/ingredient/repository/IngredientAllergenRepository.kt`
- `src/main/kotlin/com/apptolast/menus/ingredient/service/IngredientService.kt`
- `src/main/kotlin/com/apptolast/menus/ingredient/service/impl/IngredientServiceImpl.kt`
- `src/main/kotlin/com/apptolast/menus/ingredient/mapper/IngredientMapper.kt`
- `src/main/kotlin/com/apptolast/menus/ingredient/exception/IngredientNotFoundException.kt`

**Recipe module (16)**:
- `src/main/kotlin/com/apptolast/menus/recipe/controller/AdminRecipeController.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/dto/request/CreateRecipeRequest.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/dto/request/UpdateRecipeRequest.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/dto/request/RecipeIngredientRequest.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/dto/response/RecipeResponse.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/dto/response/RecipeIngredientResponse.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/dto/response/RecipeSummaryResponse.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/model/entity/Recipe.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/model/entity/RecipeIngredient.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/repository/RecipeRepository.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/repository/RecipeIngredientRepository.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/service/RecipeService.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/service/RecipeAllergenComputeService.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/service/impl/RecipeServiceImpl.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/service/impl/RecipeAllergenComputeServiceImpl.kt`
- `src/main/kotlin/com/apptolast/menus/recipe/mapper/RecipeMapper.kt`

### Modified Files (14 total)

- `src/main/kotlin/com/apptolast/menus/dish/model/entity/Dish.kt` — add recipe field
- `src/main/kotlin/com/apptolast/menus/dish/dto/request/DishRequest.kt` — add recipeId
- `src/main/kotlin/com/apptolast/menus/dish/dto/response/DishResponse.kt` — add recipeId, recipeName
- `src/main/kotlin/com/apptolast/menus/dish/service/impl/DishServiceImpl.kt` — add allergen resolution logic
- `src/main/kotlin/com/apptolast/menus/dish/repository/DishRepository.kt` — add recipe-aware query
- `src/main/kotlin/com/apptolast/menus/menu/model/entity/Menu.kt` — add published field
- `src/main/kotlin/com/apptolast/menus/menu/dto/request/MenuRequest.kt` — add published
- `src/main/kotlin/com/apptolast/menus/menu/dto/response/MenuResponse.kt` — add published
- `src/main/kotlin/com/apptolast/menus/menu/service/MenuService.kt` — add publish/unpublish
- `src/main/kotlin/com/apptolast/menus/menu/service/impl/MenuServiceImpl.kt` — implement publish/unpublish
- `src/main/kotlin/com/apptolast/menus/menu/controller/AdminMenuController.kt` — add publish endpoints
- `src/main/kotlin/com/apptolast/menus/menu/repository/MenuRepository.kt` — add published query
- `src/main/kotlin/com/apptolast/menus/menu/controller/MenuController.kt` — prioritize published menu
- `src/main/kotlin/com/apptolast/menus/config/SecurityConfig.kt` — add ingredient/recipe rules

---

## 11. References

- [Flyway migration best practices](https://www.baeldung.com/database-migrations-with-flyway)
- [Flyway ALTER TABLE patterns](https://oneuptime.com/blog/post/2026-01-25-database-migrations-flyway-spring-boot/view)
- [PostgreSQL RLS for multi-tenant apps (AWS)](https://aws.amazon.com/blogs/database/multi-tenant-data-isolation-with-postgresql-row-level-security/)
- [PostgreSQL RLS for tenants (Crunchy Data)](https://www.crunchydata.com/blog/row-level-security-for-tenants-in-postgres)
- [PostgreSQL RLS official docs](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [JPA composite primary keys (Baeldung)](https://www.baeldung.com/jpa-composite-primary-keys)
- [Best way to map composite key with JPA (Vlad Mihalcea)](https://vladmihalcea.com/the-best-way-to-map-a-composite-primary-key-with-jpa-and-hibernate/)
- [Spring Data JPA composite key patterns](https://jpa-buddy.com/blog/the-ultimate-guide-on-composite-ids-in-jpa-entities/)
