# Menus Backend — Technical Specification

> Referencia técnica completa para el equipo de agentes. Última actualización: 2026-03-10.
> Fuentes: Confluence AppToLast SD space (páginas bajo ID 190709761), build.gradle.kts, backups de alérgenos.

---

## 1. Descripción del Proyecto

**Menus Backend** es el API REST del sistema de gestión de alérgenos para restauración de AppToLast.

**Propósito**: Digitalizar la declaración obligatoria de los **14 alérgenos EU** (Reglamento UE 1169/2011) para restaurantes españoles. Los consumidores reciben filtrado de menú tipo semáforo (SAFE/RISK/DANGER) basado en su perfil de alergias.

**Modelo de negocio**: B2B (restaurantes pagan suscripción) + B2C (consumidores usan la app de forma gratuita).

**Scope de este repositorio**: SOLO backend + infraestructura. No hay frontend.

---

## 2. Stack Tecnológico

| Componente | Versión | Notas |
|---|---|---|
| Kotlin | 2.2 | JVM 21 Temurin |
| Spring Boot | 4.0.3 | Spring Framework 7.x |
| Spring Security | 7.x (incluido en Boot 4) | Stateless JWT |
| PostgreSQL | 16 | pgcrypto, RLS |
| Spring Data JPA / Hibernate | incluido en Boot 4 | FetchType.LAZY por defecto |
| Flyway | 11.x | Migraciones V1–V9 |
| jjwt | 0.12.6 | Access 15min + Refresh 7 días |
| google-api-client | 2.7.2 | Verificación ID token Google |
| springdoc-openapi | 3.0.1 | Swagger UI en `/swagger-ui/index.html` |
| Gradle | 8.10+ Kotlin DSL | `build.gradle.kts` |
| Docker | multi-stage | eclipse-temurin:21-jdk/jre-alpine |
| Kubernetes | Rancher + Traefik (NO nginx) | Longhorn PVC, cert-manager |
| GitHub Actions | — | CI/CD, ghcr.io registry |

---

## 3. Paquete Base y Refactoring

**Paquete objetivo**: `com.apptolast.menus`

**Estado actual** (a refactorizar por backend-dev-domain):
- Clase principal: `com.example.menubackend.MenuBackendApplication`
- Dependencias incorrectas en build.gradle.kts (ver sección 4)

---

## 4. build.gradle.kts — Estado Actual y Cambios Requeridos

### Dependencias a eliminar / corregir:
```kotlin
// ELIMINAR — artifact ID incorrecto para Spring Boot 4 / Jackson 3.x:
implementation("tools.jackson.module:jackson-module-kotlin")  // → com.fasterxml.jackson.module

// ELIMINAR — no existen:
testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
testImplementation("org.springframework.security:spring-security-test-utils")
```

### Dependencias a añadir:
```kotlin
// Flyway
implementation("org.flywaydb:flyway-core")
implementation("org.flywaydb:flyway-database-postgresql")

// JWT
implementation("io.jsonwebtoken:jjwt-api:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

// Google OAuth2
implementation("com.google.api-client:google-api-client:2.7.2")

// OpenAPI
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")

// Jackson Kotlin (correcto para Spring Boot 4)
implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

// Validation
implementation("org.springframework.boot:spring-boot-starter-validation")

// Actuator
implementation("org.springframework.boot:spring-boot-starter-actuator")
```

### Tests a añadir:
```kotlin
testImplementation("org.springframework.boot:spring-boot-testcontainers")
testImplementation("org.testcontainers:postgresql")
testImplementation("org.testcontainers:junit-jupiter")
testImplementation("org.springframework.security:spring-security-test")
```

### Plugin kotlin-allopen (necesario para JPA):
```kotlin
plugins {
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    kotlin("plugin.allopen") version "2.2.21"
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
```

---

## 5. Estructura de Módulos

