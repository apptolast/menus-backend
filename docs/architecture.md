# Menus Backend — Architecture Document

> Generado por agente architect (T01). Fecha: 2026-03-11.
> Revision: R2 — Reconciliation of Wave 1 implementation vs. new user specification.

---

## 0. RECONCILIATION DECISION DOCUMENT

### Context

Wave 1 implemented ingredient/recipe modules following the existing codebase patterns (direct JpaRepository, relational allergen tables, global ingredient catalog). A new specification from the user requests significant architectural changes:

1. Ports & Adapters repository pattern (custom interface + JpaXxxRepository internally)
2. JSONB allergen storage in ingredients (instead of relational `ingredient_allergen` table)
3. Tenant-scoped ingredients (instead of global catalog)
4. Sub-recipe self-referential recursion in `recipe_ingredients`
5. New `menus` table redesign with `client_name`, `valid_from/to`, `is_published`
6. New `menu_recipes` pivot table (menu-to-recipe, replacing menu-section-dish)
7. New `digital_cards` table with slug, QR, custom CSS
8. New `AllergenType` enum with displayName/abbreviation/emoji/keywords
9. New `TextAnalyzerService` for regex/keyword allergen detection
10. `RecipeAllergenCalculator` with recursive sub-recipe traversal (max depth 10, cycle detection)

### Summary of Decisions

| # | Decision | Verdict | Justification |
|---|---|---|---|
| A | Ports & Adapters for ALL repos | **NO** | Apply ONLY to new modules |
| B | Ports & Adapters for NEW modules | **YES** | ingredient, recipe, digitalcard |
| C | JSONB allergens on ingredients | **YES** | Simpler for per-tenant custom ingredients |
| D | Tenant-scoped ingredients | **YES** | Each restaurant manages its own ingredient catalog |
| E | Sub-recipe recursion | **YES** | Core business requirement |
| F | Delete V10-V15 migrations | **YES** | No production database yet; clean slate |
| G | Redesign menu table | **NO** | Keep existing `menu` table, add new fields via ALTER |
| H | New `menu_recipes` pivot table | **YES** | But as V16+, not replacing existing menu_section/dish |
| I | New `digital_cards` table | **YES** | New V-migration |
| J | TextAnalyzerService | **YES** | Pure domain service, no DB impact |
| K | AllergenType enum enhancement | **YES** | Backward compatible with existing AllergenCode |

---

### Decision A: Ports & Adapters vs Direct JpaRepository

**DECISION: Hybrid approach — Ports & Adapters for NEW modules only.**

**Verdict**: Do NOT refactor the existing 13 repositories. Apply Ports & Adapters ONLY to `ingredient`, `recipe`, `digitalcard`, and any future modules.

**Justification**:

1. **Spring Data JPA fully supports this pattern.** According to the [official Spring Data JPA documentation on custom implementations](https://docs.spring.io/spring-data/jpa/reference/repositories/custom-implementations.html), the fragment-based model allows a custom interface (the "port") to coexist with a `JpaRepository` interface. The repository infrastructure auto-detects `Impl`-suffixed classes via classpath scanning. There are NO conflicts when both exist in the same package.

2. **The pattern in Spring Data works as follows:**
   ```
   // PORT (domain interface, does NOT extend JpaRepository)
   interface IngredientRepository {
       fun findById(id: UUID): Ingredient?
       fun save(ingredient: Ingredient): Ingredient
       fun search(name: String): List<Ingredient>
   }

   // SPRING DATA INTERFACE (internal, package-private or in adapter subpackage)
   interface JpaIngredientRepository : JpaRepository<IngredientEntity, UUID> {
       fun findByNameContainingIgnoreCase(name: String): List<IngredientEntity>
   }

   // ADAPTER (implements the port, uses the JPA interface internally)
   @Repository
   class IngredientRepositoryImpl(
       private val jpa: JpaIngredientRepository
   ) : IngredientRepository {
       override fun findById(id: UUID) = jpa.findById(id).orElse(null)
       override fun save(ingredient: Ingredient) = jpa.save(ingredient)
       override fun search(name: String) = jpa.findByNameContainingIgnoreCase(name)
   }
   ```

3. **Why NOT refactor all 13 existing repos**: The 13 existing repositories (AllergenRepository, UserAccountRepository, MenuRepository, DishRepository, etc.) are stable, well-tested, and follow a consistent pattern. Refactoring them to Ports & Adapters is:
   - High-effort (26+ new files: 13 port interfaces + 13 adapter classes)
   - High-risk (touches every module, breaks all service constructors)
   - Low-value for an MVP (they already work correctly)
   - Not required by the existing CLAUDE.md spec

4. **Why YES for new modules**: New modules (ingredient, recipe, digitalcard) are being written from scratch anyway. Applying Ports & Adapters here:
   - Demonstrates the pattern for future modules
   - Isolates domain logic from Spring Data concerns
   - Makes the new JSONB-based ingredient model testable without Spring context
   - Costs very little extra effort since these files are being rewritten

5. **Spring Data classpath scanning concern**: Spring Data scans for interfaces extending `Repository` or `JpaRepository`. A plain interface named `IngredientRepository` that does NOT extend any Spring Data interface will NOT be picked up by Spring Data's auto-scan. The `JpaIngredientRepository` (extending `JpaRepository`) will be picked up. The `IngredientRepositoryImpl` (annotated `@Repository`) will be picked up by Spring's component scan. There is no conflict. This is confirmed by the official documentation.

6. **Naming convention**: To avoid the Spring Data `Impl`-suffix auto-detection matching the wrong thing, the internal JPA interface should be named differently from the port. Recommended naming:
   - Port: `IngredientRepository` (in `ingredient/repository/`)
   - JPA: `JpaIngredientRepository` (in `ingredient/repository/jpa/`)
   - Adapter: `IngredientRepositoryAdapter` (in `ingredient/repository/`) annotated `@Repository`

   This avoids the `Impl` suffix collision entirely, since Spring Data looks for `IngredientRepositoryImpl` as a fragment implementation for an interface named `IngredientRepository` that extends `JpaRepository`. Since our port does NOT extend `JpaRepository`, Spring Data ignores it completely.

---

### Decision B: Ingredient — JSONB vs Relational

**DECISION: YES to JSONB. Delete `ingredient_allergen` table.**

**Justification**:

1. **The user's spec explicitly requests JSONB.** The new ingredient table has `allergens JSONB NOT NULL DEFAULT '[]'` and `traces JSONB DEFAULT '[]'`.

2. **JSONB is the correct choice for tenant-scoped ingredients** because:
   - Each restaurant defines its own ingredients with custom allergen annotations
   - There is no need to JOIN across restaurants' ingredient allergens
   - Reads are simpler: one SELECT fetches ingredient + allergens in one row
   - The JSONB column stores structured data like `[{"code": "GLUTEN", "level": "CONTAINS"}, ...]`
   - PostgreSQL JSONB supports indexing with GIN for queries like `allergens @> '[{"code":"GLUTEN"}]'`

3. **Why relational was wrong for this case**: The Wave 1 `ingredient_allergen` table with FK to `allergen(id)` assumed ingredients are global and shared across tenants. If ingredients are tenant-scoped, the relational table adds unnecessary complexity for no benefit — there is no cross-tenant JOIN use case.

4. **Consistency note**: `dish_allergen` remains relational (FK to `allergen.id`) because dishes are the core regulatory domain and need FK integrity for EU 1169/2011 compliance. Ingredients are an internal kitchen management concept where JSONB flexibility is appropriate.

---

### Decision C: Ingredient Scoping — Global vs Tenant-scoped

**DECISION: YES, ingredients are tenant-scoped (with `tenant_id` + RLS).**

**Justification**:

1. **The user's spec is contradictory** — the DB diagram says "catalogo global compartido" but the SQL shows `tenant_id UUID NOT NULL` with RLS. The SQL wins because:
   - Different restaurants use different brands, suppliers, and custom ingredients
   - An ingredient named "Harina de trigo" at Restaurant A may have different brand/supplier/notes than at Restaurant B
   - The `created_by UUID REFERENCES user_account(id)` column implies per-restaurant ownership

2. **The 14 EU allergens remain global** (in the `allergen` table, no RLS). Only ingredients become tenant-scoped.

3. **The Wave 1 seed data (V15) with 20 common Spanish ingredients** can be repurposed as a template/import feature, not as shared global rows. Each restaurant would import from a template into their own tenant-scoped ingredients table.

---

### Decision D: Sub-recipe Recursion

**DECISION: YES, implement self-referential `sub_recipe_id` in `recipe_ingredients`.**

**Justification**:

1. This is a core business requirement. Many restaurant kitchens have base preparations (salsas, masas, fondos) that are used as ingredients in multiple recipes. If a base changes its allergens, all parent recipes must automatically reflect the change.

2. The Wave 1 `RecipeAllergenComputeServiceImpl` does a flat query joining `recipe_ingredient -> ingredient_allergen`. This must be replaced with a recursive CTE or application-level recursion that:
   - Traverses `recipe_ingredients.sub_recipe_id` when `ingredient_id IS NULL`
   - Detects cycles (recipe A uses sub-recipe B which uses sub-recipe A)
   - Enforces max depth of 10
   - Aggregates allergens with highest containment level priority: CONTAINS > MAY_CONTAIN > FREE_OF

3. The XOR constraint `CHECK (ingredient_id IS NOT NULL OR sub_recipe_id IS NOT NULL)` ensures each row references either an ingredient or a sub-recipe, never both, never neither.

---

### Decision E: Migration Strategy

**DECISION: Delete V10-V15 and recreate with new spec.**

**Justification**:

1. **No production database exists.** The project is pre-MVP. V10-V15 have never been applied to a live environment.

2. **The changes are too fundamental for additive migrations:**
   - `ingredient` changes from `SERIAL PRIMARY KEY` (global, no RLS) to `UUID PRIMARY KEY` (tenant-scoped, RLS, JSONB allergens)
   - `ingredient_allergen` table is entirely eliminated
   - `recipe_ingredient` gains `sub_recipe_id` and changes `ingredient_id` from `INTEGER` to `UUID`
   - V13 (`ALTER dish ADD recipe_id`) and V14 (`ALTER menu ADD published`) are small enough to keep conceptually, but should be renumbered

3. **New migration plan:**

| Version | File | Content |
|---|---|---|
| V10 | `V10__create_ingredient_table.sql` | `ingredients` table with UUID PK, tenant_id, JSONB allergens/traces, ocr_raw_text, brand, supplier, created_by FK. RLS policy. GIN index on allergens JSONB. |
| V11 | `V11__create_recipe_tables.sql` | `recipes` table with UUID PK, tenant_id, is_sub_elaboration, price, image_url, created_by FK. `recipe_ingredients` with sub_recipe_id self-referential FK, XOR CHECK constraint. RLS on both. |
| V12 | `V12__alter_dish_add_recipe_id.sql` | ALTER dish ADD recipe_id UUID REFERENCES recipes(id). |
| V13 | `V13__create_menu_recipes_table.sql` | `menu_recipes` pivot table (menu_id, recipe_id, section_name, sort_order, tenant_id). RLS. |
| V14 | `V14__alter_menu_add_digital_fields.sql` | ALTER menu ADD is_published BOOLEAN, valid_from TIMESTAMPTZ, valid_to TIMESTAMPTZ, client_name, client_logo_url. |
| V15 | `V15__create_digital_cards_table.sql` | `digital_cards` table with slug, qr_code_url, is_active, custom_css JSONB, tenant_id. RLS. |

---

### Decision F: Menu Table — Redesign vs ALTER

**DECISION: Keep existing `menu` table, add new fields via ALTER (V14).**

**Justification**:

1. The existing `menu` table already has the core structure: `id`, `restaurant_id`, `tenant_id`, `name`, `description`, `is_archived`, `display_order`, `created_at`, `updated_at`. The Wave 1 addition of `published` is also useful.

2. The user's new spec adds: `client_name`, `client_logo_url`, `valid_from`, `valid_to`, `is_published` (replaces `published`). These are all nullable ALTERs — non-breaking.

3. The existing `menu_section` and `dish` tables linked via `section_id` remain valid for the original menu structure. The new `menu_recipes` pivot table adds a parallel path for recipe-based menu composition. Both can coexist.

---

### Decision G: What to DELETE from Wave 1

**Files to DELETE (complete rewrite needed due to spec changes):**

**Migrations (6 files):**
- `src/main/resources/db/migration/V10__create_ingredient_tables.sql` — global SERIAL ingredient + relational ingredient_allergen incompatible with new UUID tenant-scoped JSONB design
- `src/main/resources/db/migration/V11__create_recipe_tables.sql` — missing sub_recipe_id, price, image_url, is_sub_elaboration, created_by
- `src/main/resources/db/migration/V12__enable_rls_recipe_tables.sql` — will be merged into V11
- `src/main/resources/db/migration/V13__alter_dish_add_recipe_id.sql` — renumbered to V12
- `src/main/resources/db/migration/V14__alter_menu_add_published.sql` — replaced by V14 with more fields
- `src/main/resources/db/migration/V15__seed_common_ingredients.sql` — global seed incompatible with tenant-scoped design

**Ingredient entities (3 files):**
- `src/main/kotlin/.../ingredient/model/entity/Ingredient.kt` — SERIAL PK -> UUID PK, no allergens relation, add JSONB fields
- `src/main/kotlin/.../ingredient/model/entity/IngredientAllergen.kt` — entire entity eliminated (JSONB replaces it)
- `src/main/kotlin/.../ingredient/model/enum/IngredientCategory.kt` — may be kept if category enum still needed, but user spec uses free-text VARCHAR(100) category

**Ingredient repositories (2 files):**
- `src/main/kotlin/.../ingredient/repository/IngredientRepository.kt` — extends JpaRepository directly, must become Ports & Adapters
- `src/main/kotlin/.../ingredient/repository/IngredientAllergenRepository.kt` — table eliminated

**Ingredient service (2 files):**
- `src/main/kotlin/.../ingredient/service/IngredientService.kt` — interface changes (UUID id, new fields, JSONB allergens)
- `src/main/kotlin/.../ingredient/service/impl/IngredientServiceImpl.kt` — complete rewrite for new data model

**Ingredient DTOs (3 files):**
- `src/main/kotlin/.../ingredient/dto/request/CreateIngredientRequest.kt` — new fields
- `src/main/kotlin/.../ingredient/dto/request/UpdateIngredientRequest.kt` — new fields
- `src/main/kotlin/.../ingredient/dto/response/IngredientResponse.kt` — new fields, JSONB response shape

**Ingredient mapper (1 file):**
- `src/main/kotlin/.../ingredient/mapper/IngredientMapper.kt` — new entity structure

**Ingredient controller (1 file):**
- `src/main/kotlin/.../ingredient/controller/IngredientController.kt` — path may change to `/api/v1/admin/ingredients` (tenant-scoped, not public)

**Ingredient exception (1 file):**
- `src/main/kotlin/.../ingredient/exception/IngredientNotFoundException.kt` — id type changes from Int to UUID

**Recipe entities (2 files):**
- `src/main/kotlin/.../recipe/model/entity/Recipe.kt` — missing is_sub_elaboration, price, image_url, created_by
- `src/main/kotlin/.../recipe/model/entity/RecipeIngredient.kt` — ingredient_id changes from Int FK to UUID FK, add sub_recipe_id

**Recipe repositories (2 files):**
- `src/main/kotlin/.../recipe/repository/RecipeRepository.kt` — must become Ports & Adapters
- `src/main/kotlin/.../recipe/repository/RecipeIngredientRepository.kt` — must become Ports & Adapters

**Recipe service (4 files):**
- `src/main/kotlin/.../recipe/service/RecipeService.kt` — interface changes for sub-recipes
- `src/main/kotlin/.../recipe/service/impl/RecipeServiceImpl.kt` — sub-recipe handling
- `src/main/kotlin/.../recipe/service/RecipeAllergenComputeService.kt` — must become recursive
- `src/main/kotlin/.../recipe/service/impl/RecipeAllergenComputeServiceImpl.kt` — complete rewrite for recursion + JSONB

**Recipe DTOs (5 files):**
- `src/main/kotlin/.../recipe/dto/request/CreateRecipeRequest.kt` — sub-recipe fields
- `src/main/kotlin/.../recipe/dto/request/UpdateRecipeRequest.kt` — sub-recipe fields
- `src/main/kotlin/.../recipe/dto/request/RecipeIngredientRequest.kt` — sub_recipe_id option
- `src/main/kotlin/.../recipe/dto/response/RecipeResponse.kt` — new fields
- `src/main/kotlin/.../recipe/dto/response/RecipeSummaryResponse.kt` — new fields

**Recipe mapper (1 file):**
- `src/main/kotlin/.../recipe/mapper/RecipeMapper.kt` — new entity structure

**Recipe controller (1 file):**
- `src/main/kotlin/.../recipe/controller/AdminRecipeController.kt` — sub-recipe endpoints

**Recipe exception (1 file):**
- `src/main/kotlin/.../recipe/exception/RecipeNotFoundException.kt` — minor, can be kept as-is

**TOTAL: 30 files to delete/rewrite from Wave 1.**

---

### Decision H: What to KEEP from Wave 1

**Files that can be ADAPTED (modified) rather than deleted:**

1. **`src/main/kotlin/.../dish/model/entity/Dish.kt`** — KEEP the `recipe: Recipe?` field, but update the FK type if Recipe changes.

2. **`src/main/kotlin/.../menu/model/entity/Menu.kt`** — KEEP the `published` field, rename to `isPublished` for consistency, add new fields (`clientName`, `clientLogoUrl`, `validFrom`, `validTo`).

3. **`src/main/kotlin/.../dish/repository/DishRepository.kt`** — KEEP as JpaRepository (existing pattern), the `findWithAllergensAndRecipeBySectionId` query stays valid.

4. **`src/main/kotlin/.../menu/repository/MenuRepository.kt`** — KEEP as JpaRepository (existing pattern), the `findByRestaurantIdAndPublishedTrueAndIsArchivedFalse` query stays valid (field rename `published` -> `isPublished` only).

5. **`src/main/kotlin/.../ingredient/dto/request/AllergenMappingRequest.kt`** — MAY be adaptable for JSONB allergen input, but shape changes significantly.

6. **`src/main/kotlin/.../recipe/exception/RecipeNotFoundException.kt`** — KEEP as-is, no changes needed.

7. **`src/main/kotlin/.../recipe/dto/response/RecipeIngredientResponse.kt`** — ADAPT to include `subRecipeId` option.

---

## 1. Estado Actual del Proyecto — Errores Encontrados

### 1.1 Errores en build.gradle.kts

The current `build.gradle.kts` has been largely corrected since the initial review. Remaining issues:

| # | Issue | Current State | Action Required |
|---|---|---|---|
| E1 | Jackson module group | `tools.jackson.module:jackson-module-kotlin` | Change to `com.fasterxml.jackson.module:jackson-module-kotlin` |
| E2 | Redis dependency | `spring-boot-starter-data-redis` present | KEEP if Redis is in scope for caching/session; otherwise remove |
| E3 | Flyway starter | `spring-boot-starter-flyway` | Verify this artifact exists in Boot 4.0.3; original spec says `org.flywaydb:flyway-core` directly |
| E4 | Testcontainers artifacts | `testcontainers-postgresql` and `testcontainers-junit-jupiter` | Verify correct artifact IDs; official is `org.testcontainers:postgresql` and `org.testcontainers:junit-jupiter` (without `testcontainers-` prefix) |

---

## 2. Arquitectura Objetivo

### 2.1 C4 Context — Vista de Contexto

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SISTEMA: Menus Backend                             │
│                     [Spring Boot 4 API REST / Kotlin 2.2]                   │
└─────────────────────────────────────────────────────────────────────────────┘

Actores externos:
  ┌──────────────┐     HTTP/HTTPS        ┌──────────────────────────────────┐
  │  Consumidor  │ ──────────────────>   │                                  │
  │ (App movil / │   GET /restaurants/   │       Menus Backend API          │
  │   Browser)   │   {id}/menu + JWT     │   menus-api.apptolast.com        │
  └──────────────┘                       │   (Traefik IngressRoute / K8s)   │
                                         │                                  │
  ┌──────────────┐     HTTP/HTTPS        │   Spring Security (JWT HS512)    │
  │  Restaurador │ ──────────────────>   │   Multi-tenancy RLS              │
  │  (App admin) │   /api/v1/admin/**    │   RGPD / Pseudonimizacion        │
  └──────────────┘                       └──────┬───────────────────────────┘
                                                │
                                     ┌──────────┼──────────┐
                                     │          │          │
                                     ▼          ▼          ▼
                              ┌────────────┐  [Google]  [GitHub
                              │ PostgreSQL  │  OAuth2    Actions]
                              │    16      │  API       CI/CD
                              │  (RLS +    │
                              │  pgcrypto) │
                              └────────────┘
```

### 2.2 C4 Container — Vista de Contenedores

```
┌────────────────────────────────────────────────────────────────────┐
│                       Menus Backend API                            │
│                   [Spring Boot 4 / Kotlin 2.2]                     │
│                                                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐          │
│  │  auth/   │  │allergen/ │  │restaurant│  │   menu/   │          │
│  │(JWT,OAuth│  │(14 EU    │  │(tenant,  │  │(menus,    │          │
│  │ consent) │  │ catalog) │  │ subs)    │  │ sections) │          │
│  └──────────┘  └──────────┘  └──────────┘  └───────────┘          │
│                                                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐          │
│  │  dish/   │  │consumer/ │  │ingredient│  │  recipe/  │          │
│  │(plates,  │  │(profile, │  │(JSONB,   │  │(sub-elab, │          │
│  │ semaforo)│  │ RGPD)    │  │ tenant)  │  │ allergen  │          │
│  └──────────┘  └──────────┘  └──────────┘  │ recursion)│          │
│                                             └───────────┘          │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐                        │
│  │  gdpr/   │  │  audit/  │  │digitalcard│                        │
│  │(export,  │  │(allergen │  │(slug, QR, │                        │
│  │ delete)  │  │ changes) │  │ CSS)      │                        │
│  └──────────┘  └──────────┘  └───────────┘                        │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    shared/ + config/                         │   │
│  │  JWT filter, RLS tenant, GlobalExceptionHandler, DTOs       │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────────────────────────────────┐
│                      PostgreSQL 16                                  │
│  pgcrypto (AES-256) | uuid-ossp | pg_trgm | RLS policies          │
│  Tables: allergen, allergen_translation, user_account,             │
│  user_allergen_profile, consent_record, oauth_account,             │
│  restaurant, subscription, menu, menu_section, dish,               │
│  dish_allergen, allergen_audit_log,                                │
│  ingredients (JSONB, tenant), recipes, recipe_ingredients,         │
│  menu_recipes, digital_cards                                       │
└────────────────────────────────────────────────────────────────────┘
```

### 2.3 Repository Architecture — Hybrid Pattern

```
EXISTING MODULES (13 repos) — Direct JpaRepository:
┌─────────────────────────────────────────────────────────┐
│  AllergenRepository extends JpaRepository<Allergen, Int>│
│  UserAccountRepository extends JpaRepository<...>       │
│  MenuRepository extends JpaRepository<...>              │
│  DishRepository extends JpaRepository<...>              │
│  ... (9 more)                                           │
│                                                         │
│  Services inject these directly.                        │
│  Pattern: interface XxxRepository : JpaRepository       │
└─────────────────────────────────────────────────────────┘

NEW MODULES (ingredient, recipe, digitalcard) — Ports & Adapters:
┌─────────────────────────────────────────────────────────┐
│  PORT (domain interface):                               │
│    interface IngredientRepository {                      │
│        fun findById(id: UUID): Ingredient?              │
│        fun save(ingredient: Ingredient): Ingredient     │
│        ...                                              │
│    }                                                    │
│                                                         │
│  JPA INTERFACE (internal):                              │
│    interface JpaIngredientRepository :                   │
│        JpaRepository<Ingredient, UUID> { ... }          │
│                                                         │
│  ADAPTER (implements port, uses JPA internally):        │
│    @Repository                                          │
│    class IngredientRepositoryAdapter(                    │
│        private val jpa: JpaIngredientRepository         │
│    ) : IngredientRepository { ... }                     │
│                                                         │
│  Services inject the PORT interface:                    │
│    class IngredientServiceImpl(                          │
│        private val repo: IngredientRepository // port   │
│    )                                                    │
└─────────────────────────────────────────────────────────┘
```

**Package layout for Ports & Adapters modules:**
```
ingredient/
├── repository/
│   ├── IngredientRepository.kt           # PORT interface (no Spring Data)
│   ├── IngredientRepositoryAdapter.kt    # ADAPTER (@Repository, implements port)
│   └── jpa/
│       └── JpaIngredientRepository.kt    # JpaRepository<Ingredient, UUID>
├── model/entity/
│   └── Ingredient.kt
├── service/
│   ├── IngredientService.kt
│   └── impl/IngredientServiceImpl.kt
├── controller/
│   └── AdminIngredientController.kt
├── dto/...
└── mapper/...
```

---

## 3. Modulos y Responsabilidades

```
com.apptolast.menus/
├── config/          Configuracion transversal (Security, JWT, OpenAPI, Tenant/RLS)
├── shared/          DTOs comunes, excepciones de negocio, seguridad JWT, handler global
├── auth/            Autenticacion: registro, login, refresh token, OAuth2 Google, consentimiento
├── allergen/        Catalogo de 14 alergenos EU (solo lectura publica)
├── restaurant/      Gestion de restaurantes y suscripciones
├── menu/            CRUD de menus y secciones (admin) + vista publica
├── dish/            CRUD de platos + logica semaforo SAFE/RISK/DANGER
├── consumer/        Perfil de alergenos del consumidor (datos de salud separados RGPD)
├── ingredient/      [NEW] Catalogo de ingredientes por tenant (JSONB allergens, Ports&Adapters)
├── recipe/          [NEW] Recetas con sub-elaboraciones recursivas (Ports&Adapters)
├── digitalcard/     [NEW] Carta digital con slug, QR y CSS personalizado (Ports&Adapters)
├── gdpr/            Exportacion, eliminacion y rectificacion de datos personales
└── audit/           Log de auditoria de cambios en alergenos de platos
```

---

## 4. ADRs — Architecture Decision Records

### ADR-01: Monolito Modular vs Microservicios

**Fecha**: 2026-03-10
**Estado**: Aceptado

**Contexto**: El proyecto es una MVP con un equipo pequeno. Se necesita velocidad de desarrollo y simplicidad operacional.

**Decision**: Monolito modular con paquetes claramente delimitados.

**Consecuencias positivas**:
- Un unico deployment, menos complejidad operacional
- Transacciones locales (sin Saga pattern)
- Refactorizacion a microservicios posible en el futuro

**Consecuencias negativas**:
- Escalado horizontal de todo el monolito
- Un fallo puede afectar a todos los modulos

---

### ADR-02: JWT Stateless vs Sesiones HTTP

**Fecha**: 2026-03-10
**Estado**: Aceptado

**Decision**: JWT stateless con access token (15min, HS512) + refresh token (7 dias). Libreria jjwt 0.12.6.

---

### ADR-03: Multi-tenancy via Row-Level Security de PostgreSQL

**Fecha**: 2026-03-10
**Estado**: Aceptado

**Decision**: RLS con `SET app.current_tenant = '<tenantId>'` en cada request. Tablas con RLS: restaurant, menu, menu_section, dish, dish_allergen, ingredients, recipes, recipe_ingredients, menu_recipes, digital_cards.

---

### ADR-04: Pseudonimizacion RGPD

**Fecha**: 2026-03-10
**Estado**: Aceptado

**Decision**: Separacion user_account <-> user_allergen_profile via profile_uuid sin FK. Datos de salud en tabla separada. Consentimiento explicito obligatorio.

---

### ADR-05: Traefik vs nginx como Ingress Controller

**Fecha**: 2026-03-10
**Estado**: Aceptado

**Decision**: Traefik IngressRoute (CRD traefik.io/v1alpha1) con cert-manager + Let's Encrypt.

---

### ADR-06: Hybrid Repository Pattern — Ports & Adapters for New Modules Only

**Fecha**: 2026-03-11
**Estado**: Aceptado

**Contexto**: The user requested Ports & Adapters (hexagonal) repository pattern. The existing codebase has 13 repositories extending JpaRepository directly.

**Decision**: Apply Ports & Adapters ONLY to new modules (ingredient, recipe, digitalcard). Existing 13 repositories remain as direct JpaRepository interfaces.

**Justification**: Refactoring 13 stable repositories adds risk and effort with no functional benefit for MVP. New modules are being written from scratch anyway. Spring Data JPA's fragment-based model supports both patterns coexisting. The naming convention (`JpaXxxRepository` for Spring Data, `XxxRepository` for port, `XxxRepositoryAdapter` for impl) avoids any classpath scanning conflicts.

**Consecuencias positivas**:
- New modules demonstrate the pattern for future code
- Domain logic in new modules is testable without Spring context
- Zero disruption to existing stable code

**Consecuencias negativas**:
- Two repository patterns coexist in the codebase (documented, intentional)
- Future developers must understand which pattern applies to which module

---

### ADR-07: JSONB Allergens in Ingredients vs Relational Table

**Fecha**: 2026-03-11
**Estado**: Aceptado

**Contexto**: Wave 1 implemented a relational `ingredient_allergen` table (FK to `allergen`). The new spec requests JSONB columns for allergens and traces.

**Decision**: Use JSONB `allergens` and `traces` columns in the `ingredients` table. Delete the `ingredient_allergen` relational table.

**Justification**: Ingredients are tenant-scoped. Each restaurant defines its own ingredients with custom allergen annotations. There is no cross-tenant JOIN use case. JSONB provides simpler reads (no JOIN), supports GIN indexing for queries, and aligns with the OCR/text-analysis workflow where raw label data is ingested. The regulatory `dish_allergen` table remains relational with FK integrity.

**Consecuencias positivas**:
- Simpler reads: one SELECT returns ingredient + allergens
- Flexible schema for OCR-parsed label data
- GIN index enables efficient JSONB queries

**Consecuencias negativas**:
- No FK integrity between ingredient allergens and allergen catalog
- Application layer must validate allergen codes
- JSONB is slightly harder to migrate/refactor than relational

---

### ADR-08: Tenant-Scoped Ingredients

**Fecha**: 2026-03-11
**Estado**: Aceptado

**Contexto**: Wave 1 implemented ingredients as a global catalog (no tenant_id, no RLS, SERIAL PK). The new spec shows tenant_id + RLS.

**Decision**: Ingredients are tenant-scoped. Each restaurant manages its own ingredient catalog. UUID PK (not SERIAL). RLS enabled.

**Justification**: Different restaurants use different brands, suppliers, and custom ingredient names. The `created_by` field implies ownership. The 14 EU allergens remain the global reference; ingredients are per-restaurant data.

---

### ADR-09: Sub-Recipe Recursion with Cycle Detection

**Fecha**: 2026-03-11
**Estado**: Aceptado

**Contexto**: The new spec requires recipes to reference other recipes as sub-elaborations (e.g., a salsa used in multiple dishes).

**Decision**: Self-referential `sub_recipe_id UUID REFERENCES recipes(id)` in `recipe_ingredients` with XOR CHECK constraint (`ingredient_id IS NOT NULL OR sub_recipe_id IS NOT NULL`). Application-level recursion with max depth 10 and cycle detection via visited-set.

**Justification**: PostgreSQL recursive CTEs could solve this at the DB level, but application-level recursion provides:
- Better error messages (which cycle, which depth exceeded)
- Easier testing (unit tests without DB)
- Integration with JSONB allergen parsing from ingredients
- Consistent with Ports & Adapters pattern (domain logic in service, not SQL)

---

## 5. Flyway Migrations — Revised Order (V1-V15)

### V1-V9: Unchanged (already applied/stable)

| Version | File | Content |
|---|---|---|
| V1 | `V1__create_schema_extensions.sql` | pgcrypto, uuid-ossp |
| V2 | `V2__create_reference_tables.sql` | allergen, allergen_translation |
| V3 | `V3__create_user_tables.sql` | user_account, user_allergen_profile, consent_record, oauth_account |
| V4 | `V4__create_restaurant_tables.sql` | restaurant, subscription |
| V5 | `V5__create_menu_tables.sql` | menu, menu_section |
| V6 | `V6__create_dish_tables.sql` | dish, dish_allergen |
| V7 | `V7__create_audit_tables.sql` | allergen_audit_log |
| V8 | `V8__enable_rls.sql` | RLS on restaurant, menu, menu_section, dish, dish_allergen |
| V9 | `V9__seed_allergens.sql` | 14 EU allergens + translations |

### V10-V15: NEW (replace Wave 1 V10-V15)

| Version | File | Content |
|---|---|---|
| V10 | `V10__create_ingredient_table.sql` | `CREATE EXTENSION IF NOT EXISTS pg_trgm;` `CREATE TABLE ingredients (id UUID PK, tenant_id UUID NOT NULL, name VARCHAR(255), brand VARCHAR(255), supplier VARCHAR(255), allergens JSONB DEFAULT '[]', traces JSONB DEFAULT '[]', ocr_raw_text TEXT, notes TEXT, created_by UUID REFERENCES user_account(id), created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ);` RLS policy. GIN index on allergens. Trigram index on name. |
| V11 | `V11__create_recipe_tables.sql` | `CREATE TABLE recipes (id UUID PK, tenant_id UUID NOT NULL, restaurant_id UUID REFERENCES restaurant(id), name VARCHAR(255), description TEXT, category VARCHAR(100), is_sub_elaboration BOOLEAN DEFAULT false, price DECIMAL(10,2), image_url VARCHAR(500), created_by UUID REFERENCES user_account(id), is_active BOOLEAN DEFAULT true, created_at, updated_at);` `CREATE TABLE recipe_ingredients (id UUID PK, recipe_id UUID REFERENCES recipes(id), ingredient_id UUID REFERENCES ingredients(id), sub_recipe_id UUID REFERENCES recipes(id), tenant_id UUID, quantity DECIMAL, unit VARCHAR(30), notes VARCHAR(500), sort_order INT, CONSTRAINT check_ingredient_or_subrecipe CHECK ((ingredient_id IS NOT NULL AND sub_recipe_id IS NULL) OR (ingredient_id IS NULL AND sub_recipe_id IS NOT NULL)));` RLS on both tables. |
| V12 | `V12__alter_dish_add_recipe_id.sql` | `ALTER TABLE dish ADD COLUMN recipe_id UUID REFERENCES recipes(id) ON DELETE SET NULL;` Index. |
| V13 | `V13__create_menu_recipes_table.sql` | `CREATE TABLE menu_recipes (id UUID PK, menu_id UUID REFERENCES menu(id) ON DELETE CASCADE, recipe_id UUID REFERENCES recipes(id), section_name VARCHAR(255), sort_order INT DEFAULT 0, tenant_id UUID NOT NULL);` RLS policy. |
| V14 | `V14__alter_menu_add_digital_fields.sql` | `ALTER TABLE menu ADD COLUMN is_published BOOLEAN DEFAULT false; ALTER TABLE menu ADD COLUMN valid_from TIMESTAMPTZ; ALTER TABLE menu ADD COLUMN valid_to TIMESTAMPTZ; ALTER TABLE menu ADD COLUMN client_name VARCHAR(255); ALTER TABLE menu ADD COLUMN client_logo_url VARCHAR(500);` Drop old `published` column if different from `is_published`. |
| V15 | `V15__create_digital_cards_table.sql` | `CREATE TABLE digital_cards (id UUID PK, restaurant_id UUID REFERENCES restaurant(id), menu_id UUID REFERENCES menu(id), tenant_id UUID NOT NULL, slug VARCHAR(100) UNIQUE, qr_code_url VARCHAR(500), is_active BOOLEAN DEFAULT true, custom_css JSONB DEFAULT '{}', created_at, updated_at);` RLS policy. |

---

## 6. Diagrama de Dependencias entre Modulos (Updated)

```
                        ┌─────────────┐
                        │   config/   │
                        │ (Security,  │
                        │  JWT, RLS,  │
                        │  OpenAPI)   │
                        └──────┬──────┘
                               │
                               ▼
                        ┌─────────────┐
                        │   shared/   │◄─────── ALL modules depend on this
                        └──────┬──────┘
                               │
         ┌─────────────────────┼──────────────────────────┐
         │                     │                          │
         ▼                     ▼                          ▼
  ┌────────────┐       ┌──────────────┐           ┌────────────┐
  │   auth/    │       │  allergen/   │           │ consumer/  │
  └──────┬─────┘       └──────────────┘           └─────┬──────┘
         │                     │                        │
         ▼                     │ (enum codes)           │
  ┌──────────────┐             │                        │
  │  restaurant/ │             │                        │
  └──────┬───────┘             │                        │
         │                     │                        │
         ├─────────────────────┤                        │
         ▼                     ▼                        │
  ┌──────────────┐     ┌──────────────┐                 │
  │ ingredient/  │     │    menu/     │                 │
  │ (JSONB,      │     │ (sections,   │                 │
  │  tenant,     │     │  digital     │                 │
  │  P&A repos)  │     │  fields)     │                 │
  └──────┬───────┘     └──────┬───────┘                 │
         │                    │                         │
         ▼                    │                         │
  ┌──────────────┐            │                         │
  │   recipe/    │────────────┤                         │
  │ (sub-elab,   │            │                         │
  │  recursive   │            │                         │
  │  allergens,  │            ▼                         │
  │  P&A repos)  │     ┌──────────────┐                 │
  └──────┬───────┘     │    dish/     │◄────────────────┘
         │             │ (semaforo,   │
         │             │  recipe FK)  │
         │             └──────┬───────┘
         │                    │
         ▼                    ▼
  ┌──────────────┐     ┌──────────────┐
  │ digitalcard/ │     │   audit/     │
  │ (slug, QR,   │     │ (audit_log)  │
  │  P&A repos)  │     └──────────────┘
  └──────────────┘
                        ┌──────────────┐
                        │    gdpr/     │
                        │ (export,     │
                        │  delete)     │
                        └──────────────┘
```

---

## 7. New Entity Designs (ingredient, recipe, digitalcard)

### 7.1 Ingredient Entity (JSONB-based)

```kotlin
@Entity
@Table(name = "ingredients")
class Ingredient(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "brand", length = 255)
    var brand: String? = null,

    @Column(name = "supplier", length = 255)
    var supplier: String? = null,

    // JSONB: [{"code":"GLUTEN","level":"CONTAINS"}, ...]
    @Column(name = "allergens", columnDefinition = "jsonb", nullable = false)
    var allergens: String = "[]",  // or use a custom type/converter

    // JSONB: [{"code":"MILK","source":"shared equipment"}, ...]
    @Column(name = "traces", columnDefinition = "jsonb")
    var traces: String? = "[]",

    @Column(name = "ocr_raw_text", columnDefinition = "TEXT")
    var ocrRawText: String? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "created_by")
    val createdBy: UUID? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
```

### 7.2 Recipe Entity (with sub-elaboration support)

```kotlin
@Entity
@Table(name = "recipes")
class Recipe(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "restaurant_id", nullable = false)
    val restaurantId: UUID,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "category", length = 100)
    var category: String? = null,

    @Column(name = "is_sub_elaboration", nullable = false)
    var isSubElaboration: Boolean = false,

    @Column(name = "price", precision = 10, scale = 2)
    var price: BigDecimal? = null,

    @Column(name = "image_url", length = 500)
    var imageUrl: String? = null,

    @Column(name = "created_by")
    val createdBy: UUID? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @OneToMany(mappedBy = "recipe", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val ingredients: MutableList<RecipeIngredient> = mutableListOf()
)
```

### 7.3 RecipeIngredient Entity (with sub-recipe reference)

```kotlin
@Entity
@Table(name = "recipe_ingredients")
class RecipeIngredient(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    val recipe: Recipe,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id")
    val ingredient: Ingredient? = null,        // NULL if sub-recipe

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_recipe_id")
    val subRecipe: Recipe? = null,             // NULL if ingredient

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "quantity", precision = 10, scale = 3)
    var quantity: BigDecimal? = null,

    @Column(name = "unit", length = 30)
    var unit: String? = null,

    @Column(name = "notes", length = 500)
    var notes: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0
)
// DB CHECK constraint ensures exactly one of ingredient_id/sub_recipe_id is non-null
```

### 7.4 DigitalCard Entity

```kotlin
@Entity
@Table(name = "digital_cards")
class DigitalCard(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "restaurant_id", nullable = false)
    val restaurantId: UUID,

    @Column(name = "menu_id", nullable = false)
    val menuId: UUID,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "slug", length = 100, unique = true)
    var slug: String,

    @Column(name = "qr_code_url", length = 500)
    var qrCodeUrl: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "custom_css", columnDefinition = "jsonb")
    var customCss: String? = "{}",

    @Column(name = "created_at", updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
```

---

## 8. RecipeAllergenCalculator — Recursive Design

```
Algorithm: computeAllergensForRecipe(recipeId, visited = {}, depth = 0)

1. If depth > 10, throw MaxDepthExceededException
2. If recipeId in visited, throw CyclicRecipeException(cycle path)
3. Add recipeId to visited set
4. Fetch recipe_ingredients for recipeId
5. For each recipe_ingredient:
   a. If ingredient_id is not null:
      - Parse ingredient.allergens JSONB -> List<AllergenEntry>
      - Parse ingredient.traces JSONB -> List<TraceEntry>
      - Add to result: allergens as CONTAINS/MAY_CONTAIN, traces as MAY_CONTAIN
   b. If sub_recipe_id is not null:
      - RECURSE: computeAllergensForRecipe(sub_recipe_id, visited.copy(), depth + 1)
      - Merge results with priority: CONTAINS > MAY_CONTAIN > FREE_OF
6. Return aggregated allergen map: Map<AllergenCode, ContainmentLevel>
```

---

## 9. Complete File Inventory — Wave 2 Implementation Plan

### Files to DELETE (Wave 1 artifacts that are incompatible):

```
src/main/resources/db/migration/V10__create_ingredient_tables.sql
src/main/resources/db/migration/V11__create_recipe_tables.sql
src/main/resources/db/migration/V12__enable_rls_recipe_tables.sql
src/main/resources/db/migration/V13__alter_dish_add_recipe_id.sql
src/main/resources/db/migration/V14__alter_menu_add_published.sql
src/main/resources/db/migration/V15__seed_common_ingredients.sql
src/main/kotlin/.../ingredient/model/entity/Ingredient.kt
src/main/kotlin/.../ingredient/model/entity/IngredientAllergen.kt
src/main/kotlin/.../ingredient/model/enum/IngredientCategory.kt
src/main/kotlin/.../ingredient/repository/IngredientRepository.kt
src/main/kotlin/.../ingredient/repository/IngredientAllergenRepository.kt
src/main/kotlin/.../ingredient/service/IngredientService.kt
src/main/kotlin/.../ingredient/service/impl/IngredientServiceImpl.kt
src/main/kotlin/.../ingredient/dto/request/CreateIngredientRequest.kt
src/main/kotlin/.../ingredient/dto/request/UpdateIngredientRequest.kt
src/main/kotlin/.../ingredient/dto/request/AllergenMappingRequest.kt
src/main/kotlin/.../ingredient/dto/response/IngredientResponse.kt
src/main/kotlin/.../ingredient/mapper/IngredientMapper.kt
src/main/kotlin/.../ingredient/controller/IngredientController.kt
src/main/kotlin/.../ingredient/exception/IngredientNotFoundException.kt
src/main/kotlin/.../recipe/model/entity/Recipe.kt
src/main/kotlin/.../recipe/model/entity/RecipeIngredient.kt
src/main/kotlin/.../recipe/repository/RecipeRepository.kt
src/main/kotlin/.../recipe/repository/RecipeIngredientRepository.kt
src/main/kotlin/.../recipe/service/RecipeService.kt
src/main/kotlin/.../recipe/service/impl/RecipeServiceImpl.kt
src/main/kotlin/.../recipe/service/RecipeAllergenComputeService.kt
src/main/kotlin/.../recipe/service/impl/RecipeAllergenComputeServiceImpl.kt
src/main/kotlin/.../recipe/dto/request/CreateRecipeRequest.kt
src/main/kotlin/.../recipe/dto/request/UpdateRecipeRequest.kt
src/main/kotlin/.../recipe/dto/request/RecipeIngredientRequest.kt
src/main/kotlin/.../recipe/dto/response/RecipeResponse.kt
src/main/kotlin/.../recipe/dto/response/RecipeSummaryResponse.kt
src/main/kotlin/.../recipe/dto/response/RecipeIngredientResponse.kt
src/main/kotlin/.../recipe/mapper/RecipeMapper.kt
src/main/kotlin/.../recipe/controller/AdminRecipeController.kt
src/main/kotlin/.../recipe/exception/RecipeNotFoundException.kt
```

### Files to MODIFY (existing code with minor changes):

```
src/main/kotlin/.../dish/model/entity/Dish.kt           — update Recipe import path
src/main/kotlin/.../menu/model/entity/Menu.kt            — rename published->isPublished, add new fields
src/main/kotlin/.../dish/repository/DishRepository.kt    — update fetch join query for new Recipe entity
src/main/kotlin/.../menu/repository/MenuRepository.kt    — update query for isPublished field name
```

### Files to CREATE (new in Wave 2):

```
# Migrations
src/main/resources/db/migration/V10__create_ingredient_table.sql
src/main/resources/db/migration/V11__create_recipe_tables.sql
src/main/resources/db/migration/V12__alter_dish_add_recipe_id.sql
src/main/resources/db/migration/V13__create_menu_recipes_table.sql
src/main/resources/db/migration/V14__alter_menu_add_digital_fields.sql
src/main/resources/db/migration/V15__create_digital_cards_table.sql

# Ingredient module (Ports & Adapters)
src/main/kotlin/.../ingredient/model/entity/Ingredient.kt
src/main/kotlin/.../ingredient/repository/IngredientRepository.kt              # PORT
src/main/kotlin/.../ingredient/repository/IngredientRepositoryAdapter.kt       # ADAPTER
src/main/kotlin/.../ingredient/repository/jpa/JpaIngredientRepository.kt       # JPA
src/main/kotlin/.../ingredient/service/IngredientService.kt
src/main/kotlin/.../ingredient/service/impl/IngredientServiceImpl.kt
src/main/kotlin/.../ingredient/service/TextAnalyzerService.kt
src/main/kotlin/.../ingredient/service/impl/TextAnalyzerServiceImpl.kt
src/main/kotlin/.../ingredient/dto/request/CreateIngredientRequest.kt
src/main/kotlin/.../ingredient/dto/request/UpdateIngredientRequest.kt
src/main/kotlin/.../ingredient/dto/response/IngredientResponse.kt
src/main/kotlin/.../ingredient/mapper/IngredientMapper.kt
src/main/kotlin/.../ingredient/controller/AdminIngredientController.kt
src/main/kotlin/.../ingredient/exception/IngredientNotFoundException.kt

# Recipe module (Ports & Adapters)
src/main/kotlin/.../recipe/model/entity/Recipe.kt
src/main/kotlin/.../recipe/model/entity/RecipeIngredient.kt
src/main/kotlin/.../recipe/repository/RecipeRepository.kt                      # PORT
src/main/kotlin/.../recipe/repository/RecipeRepositoryAdapter.kt               # ADAPTER
src/main/kotlin/.../recipe/repository/jpa/JpaRecipeRepository.kt               # JPA
src/main/kotlin/.../recipe/repository/RecipeIngredientRepository.kt            # PORT
src/main/kotlin/.../recipe/repository/RecipeIngredientRepositoryAdapter.kt     # ADAPTER
src/main/kotlin/.../recipe/repository/jpa/JpaRecipeIngredientRepository.kt     # JPA
src/main/kotlin/.../recipe/service/RecipeService.kt
src/main/kotlin/.../recipe/service/impl/RecipeServiceImpl.kt
src/main/kotlin/.../recipe/service/RecipeAllergenCalculator.kt                 # Interface
src/main/kotlin/.../recipe/service/impl/RecipeAllergenCalculatorImpl.kt        # Recursive
src/main/kotlin/.../recipe/dto/request/CreateRecipeRequest.kt
src/main/kotlin/.../recipe/dto/request/UpdateRecipeRequest.kt
src/main/kotlin/.../recipe/dto/request/RecipeIngredientRequest.kt
src/main/kotlin/.../recipe/dto/response/RecipeResponse.kt
src/main/kotlin/.../recipe/dto/response/RecipeSummaryResponse.kt
src/main/kotlin/.../recipe/dto/response/RecipeIngredientResponse.kt
src/main/kotlin/.../recipe/mapper/RecipeMapper.kt
src/main/kotlin/.../recipe/controller/AdminRecipeController.kt
src/main/kotlin/.../recipe/exception/RecipeNotFoundException.kt
src/main/kotlin/.../recipe/exception/CyclicRecipeException.kt
src/main/kotlin/.../recipe/exception/MaxDepthExceededException.kt

# DigitalCard module (Ports & Adapters)
src/main/kotlin/.../digitalcard/model/entity/DigitalCard.kt
src/main/kotlin/.../digitalcard/repository/DigitalCardRepository.kt            # PORT
src/main/kotlin/.../digitalcard/repository/DigitalCardRepositoryAdapter.kt     # ADAPTER
src/main/kotlin/.../digitalcard/repository/jpa/JpaDigitalCardRepository.kt     # JPA
src/main/kotlin/.../digitalcard/service/DigitalCardService.kt
src/main/kotlin/.../digitalcard/service/impl/DigitalCardServiceImpl.kt
src/main/kotlin/.../digitalcard/dto/request/CreateDigitalCardRequest.kt
src/main/kotlin/.../digitalcard/dto/response/DigitalCardResponse.kt
src/main/kotlin/.../digitalcard/mapper/DigitalCardMapper.kt
src/main/kotlin/.../digitalcard/controller/AdminDigitalCardController.kt
src/main/kotlin/.../digitalcard/controller/PublicDigitalCardController.kt

# MenuRecipe pivot (in menu module)
src/main/kotlin/.../menu/model/entity/MenuRecipe.kt
src/main/kotlin/.../menu/repository/MenuRecipeRepository.kt                    # Direct JPA (existing pattern)

# AllergenType enum enhancement (in allergen module)
src/main/kotlin/.../allergen/model/enum/AllergenType.kt                        # Enhanced enum
```

---

## 10. Blockers and Open Questions

1. **JSONB type mapping in JPA**: The `allergens JSONB` column needs either a Hibernate `@Type` annotation (with `io.hypersistence:hypersistence-utils-hibernate-63` or equivalent for Hibernate 7) or a manual `AttributeConverter<List<AllergenEntry>, String>`. Recommend the `AttributeConverter` approach to avoid adding another dependency.

2. **Menu.published vs Menu.isPublished**: Wave 1 added `published` via V14. The new spec uses `is_published`. Since V10-V15 are being deleted and rewritten, this is a non-issue — the new V14 will use `is_published` directly.

3. **`spring-boot-starter-flyway` artifact**: The current build.gradle.kts uses `spring-boot-starter-flyway`. Verify this exists in Boot 4.0.3. The tech spec says `org.flywaydb:flyway-core` directly. Both approaches work but the starter may not exist; fallback to direct dependency.

4. **`tools.jackson.module` vs `com.fasterxml.jackson.module`**: Still incorrect in build.gradle.kts. Must be `com.fasterxml.jackson.module:jackson-module-kotlin` for Spring Boot 4.0.3.

5. **Testcontainers artifact IDs**: Current build uses `testcontainers-postgresql` and `testcontainers-junit-jupiter`. Official artifact IDs are `postgresql` and `junit-jupiter` under `org.testcontainers` group. Verify which is correct for the latest Testcontainers version.