```
src/main/kotlin/com/apptolast/menus/
├── MenusBackendApplication.kt        # Main class
├── config/
│   ├── SecurityConfig.kt             # Spring Security, filtros JWT, CORS
│   ├── JwtConfig.kt                  # Propiedades JWT (secreto, expiración)
│   ├── OpenApiConfig.kt              # Swagger/OpenAPI config
│   └── TenantConfig.kt               # RLS tenant filter (ThreadLocal)
├── shared/
│   ├── dto/
│   │   ├── ErrorResponse.kt
│   │   └── PageResponse.kt
│   ├── exception/
│   │   ├── BusinessException.kt      # Base exception
│   │   ├── ResourceNotFoundException.kt
│   │   ├── ConflictException.kt
│   │   ├── ForbiddenException.kt
│   │   └── ConsentRequiredException.kt
│   ├── security/
│   │   ├── JwtTokenProvider.kt
│   │   ├── JwtAuthenticationFilter.kt
│   │   └── UserPrincipal.kt
│   └── handler/
│       └── GlobalExceptionHandler.kt
├── auth/
│   ├── controller/
│   │   └── AuthController.kt
│   ├── dto/
│   │   ├── request/
│   │   │   ├── RegisterRequest.kt
│   │   │   ├── LoginRequest.kt
│   │   │   ├── RefreshTokenRequest.kt
│   │   │   └── GoogleCallbackRequest.kt
│   │   └── response/
│   │       └── AuthResponse.kt
│   ├── service/
│   │   ├── AuthService.kt            # Interface
│   │   ├── impl/
│   │   │   ├── AuthServiceImpl.kt
│   │   │   └── OAuth2ServiceImpl.kt
│   │   └── ConsentService.kt
│   └── model/
│       └── entity/
│           └── (usa UserAccount de consumer/)
├── allergen/
│   ├── controller/
│   │   └── AllergenController.kt     # GET /api/v1/allergens
│   ├── dto/response/
│   │   └── AllergenResponse.kt
│   ├── model/
│   │   ├── entity/
│   │   │   ├── Allergen.kt
│   │   │   └── AllergenTranslation.kt
│   │   └── enum/
│   │       └── AllergenCode.kt       # GLUTEN, CRUSTACEANS, EGGS, FISH, PEANUTS,
│   │                                 # SOYBEANS, MILK, NUTS, CELERY, MUSTARD,
│   │                                 # SESAME, SULPHITES, LUPIN, MOLLUSCS
│   ├── repository/
│   │   ├── AllergenRepository.kt
│   │   └── AllergenTranslationRepository.kt
│   └── service/
│       ├── AllergenService.kt
│       └── impl/AllergenServiceImpl.kt
├── restaurant/
│   ├── controller/
│   │   ├── RestaurantController.kt   # Consumer: GET /restaurants, GET /restaurants/{id}
│   │   └── AdminRestaurantController.kt # Admin: GET/PUT /admin/restaurant
│   ├── dto/
│   │   ├── request/
│   │   │   └── RestaurantRequest.kt
│   │   └── response/
│   │       └── RestaurantResponse.kt
│   ├── model/
│   │   ├── entity/
│   │   │   ├── Restaurant.kt
│   │   │   └── Subscription.kt
│   │   └── enum/
│   │       └── SubscriptionTier.kt   # BASIC, PROFESSIONAL, PREMIUM
│   ├── repository/
│   │   └── RestaurantRepository.kt
│   └── service/
│       ├── RestaurantService.kt
│       └── impl/RestaurantServiceImpl.kt
├── menu/
│   ├── controller/
│   │   ├── MenuController.kt         # Consumer: GET /restaurants/{id}/menu
│   │   └── AdminMenuController.kt    # Admin: CRUD menus + sections
│   ├── dto/
│   │   ├── request/
│   │   │   ├── MenuRequest.kt
│   │   │   └── SectionRequest.kt
│   │   └── response/
│   │       ├── MenuResponse.kt
│   │       └── SectionResponse.kt
│   ├── model/
│   │   ├── entity/
│   │   │   ├── Menu.kt               # is_archived soft delete
│   │   │   └── MenuSection.kt        # display_order
│   ├── repository/
│   │   ├── MenuRepository.kt
│   │   └── MenuSectionRepository.kt
│   └── service/
│       ├── MenuService.kt
│       └── impl/MenuServiceImpl.kt
├── dish/
│   ├── controller/
│   │   ├── DishController.kt         # Consumer: semáforo filtrado
│   │   └── AdminDishController.kt    # Admin: CRUD dishes
│   ├── dto/
│   │   ├── request/
│   │   │   └── DishRequest.kt
│   │   └── response/
│   │       ├── DishResponse.kt
│   │       └── DishAllergenResponse.kt
│   ├── model/
│   │   ├── entity/
│   │   │   ├── Dish.kt
│   │   │   └── DishAllergen.kt       # CRÍTICA: dish + allergen + ContainmentLevel
│   │   └── enum/
│   │       └── ContainmentLevel.kt   # CONTAINS, MAY_CONTAIN, FREE_OF
│   ├── repository/
│   │   ├── DishRepository.kt
│   │   └── DishAllergenRepository.kt
│   └── service/
│       ├── DishService.kt
│       ├── AllergenFilterService.kt  # Semáforo: SAFE/RISK/DANGER
│       └── impl/
│           ├── DishServiceImpl.kt
│           └── AllergenFilterServiceImpl.kt
├── consumer/
│   ├── controller/
│   │   └── UserController.kt         # GET/PUT /users/me/allergen-profile
│   ├── dto/
│   │   ├── request/
│   │   │   └── AllergenProfileRequest.kt
│   │   └── response/
│   │       └── AllergenProfileResponse.kt
│   ├── model/
│   │   ├── entity/
│   │   │   ├── UserAccount.kt        # Datos personales (email cifrado AES-256)
│   │   │   ├── UserAllergenProfile.kt # Datos salud SEPARADOS (link por profile_uuid)
│   │   │   ├── ConsentRecord.kt
│   │   │   └── OAuthAccount.kt
│   │   └── enum/
│   │       └── UserRole.kt           # CONSUMER, RESTAURANT_OWNER, KITCHEN_STAFF, ADMIN
│   ├── repository/
│   │   ├── UserAccountRepository.kt
│   │   ├── UserAllergenProfileRepository.kt
│   │   ├── ConsentRecordRepository.kt
│   │   └── OAuthAccountRepository.kt
│   └── service/
│       ├── UserAllergenProfileService.kt
│       └── impl/UserAllergenProfileServiceImpl.kt
├── gdpr/
│   ├── controller/
│   │   └── GdprController.kt         # /users/me/data-export, data-delete, data-rectification
│   ├── dto/
│   │   ├── request/
│   │   │   └── RectificationRequest.kt
│   │   └── response/
│   │       └── DataExportResponse.kt
│   ├── service/
│   │   ├── GdprService.kt
│   │   └── impl/GdprServiceImpl.kt
└── audit/
    ├── model/
    │   └── entity/
    │       └── AllergenAuditLog.kt
    ├── repository/
    │   └── AllergenAuditLogRepository.kt
    └── service/
        ├── AuditService.kt
        └── impl/AuditServiceImpl.kt
```

---

## 6. Esquema de Base de Datos

### 6.1 Migraciones Flyway (V1–V9)

| Versión | Archivo | Contenido |
|---|---|---|
| V1 | `V1__create_schema_extensions.sql` | CREATE EXTENSION pgcrypto; CREATE EXTENSION "uuid-ossp"; |
| V2 | `V2__create_reference_tables.sql` | allergen, allergen_translation |
| V3 | `V3__create_user_tables.sql` | user_account, user_allergen_profile, consent_record, oauth_account |
| V4 | `V4__create_restaurant_tables.sql` | restaurant, subscription |
| V5 | `V5__create_menu_tables.sql` | menu, menu_section |
| V6 | `V6__create_dish_tables.sql` | dish, dish_allergen |
| V7 | `V7__create_audit_tables.sql` | allergen_audit_log |
| V8 | `V8__enable_rls.sql` | Row-Level Security policies en restaurant, menu, menu_section, dish, dish_allergen |
| V9 | `V9__seed_allergens.sql` | INSERT 14 alérgenos + traducciones ES/EN/CA/EU/GL |

### 6.2 Tablas Detalladas

#### allergen
```sql
CREATE TABLE allergen (
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(20) UNIQUE NOT NULL,  -- AllergenCode enum
    icon_url    VARCHAR(500),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
```

#### allergen_translation
```sql
CREATE TABLE allergen_translation (
    id          SERIAL PRIMARY KEY,
    allergen_id INTEGER REFERENCES allergen(id),
    locale      VARCHAR(5) NOT NULL,  -- 'es', 'en', 'ca', 'eu', 'gl'
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    UNIQUE (allergen_id, locale)
);
```

#### user_account
```sql
CREATE TABLE user_account (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           BYTEA NOT NULL UNIQUE,   -- AES-256 cifrado con pgcrypto
    email_hash      VARCHAR(64) NOT NULL UNIQUE, -- SHA-256 para búsquedas
    password_hash   VARCHAR(255),            -- BCrypt, nullable para OAuth-only
    profile_uuid    UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(), -- link RGPD
    role            VARCHAR(30) NOT NULL DEFAULT 'CONSUMER',
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);
```

#### user_allergen_profile
```sql
CREATE TABLE user_allergen_profile (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_uuid    UUID UNIQUE NOT NULL,    -- FK lógica a user_account.profile_uuid
    allergen_codes  TEXT[] NOT NULL DEFAULT '{}', -- Array de AllergenCode
    severity_notes  TEXT,                    -- Texto libre (cifrado)
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);
-- NO hay FK explícita a user_account — separación RGPD
```

#### consent_record
```sql
CREATE TABLE consent_record (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_uuid    UUID NOT NULL,
    consent_type    VARCHAR(50) NOT NULL,    -- 'HEALTH_DATA_PROCESSING'
    granted         BOOLEAN NOT NULL,
    ip_address      INET,
    user_agent      TEXT,
    granted_at      TIMESTAMPTZ DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ
);
```

#### oauth_account
```sql
CREATE TABLE oauth_account (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES user_account(id),
    provider        VARCHAR(20) NOT NULL,    -- 'GOOGLE'
    provider_id     VARCHAR(255) NOT NULL,
    email           BYTEA NOT NULL,          -- cifrado
    UNIQUE (provider, provider_id)
);
```

#### restaurant
```sql
CREATE TABLE restaurant (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID UNIQUE NOT NULL,    -- = id (restaurant es su propio tenant)
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
```

#### subscription
```sql
CREATE TABLE subscription (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    restaurant_id   UUID UNIQUE NOT NULL REFERENCES restaurant(id),
    tier            VARCHAR(20) NOT NULL DEFAULT 'BASIC',  -- BASIC/PROFESSIONAL/PREMIUM
    max_menus       INTEGER NOT NULL DEFAULT 1,
    max_dishes      INTEGER NOT NULL DEFAULT 50,
    starts_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,
    is_active       BOOLEAN DEFAULT TRUE
);
```

#### menu
```sql
CREATE TABLE menu (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    restaurant_id   UUID NOT NULL REFERENCES restaurant(id),
    tenant_id       UUID NOT NULL,           -- RLS
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    is_archived     BOOLEAN DEFAULT FALSE,   -- soft delete
    display_order   INTEGER DEFAULT 0,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);
```

#### menu_section
```sql
CREATE TABLE menu_section (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    menu_id         UUID NOT NULL REFERENCES menu(id),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    display_order   INTEGER DEFAULT 0
);
```

#### dish
```sql
CREATE TABLE dish (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    section_id      UUID NOT NULL REFERENCES menu_section(id),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    price           DECIMAL(10,2),
    image_url       VARCHAR(500),
    is_available    BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);
```

#### dish_allergen (TABLA CRÍTICA)
```sql
CREATE TABLE dish_allergen (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dish_id         UUID NOT NULL REFERENCES dish(id),
    allergen_id     INTEGER NOT NULL REFERENCES allergen(id),
    tenant_id       UUID NOT NULL,
    containment_level VARCHAR(20) NOT NULL,  -- CONTAINS, MAY_CONTAIN, FREE_OF
    notes           TEXT,
    UNIQUE (dish_id, allergen_id)
);
```

#### allergen_audit_log
```sql
CREATE TABLE allergen_audit_log (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dish_id         UUID NOT NULL,
    allergen_id     INTEGER NOT NULL,
    tenant_id       UUID NOT NULL,
    changed_by_uuid UUID NOT NULL,           -- profile_uuid, nunca user id
    action          VARCHAR(20) NOT NULL,    -- 'ADD', 'REMOVE', 'UPDATE'
    old_level       VARCHAR(20),
    new_level       VARCHAR(20),
    changed_at      TIMESTAMPTZ DEFAULT NOW()
);
```

### 6.3 RLS Policies (V8)

```sql
-- Habilitar RLS
ALTER TABLE restaurant ENABLE ROW LEVEL SECURITY;
ALTER TABLE menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE menu_section ENABLE ROW LEVEL SECURITY;
ALTER TABLE dish ENABLE ROW LEVEL SECURITY;
ALTER TABLE dish_allergen ENABLE ROW LEVEL SECURITY;

-- Policy ejemplo (repetir para cada tabla):
CREATE POLICY tenant_isolation ON restaurant
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

-- Seed data (allergen, allergen_translation) NO tienen RLS — son tablas de referencia
-- user_account, user_allergen_profile NO tienen RLS — acceso por user_id en servicio
```

### 6.4 Seed Alérgenos (V9)

Los 14 alérgenos del Reglamento UE 1169/2011:

| Code | ES | EN |
|---|---|---|
| GLUTEN | Gluten | Gluten |
| CRUSTACEANS | Crustáceos | Crustaceans |
| EGGS | Huevos | Eggs |
| FISH | Pescado | Fish |
| PEANUTS | Cacahuetes | Peanuts |
| SOYBEANS | Soja | Soybeans |
| MILK | Leche | Milk |
| NUTS | Frutos de cáscara | Nuts |
| CELERY | Apio | Celery |
| MUSTARD | Mostaza | Mustard |
| SESAME | Sésamo | Sesame |
| SULPHITES | Dióxido de azufre y sulfitos | Sulphur dioxide and sulphites |
| LUPIN | Altramuces | Lupin |
| MOLLUSCS | Moluscos | Molluscs |

---

## 7. API REST — Especificación Completa

### 7.1 Convenciones Generales

- Base URL: `/api/v1`
- Content-Type: `application/json`
- Autenticación: `Authorization: Bearer <JWT>`
- Paginación: `?page=0&size=20&sort=createdAt,desc`
- Versión de API en la URL (no en headers)

### 7.2 Formato de Error Estándar (SIEMPRE)

```json
{
  "error": {
    "code": "ALLERGEN_PROFILE_CONSENT_REQUIRED",
    "message": "You must provide explicit consent to process health data.",
    "status": 403,
    "timestamp": "2026-03-10T10:30:00Z",
    "path": "/api/v1/users/me/allergen-profile"
  }
}
```

### 7.3 Auth Endpoints (`/api/v1/auth`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/register` | Public | Registro email+password |
| POST | `/login` | Public | Login, devuelve access+refresh token |
| POST | `/refresh` | Public | Renovar access token con refresh token |
| GET | `/oauth2/google` | Public | Iniciar flujo OAuth2 Google |
| POST | `/oauth2/google/callback` | Public | Callback Google, devuelve JWT |
| POST | `/consent` | JWT | Dar consentimiento RGPD datos de salud |
| DELETE | `/consent` | JWT | Revocar consentimiento (borra perfil alérgenos) |

#### POST /register
```json
// Request:
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "acceptTerms": true
}
// Response 201:
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
// Errors: 400 VALIDATION_ERROR, 409 EMAIL_ALREADY_EXISTS
```

#### POST /login
```json
// Request:
{ "email": "user@example.com", "password": "SecurePass123!" }
// Response 200: same as register
// Errors: 401 INVALID_CREDENTIALS, 403 ACCOUNT_DISABLED
```

#### POST /oauth2/google/callback
```json
// Request:
{ "idToken": "Google ID token from frontend" }
// Response 200: same as login (crear cuenta si no existe)
// Errors: 401 INVALID_GOOGLE_TOKEN
```

### 7.4 Consumer Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/users/me/allergen-profile` | JWT + Consent | Ver perfil de alérgenos |
| PUT | `/users/me/allergen-profile` | JWT + Consent | Actualizar perfil |
| GET | `/restaurants` | Public | Buscar restaurantes (query params: name, city) |
| GET | `/restaurants/{id}` | Public | Detalle restaurante |
| GET | `/restaurants/{id}/menu` | JWT (opt) | Menú con filtrado semáforo si hay JWT+Consent |

#### GET /restaurants/{id}/menu — Respuesta con semáforo
```json
{
  "restaurantId": "uuid",
  "restaurantName": "Cobre y Picón",
  "sections": [
    {
      "sectionId": "uuid",
      "name": "Entrantes",
      "dishes": [
        {
          "dishId": "uuid",
          "name": "Ensalada César",
          "price": 8.50,
          "safetyLevel": "RISK",         // SAFE | RISK | DANGER | null (si no hay perfil)
          "matchedAllergens": ["EGGS"],  // Alérgenos del perfil que matchan
          "allergens": [
            {
              "code": "EGGS",
              "name": "Huevos",
              "containmentLevel": "MAY_CONTAIN"
            }
          ]
        }
      ]
    }
  ]
}
```

### 7.5 Admin Endpoints (`/api/v1/admin`) — RESTAURANT_OWNER

| Method | Path | Description |
|---|---|---|
| GET | `/restaurant` | Datos del restaurante propio |
| PUT | `/restaurant` | Actualizar datos del restaurante |
| GET | `/menus` | Listar menús (incluye archivados si ?archived=true) |
| POST | `/menus` | Crear menú |
| PUT | `/menus/{id}` | Actualizar menú |
| DELETE | `/menus/{id}` | Archivar menú (soft delete, is_archived=true) |
| POST | `/menus/{menuId}/sections` | Crear sección en menú |
| PUT | `/menus/{menuId}/sections/{id}` | Actualizar sección |
| DELETE | `/menus/{menuId}/sections/{id}` | Eliminar sección |
| GET | `/dishes` | Listar platos del restaurante |
| POST | `/dishes` | Crear plato con alérgenos |
| PUT | `/dishes/{id}` | Actualizar plato (registra en audit_log) |
| DELETE | `/dishes/{id}` | Eliminar plato |
| POST | `/dishes/{id}/allergens` | Añadir/actualizar alérgeno a plato |
| DELETE | `/dishes/{id}/allergens/{allergenId}` | Quitar alérgeno de plato |
| GET | `/subscription` | Suscripción actual |
| GET | `/analytics` | Estadísticas del restaurante |
| POST | `/qr/generate` | Generar QR del menú |

#### POST /admin/dishes — Request
```json
{
  "sectionId": "uuid",
  "name": "Paella Valenciana",
  "description": "...",
  "price": 18.00,
  "allergens": [
    { "allergenCode": "CRUSTACEANS", "containmentLevel": "CONTAINS" },
    { "allergenCode": "FISH", "containmentLevel": "CONTAINS" },
    { "allergenCode": "GLUTEN", "containmentLevel": "MAY_CONTAIN" }
  ]
}
```

### 7.6 RGPD Endpoints (`/api/v1/users/me`)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/data-export` | JWT | Exportar todos los datos del usuario (JSON) |
| DELETE | `/data-delete` | JWT | Eliminar cuenta (soft delete + anonimización) |
| PUT | `/data-rectification` | JWT | Rectificar datos personales |

#### DELETE /users/me/data-delete
- Email → reemplazado por hash irreversible
- password_hash → null
- user_allergen_profile → eliminado físicamente
- consent_record → marcado revoked_at=now()
- oauth_account → eliminado
- Restaurant y platos → se mantienen (pertenecen al negocio, no al usuario)
- Devuelve 204 No Content

### 7.7 Allergen Endpoints (público)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/allergens` | Public | Lista 14 alérgenos con traducciones |
| GET | `/allergens/{code}` | Public | Detalle alérgeno |

---

## 8. Seguridad

### 8.1 JWT

- **Access token**: 15 minutos, firmado con HS512, claims: `sub` (userId), `role`, `tenantId`
- **Refresh token**: 7 días, firmado con mismo secret, claims: `sub` (userId), `type: "refresh"`
- **Secret**: mínimo 64 bytes, desde variable de entorno `JWT_SECRET`
- **Filter**: `JwtAuthenticationFilter extends OncePerRequestFilter`
  - Extrae token de `Authorization: Bearer <token>`
  - Valida y carga `UserPrincipal` en `SecurityContextHolder`
  - Establece `SET app.current_tenant = <tenantId>` si hay tenantId en claims

### 8.2 Spring Security Config

```kotlin
@Configuration
class SecurityConfig(private val jwtFilter: JwtAuthenticationFilter) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers("/api/v1/auth/**").permitAll()
            it.requestMatchers(GET, "/api/v1/allergens/**").permitAll()
            it.requestMatchers(GET, "/api/v1/restaurants/**").permitAll()
            it.requestMatchers("/api/v1/admin/**").hasRole("RESTAURANT_OWNER")
            it.requestMatchers("/api/v1/users/me/**").authenticated()
            it.anyRequest().authenticated()
        }
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        .build()
}
```

### 8.3 RBAC

| Role | Acceso |
|---|---|
| `CONSUMER` | `/api/v1/users/me/**`, menús públicos |
| `RESTAURANT_OWNER` | `/api/v1/admin/**` (solo su propio restaurante) |
| `KITCHEN_STAFF` | Solo GET en `/api/v1/admin/dishes/**` |
| `ADMIN` | Endpoints de administración de plataforma (futuro) |

### 8.4 Multi-tenancy con RLS

```kotlin
// TenantConfig.kt — Filter que establece el tenant en cada request
@Component
class TenantFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, ...) {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? UserPrincipal
        val tenantId = principal?.tenantId
        if (tenantId != null) {
            entityManager.createNativeQuery("SET app.current_tenant = '$tenantId'").executeUpdate()
        }
        filterChain.doFilter(request, response)
    }
}
```

### 8.5 RGPD / GDPR

**Pseudonimización**:
- `user_account.profile_uuid` → UUID aleatorio, no predecible
- `user_allergen_profile.profile_uuid` → mismo UUID (link lógico, sin FK)
- Logs de auditoría usan `profile_uuid`, nunca `user_id`

**Cifrado de datos personales**:
- `user_account.email` → cifrado AES-256 con pgcrypto: `pgp_sym_encrypt(email, key)`
- `user_account.email_hash` → SHA-256 para búsquedas: `digest(email, 'sha256')`
- `user_allergen_profile.severity_notes` → cifrado si contiene diagnósticos

**Consentimiento**:
- Acceso a `user_allergen_profile` requiere `consent_record` activo de tipo `HEALTH_DATA_PROCESSING`
- `ConsentService.requireConsent(userId)` → lanza `ConsentRequiredException` si no hay consentimiento
- Revocar consentimiento borra físicamente `user_allergen_profile`

---

## 9. Lógica de Negocio — AllergenFilterService

```kotlin
interface AllergenFilterService {
    fun calculateSafetyLevel(
        dishAllergens: List<DishAllergen>,
        userProfile: UserAllergenProfile?
    ): SafetyLevel
}

enum class SafetyLevel { SAFE, RISK, DANGER }

// Implementación:
class AllergenFilterServiceImpl : AllergenFilterService {
    override fun calculateSafetyLevel(
        dishAllergens: List<DishAllergen>,
        userProfile: UserAllergenProfile?
    ): SafetyLevel {
        if (userProfile == null || userProfile.allergenCodes.isEmpty()) return SafetyLevel.SAFE

        val userCodes = userProfile.allergenCodes.toSet()
        val dishMap = dishAllergens.associateBy { it.allergen.code }

        for (code in userCodes) {
            when (dishMap[code]?.containmentLevel) {
                ContainmentLevel.CONTAINS -> return SafetyLevel.DANGER   // Prioridad máxima
                ContainmentLevel.MAY_CONTAIN -> { /* continuar buscando CONTAINS */ }
                else -> {}
            }
        }

        // Si llegamos aquí, no hay CONTAINS — buscar MAY_CONTAIN
        for (code in userCodes) {
            if (dishMap[code]?.containmentLevel == ContainmentLevel.MAY_CONTAIN) {
                return SafetyLevel.RISK
            }
        }

        return SafetyLevel.SAFE
    }
}
```

**Casos de test obligatorios**:
1. Perfil vacío → SAFE
2. Alérgeno del perfil con CONTAINS → DANGER
3. Alérgeno del perfil con MAY_CONTAIN → RISK
4. Alérgeno del perfil con FREE_OF → SAFE
5. Múltiples alérgenos: uno CONTAINS + uno MAY_CONTAIN → DANGER (prioridad)
6. Perfil null → SAFE

---

## 10. Configuración — application.yml

```yaml
spring:
  application:
    name: menus-backend
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/menusdb}
    username: ${DATABASE_USERNAME:menus}
    password: ${DATABASE_PASSWORD:menus}
    hikari:
      maximum-pool-size: 10
      connection-timeout: 30000
  jpa:
    hibernate:
      ddl-auto: validate          # Flyway gestiona el schema
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        default_schema: public
        jdbc:
          batch_size: 20
    open-in-view: false           # OBLIGATORIO desactivar
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}

app:
  jwt:
    secret: ${JWT_SECRET}         # Mínimo 64 bytes base64
    access-expiration: 900        # 15 minutos en segundos
    refresh-expiration: 604800    # 7 días en segundos
  encryption:
    key: ${ENCRYPTION_KEY}        # AES-256 para pgcrypto
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:3000,https://app.apptolast.com}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized

server:
  port: ${PORT:8080}
  servlet:
    context-path: /
  compression:
    enabled: true
```

---

## 11. Infraestructura

### 11.1 Docker — Dockerfile multi-stage

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon
COPY src/ src/
RUN ./gradlew build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${SPRING_PROFILE:prod}", "app.jar"]
```

### 11.2 Docker Compose (local dev)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: menusdb
      POSTGRES_USER: menus
      POSTGRES_PASSWORD: menus
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U menus -d menusdb"]
      interval: 10s
      timeout: 5s
      retries: 5

  api:
    build: .
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/menusdb
      DATABASE_USERNAME: menus
      DATABASE_PASSWORD: menus
      JWT_SECRET: ${JWT_SECRET}
      ENCRYPTION_KEY: ${ENCRYPTION_KEY}
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
```

### 11.3 Kubernetes (Rancher/Traefik)

**Namespaces**: `apptolast-menus-dev`, `apptolast-menus-prod`

**Manifests** (en `k8s/`):
- `00-namespace.yaml` — Namespaces dev y prod
- `01-postgres-pvc.yaml` — PVC Longhorn 5GB
- `02-postgres-deployment.yaml` — StatefulSet PostgreSQL 16
- `03-postgres-service.yaml` — ClusterIP service
- `04-api-deployment.yaml` — Deployment con resource limits
- `05-api-service.yaml` — ClusterIP service
- `06-api-ingress.yaml` — Traefik IngressRoute con cert-manager TLS
- `07-secrets.yaml` — Secretos (kubectl apply, nunca en git con valores reales)
- `08-configmap.yaml` — ConfigMap para variables no sensibles

**Resource limits** (api deployment):
```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

**Traefik IngressRoute** (NO nginx Ingress):
```yaml
apiVersion: traefik.io/v1alpha1
kind: IngressRoute
metadata:
  name: menus-api
  namespace: apptolast-menus-prod
spec:
  entryPoints:
    - websecure
  routes:
    - match: Host(`menus-api.apptolast.com`)
      kind: Rule
      services:
        - name: menus-api
          port: 8080
  tls:
    certResolver: letsencrypt
```

**URLs**:
- Dev: `menus-api-dev.apptolast.com`
- Prod: `menus-api.apptolast.com`

### 11.4 GitHub Actions CI/CD

**Pipeline** (`.github/workflows/ci-cd.yml`):
1. **test**: `./gradlew test` con Testcontainers PostgreSQL 16
2. **build**: `./gradlew build -x test`, Docker multi-stage build
3. **push**: `docker push ghcr.io/apptolast/menus-backend:<sha>`
4. **deploy-dev**: `kubectl set image` en `apptolast-menus-dev` (auto en push a main)
5. **deploy-prod**: `kubectl set image` en `apptolast-menus-prod` (manual approval)

---

## 12. Tests

### 12.1 Estructura de Tests

```
src/test/kotlin/com/apptolast/menus/
├── unit/
│   ├── AllergenFilterServiceTest.kt    # 6 casos semáforo (sin Spring context)
│   └── JwtTokenProviderTest.kt
├── integration/
│   ├── AuthControllerTest.kt           # MockMvc + Testcontainers PG
│   ├── DishControllerTest.kt           # CRUD + auditoría
│   ├── GdprControllerTest.kt           # Export + Delete + Rectify
│   ├── MultiTenancyTest.kt             # RLS: tenant A no ve tenant B
│   └── SecurityTest.kt                 # RBAC: acceso denegado por rol
└── config/
    └── TestContainersConfig.kt         # @TestConfiguration con @Container PG 16
```

### 12.2 TestContainersConfig

```kotlin
@TestConfiguration
class TestContainersConfig {
    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("menusdb_test")
            withUsername("menus")
            withPassword("menus")
        }
    }
}
```

### 12.3 Cobertura Mínima

- Código nuevo: 80%
- AllergenFilterService: 100%
- GdprService: 90% (todos los paths RGPD)
- AuthService: 85%

---

## 13. Convenciones de Código

### Kotlin
- `val` por defecto, `var` solo cuando necesario
- Entities JPA: `open class` (allOpen plugin lo gestiona automáticamente)
- UUIDs como PK: `val id: UUID = UUID.randomUUID()`
- `@field:NotBlank`, `@field:Email` (no `@NotBlank` — Kotlin annotation target)
- `FetchType.LAZY` en todas las relaciones
- `@Transactional` en la capa service, NUNCA en controllers
- `!!` prohibido en src/main/ (causa NPE en runtime)
- Extension functions para mappers: `fun Entity.toResponse(): ResponseDto`
- No WebFlux/coroutines — Spring MVC sincrónico

### Naming
- Entities: `PascalCase`, tabla snake_case (`@Table(name = "dish_allergen")`)
- DTOs: `CreateDishRequest`, `DishResponse`, `UpdateDishAllergenRequest`
- Services: interface `DishService` + impl `DishServiceImpl`
- Repositorios: `DishRepository extends JpaRepository<Dish, UUID>`
- Excepciones: `ResourceNotFoundException`, `ConflictException`, `ForbiddenException`

### Git
- Commit message en inglés, imperativo: `feat(dish): add allergen containment audit log`
- Un commit por task completada
- Branch model: `feature/T01-architect-review`, `fix/T06-jwt-filter`

---

## 14. Datos de Ejemplo (del backup real)

El backup `alergenos_backup_2026-02-24.json` contiene datos reales del restaurante "Cobre y Picón":

**Ingredientes con alérgenos** (muestra):
- Harina de trigo → GLUTEN (CONTAINS)
- Leche entera → MILK (CONTAINS)
- Huevos de gallina → EGGS (CONTAINS)
- Gambas → CRUSTACEANS (CONTAINS)
- Salmón → FISH (CONTAINS)
- Nueces → NUTS (CONTAINS)
- Mostaza → MUSTARD (CONTAINS)

**Recetas** (muestra):
- Ensalada César: lechuga, pollo, parmesano (MILK), picatostes (GLUTEN), anchoas (FISH), mostaza (MUSTARD)
- Paella: arroz, gambas (CRUSTACEANS), mejillones (MOLLUSCS), azafrán

Estos datos se usan para validar el seed V9 y como datos de prueba en tests de integración.

---

## 15. Dependencias Completas — build.gradle.kts Objetivo

```kotlin
plugins {
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    kotlin("plugin.allopen") version "2.2.21"
}

group = "com.apptolast"
version = "0.0.1-SNAPSHOT"

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Google OAuth2
    implementation("com.google.api-client:google-api-client:2.7.2")

    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

tasks.withType<Test> { useJUnitPlatform() }
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
```

---

## 16. Referencias

- **Confluence**: https://apptolast.atlassian.net/wiki/spaces/SD/pages/190709761/Menus
- **Jira**: https://apptolast.atlassian.net/jira/software/projects/MEN
- **Registry**: ghcr.io/apptolast/menus-backend
- **Reglamento UE 1169/2011**: https://eur-lex.europa.eu/legal-content/ES/TXT/?uri=CELEX:32011R1169
- **jjwt**: https://github.com/jwtk/jjwt
- **springdoc-openapi**: https://springdoc.org/v3/
- **Testcontainers**: https://java.testcontainers.org/
