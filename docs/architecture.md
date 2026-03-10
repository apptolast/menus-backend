# Menus Backend — Architecture Document

> Generado por agente architect (T01). Fecha: 2026-03-10.

---

## 1. Estado Actual del Proyecto — Errores Encontrados

### 1.1 Errores en build.gradle.kts

El archivo `build.gradle.kts` actual contiene los siguientes problemas que deben ser corregidos por el agente **backend-dev-domain**:

#### Errores de Group / Artifact ID

| # | Tipo | Línea actual (incorrecta) | Corrección |
|---|---|---|---|
| E1 | Artifact ID incorrecto (group) | `group = "com.example"` | `group = "com.apptolast"` |
| E2 | Artifact ID incorrecto (Jackson) | `implementation("tools.jackson.module:jackson-module-kotlin")` | `implementation("com.fasterxml.jackson.module:jackson-module-kotlin")` |
| E3 | Starter incorrecto | `implementation("org.springframework.boot:spring-boot-starter-webmvc")` | `implementation("org.springframework.boot:spring-boot-starter-web")` (no existe `webmvc` como starter independiente) |

#### Dependencias Inexistentes (deben eliminarse)

| # | Dependencia incorrecta | Motivo |
|---|---|---|
| E4 | `testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")` | No existe. El test correcto es `spring-boot-starter-test` |
| E5 | `testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")` | No existe como artifact independiente |
| E6 | `testImplementation("org.springframework.boot:spring-boot-starter-validation-test")` | No existe como artifact independiente |
| E7 | `testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")` | No existe. El correcto es `spring-boot-starter-test` con MockMvc |
| E8 | `testImplementation("org.springframework.boot:spring-boot-starter-security-test")` | No existe. El correcto es `org.springframework.security:spring-security-test` |

#### Dependencias Faltantes (deben añadirse)

| # | Dependencia faltante | Propósito |
|---|---|---|
| E9 | `implementation("org.flywaydb:flyway-core")` | Flyway — migraciones de base de datos |
| E10 | `implementation("org.flywaydb:flyway-database-postgresql")` | Flyway driver específico para PostgreSQL 16 |
| E11 | `implementation("io.jsonwebtoken:jjwt-api:0.12.6")` | jjwt — API JWT |
| E12 | `runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")` | jjwt — implementación runtime |
| E13 | `runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")` | jjwt — serialización Jackson |
| E14 | `implementation("com.google.api-client:google-api-client:2.7.2")` | Verificación ID token Google OAuth2 |
| E15 | `implementation("org.springframework.boot:spring-boot-starter-oauth2-client")` | Spring OAuth2 Client |
| E16 | `testImplementation("org.springframework.boot:spring-boot-starter-test")` | Test framework base (falta completamente) |
| E17 | `testImplementation("org.springframework.security:spring-security-test")` | Spring Security MockMvc test support |
| E18 | `testImplementation("org.springframework.boot:spring-boot-testcontainers")` | Integración Spring + Testcontainers |
| E19 | `testImplementation("org.testcontainers:postgresql")` | Contenedor PostgreSQL para tests |
| E20 | `testImplementation("org.testcontainers:junit-jupiter")` | Integración JUnit 5 con Testcontainers |

#### Plugins Faltantes

| # | Plugin faltante | Estado actual |
|---|---|---|
| E21 | `kotlin("plugin.allopen") version "2.2.21"` | FALTA. El bloque `allOpen {}` existe pero sin el plugin declarado, no tiene efecto |

#### Clase Principal — Paquete Incorrecto

| # | Problema | Corrección |
|---|---|---|
| E22 | `package com.example.menubackend` (MenuBackendApplication.kt) | Debe ser `com.apptolast.menus` |
| E23 | Clase llamada `MenuBackendApplication` | Debe ser `MenusBackendApplication` |

#### compose.yaml — Problemas

| # | Problema | Corrección |
|---|---|---|
| E24 | `image: 'postgres:latest'` | Debe ser `postgres:16-alpine` (versión fijada) |
| E25 | `POSTGRES_DB=mydatabase`, usuario/password genéricos | Debe usar `menusdb`, usuario `menus`, password `menus` |
| E26 | Puerto `'5432'` sin mapeo explícito | Debe ser `"5432:5432"` para acceso local |
| E27 | Falta healthcheck en postgres | Necesario para `depends_on: condition: service_healthy` |

#### application.properties — Problemas

| # | Problema | Corrección |
|---|---|---|
| E28 | Archivo es `.properties`, no `.yml` | Debe migrarse a `application.yml` con la configuración completa de la sección 10 del spec |
| E29 | Solo contiene `spring.application.name=menu-backend` | Falta toda la configuración de datasource, JPA, Flyway, JWT, CORS, actuator |

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
  │ (App móvil / │   GET /restaurants/   │       Menus Backend API          │
  │   Browser)   │   {id}/menu + JWT     │   menus-api.apptolast.com        │
  └──────────────┘                       │   (Traefik IngressRoute / K8s)   │
                                         │                                  │
  ┌──────────────┐     HTTP/HTTPS        │   Spring Security (JWT HS512)    │
  │  Restaurador │ ──────────────────>   │   Multi-tenancy RLS              │
  │  (App admin) │   /api/v1/admin/**    │   RGPD / Pseudonimización        │
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

### 2.2 Descripción del Sistema

**Menus Backend** es una API REST para la digitalización de la declaración obligatoria de los 14 alérgenos EU (Reglamento UE 1169/2011) en restaurantes españoles.

- **Modelo B2B**: Restaurantes pagan suscripción (BASIC/PROFESSIONAL/PREMIUM)
- **Modelo B2C**: Consumidores usan la app gratuitamente
- **Regulación**: Cumplimiento RGPD con pseudonimización de datos de salud

---

## 3. Módulos y Responsabilidades

```
com.apptolast.menus/
├── config/          Configuración transversal (Security, JWT, OpenAPI, Tenant/RLS)
├── shared/          DTOs comunes, excepciones de negocio, seguridad JWT, handler global
├── auth/            Autenticación: registro, login, refresh token, OAuth2 Google, consentimiento
├── allergen/        Catálogo de 14 alérgenos EU (solo lectura pública)
├── restaurant/      Gestión de restaurantes y suscripciones
├── menu/            CRUD de menús y secciones (admin) + vista pública
├── dish/            CRUD de platos + lógica semáforo SAFE/RISK/DANGER
├── consumer/        Perfil de alérgenos del consumidor (datos de salud separados RGPD)
├── gdpr/            Exportación, eliminación y rectificación de datos personales
└── audit/           Log de auditoría de cambios en alérgenos de platos
```

### Responsabilidades Detalladas

| Módulo | Capa | Responsabilidad principal |
|---|---|---|
| `config` | Infraestructura | SecurityConfig (JWT filter chain), JwtConfig (propiedades), OpenApiConfig (Swagger), TenantConfig (RLS ThreadLocal) |
| `shared` | Transversal | ErrorResponse/PageResponse DTOs, jerarquía de excepciones de negocio, JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal, GlobalExceptionHandler |
| `auth` | Feature | Registro email+password, login, refresh token, OAuth2 Google callback, gestión de consentimiento RGPD |
| `allergen` | Feature | Exposición del catálogo de 14 alérgenos EU con traducciones (ES/EN/CA/EU/GL). Solo lectura pública |
| `restaurant` | Feature | CRUD restaurante propio (admin), búsqueda pública de restaurantes, gestión de suscripciones |
| `menu` | Feature | CRUD menús y secciones (admin, RLS por tenant), vista pública del menú |
| `dish` | Feature | CRUD platos con alérgenos (admin), filtrado semáforo para consumidor, registro en audit_log |
| `consumer` | Feature | Gestión del perfil de alérgenos del usuario (requiere consentimiento explícito) |
| `gdpr` | Feature | Endpoints RGPD: exportación de datos, eliminación de cuenta, rectificación |
| `audit` | Feature | Almacenamiento y consulta del log de cambios en allergen_audit_log |

---

## 4. ADRs — Architecture Decision Records

### ADR-01: Monolito Modular vs Microservicios

**Fecha**: 2026-03-10
**Estado**: Aceptado

**Contexto**: El proyecto es una MVP con un equipo pequeño. Se necesita velocidad de desarrollo y simplicidad operacional.

**Decisión**: Monolito modular con paquetes claramente delimitados (`auth`, `allergen`, `restaurant`, `menu`, `dish`, `consumer`, `gdpr`, `audit`).

**Consecuencias positivas**:
- Un único deployment, menos complejidad operacional
- Transacciones locales (sin Saga pattern)
- Refactorización a microservicios posible en el futuro siguiendo las fronteras de módulo ya establecidas

**Consecuencias negativas**:
- Escalado horizontal de todo el monolito, no de módulos individuales
- Un fallo puede afectar a todos los módulos

---

### ADR-02: JWT Stateless vs Sesiones HTTP

**Fecha**: 2026-03-10
**Estado**: Aceptado

**Contexto**: La API es consumida por apps móviles y frontends SPA. Se necesita autenticación sin estado para facilitar el escalado horizontal.

**Decisión**: JWT stateless con dos tokens:
- **Access token**: firmado HS512, validez 15 minutos, claims: `sub` (userId), `role`, `tenantId`
- **Refresh token**: firmado con mismo secret, validez 7 días, claims: `sub` (userId), `type: "refresh"`
- Secret: mínimo 64 bytes, desde variable de entorno `JWT_SECRET`
- Librería: **jjwt 0.12.6**

**Consecuencias positivas**:
- Sin estado en el servidor (STATELESS en Spring Security)
- Compatible con escalado horizontal sin sesión compartida
- Access token corto reduce ventana de ataque

**Consecuencias negativas**:
- No hay revocación inmediata de tokens (hasta expiración del access token)
- El refresh token de 7 días debe protegerse en cliente

---

### ADR-03: Multi-tenancy via Row-Level Security de PostgreSQL

**Fecha**: 2026-03-10
**Estado**: Aceptado

**Contexto**: Cada restaurante es un tenant. Sus menús, secciones, platos y alérgenos son privados y no deben ser visibles para otros restaurantes.

**Decisión**: Row-Level Security (RLS) de PostgreSQL con `SET app.current_tenant = '<tenantId>'` establecido en cada request. El `tenant_id` del restaurante autenticado se incluye en el JWT y se aplica antes de cada query mediante un filtro (`TenantFilter`).

**Tablas con RLS**: `restaurant`, `menu`, `menu_section`, `dish`, `dish_allergen`

**Tablas sin RLS** (referencia/globales): `allergen`, `allergen_translation`, `user_account`, `user_allergen_profile`, `consent_record`, `oauth_account`, `allergen_audit_log`

**Consecuencias positivas**:
- Aislamiento de datos garantizado a nivel de base de datos (no solo aplicación)
- Protección incluso ante bugs en la capa de aplicación
- Sin necesidad de añadir `WHERE tenant_id = ?` en cada query JPA

**Consecuencias negativas**:
- Requiere `SET` antes de cada transacción (overhead mínimo)
- Complejidad en tests de integración (necesita configurar tenant en Testcontainers)

---

### ADR-04: Pseudonimización RGPD — user_account ↔ user_allergen_profile via profile_uuid

**Fecha**: 2026-03-10
**Estado**: Aceptado

**Contexto**: Los datos de salud (alérgenos) son datos especialmente sensibles bajo el Artículo 9 del RGPD. Deben poder eliminarse sin afectar a los datos de identidad del usuario, y viceversa.

**Decisión**: Separación física en dos tablas sin FK explícita entre ellas:
- `user_account` contiene datos de identidad: `email` (cifrado AES-256 con pgcrypto), `email_hash` (SHA-256 para búsquedas), `profile_uuid` (UUID aleatorio)
- `user_allergen_profile` contiene datos de salud: `profile_uuid` (link lógico, sin FK declarada en DDL)
- Los logs de auditoría usan `profile_uuid`, nunca `user_id`
- El consentimiento explícito (`consent_record` de tipo `HEALTH_DATA_PROCESSING`) es obligatorio para acceder al perfil de alérgenos

**Consecuencias positivas**:
- Borrado selectivo: eliminar `user_allergen_profile` sin tocar `user_account` (y viceversa)
- Pseudonimización real: si se elimina la correspondencia en `user_account`, el perfil queda huérfano irreversiblemente
- Cumplimiento del principio de minimización de datos del RGPD

**Consecuencias negativas**:
- Joins requieren lógica en la capa de aplicación (no mediante FK de base de datos)
- El desarrollador debe entender la separación para no introducir FKs accidentalmente

---

### ADR-05: Traefik vs nginx como Ingress Controller

**Fecha**: 2026-03-10
**Estado**: Aceptado

**Contexto**: El cluster Kubernetes es gestionado por Rancher. Rancher instala Traefik como Ingress Controller por defecto.

**Decisión**: Usar **Traefik** con `IngressRoute` (CRD `traefik.io/v1alpha1`), NO nginx Ingress. TLS gestionado por cert-manager con Let's Encrypt.

**Consecuencias positivas**:
- Sin instalación adicional — Traefik ya está disponible en el cluster Rancher
- `IngressRoute` es más expresivo que `Ingress` estándar
- cert-manager con `certResolver: letsencrypt` gestiona renovación automática de certificados

**Consecuencias negativas**:
- `IngressRoute` es un CRD de Traefik (vendor lock-in), no es `Ingress` estándar de Kubernetes
- Si el cluster migra a otro proveedor, hay que reescribir los manifests de ingress

---

## 5. Flyway Migrations — Orden y Descripción (V1–V9)

Las migraciones deben ejecutarse en este orden estricto. Cada script debe residir en `src/main/resources/db/migration/`.

| Orden | Archivo | Descripción | Dependencias |
|---|---|---|---|
| 1 | `V1__create_schema_extensions.sql` | Habilita extensiones PostgreSQL: `pgcrypto` (cifrado AES-256, SHA-256) y `uuid-ossp` (generación UUID). Sin estas extensiones, V3 fallará. | Ninguna |
| 2 | `V2__create_reference_tables.sql` | Crea tablas de referencia global: `allergen` (14 alérgenos EU con código y URL de icono) y `allergen_translation` (nombres en ES/EN/CA/EU/GL). Sin RLS — son datos públicos. | V1 (uuid-ossp para UUIDs) |
| 3 | `V3__create_user_tables.sql` | Crea tablas de usuarios: `user_account` (email cifrado BYTEA, email_hash SHA-256, profile_uuid), `user_allergen_profile` (datos de salud separados, sin FK a user_account), `consent_record` (historial de consentimientos RGPD), `oauth_account` (cuentas Google vinculadas). | V1 (pgcrypto, uuid-ossp) |
| 4 | `V4__create_restaurant_tables.sql` | Crea `restaurant` (owner_id FK a user_account, tenant_id=id, slug único) y `subscription` (tier BASIC/PROFESSIONAL/PREMIUM, límites de menús y platos). | V3 (FK owner_id → user_account) |
| 5 | `V5__create_menu_tables.sql` | Crea `menu` (restaurant_id FK, tenant_id para RLS, is_archived soft delete, display_order) y `menu_section` (menu_id FK, tenant_id, display_order). | V4 (FK restaurant_id → restaurant) |
| 6 | `V6__create_dish_tables.sql` | Crea `dish` (section_id FK, tenant_id, price, is_available) y `dish_allergen` (dish_id + allergen_id + tenant_id + containment_level CONTAINS/MAY_CONTAIN/FREE_OF, UNIQUE dish+allergen). Esta es la tabla más crítica del dominio. | V2 (FK allergen_id), V5 (FK section_id) |
| 7 | `V7__create_audit_tables.sql` | Crea `allergen_audit_log` (dish_id, allergen_id, tenant_id, changed_by_uuid usando profile_uuid nunca user_id, action ADD/REMOVE/UPDATE, old_level, new_level). Sin FK a user_account por diseño RGPD. | V6 (referencia lógica a dish_allergen) |
| 8 | `V8__enable_rls.sql` | Habilita Row-Level Security en 5 tablas: `restaurant`, `menu`, `menu_section`, `dish`, `dish_allergen`. Crea policies `tenant_isolation` que filtran por `current_setting('app.current_tenant')::UUID`. | V4, V5, V6 (tablas deben existir antes de aplicar RLS) |
| 9 | `V9__seed_allergens.sql` | Inserta los 14 alérgenos del Reglamento UE 1169/2011 con traducciones en 5 idiomas (ES, EN, CA, EU, GL). Usa INSERT sin conflicto para ser idempotente. | V2 (tablas allergen y allergen_translation) |

**Nota crítica**: V8 debe ejecutarse DESPUÉS de V4-V6 porque aplica RLS a tablas ya creadas. V9 debe ejecutarse DESPUÉS de V2 porque inserta datos en las tablas de referencia.

---

## 6. Diagrama de Dependencias entre Módulos

```
                        ┌─────────────┐
                        │   config/   │
                        │ (Security,  │
                        │  JWT, RLS,  │
                        │  OpenAPI)   │
                        └──────┬──────┘
                               │ usa
                               ▼
                        ┌─────────────┐
                        │   shared/   │◄──────────────────────────────┐
                        │ (DTOs,      │                               │
                        │  Exceptions,│                               │
                        │  JwtFilter, │                               │
                        │  Handler)   │                               │
                        └──────┬──────┘                               │
                               │ usa                                  │
              ┌────────────────┼─────────────────────────┐            │
              │                │                         │            │
              ▼                ▼                         ▼            │
       ┌────────────┐  ┌──────────────┐          ┌────────────┐      │
       │   auth/    │  │  allergen/   │          │ consumer/  │      │
       │ (registro, │  │ (catálogo 14 │          │ (perfil    │      │
       │  login,    │  │  alérgenos,  │          │  alérgenos,│      │
       │  OAuth2,   │  │  público)    │          │  consent)  │      │
       │  consent)  │  └──────────────┘          └─────┬──────┘      │
       └──────┬─────┘                                  │             │
              │                                        │             │
              │ crea UserAccount                       │ usa         │
              ▼                                        │             │
       ┌──────────────┐                                │             │
       │  restaurant/ │◄───── auth/ (owner_id)         │             │
       │ (CRUD,       │                                │             │
       │  suscripción)│                                │             │
       └──────┬───────┘                                │             │
              │ tiene                                  │             │
              ▼                                        │             │
       ┌──────────────┐                                │             │
       │    menu/     │                                │             │
       │ (menús y     │                                │             │
       │  secciones)  │                                │             │
       └──────┬───────┘                                │             │
              │ contiene                               │             │
              ▼                                        ▼             │
       ┌──────────────┐◄────── allergen/ (catálogo) ───┘             │
       │    dish/     │                                               │
       │ (platos,     │──── AllergenFilterService ─────────────────► │
       │  semáforo)   │     usa consumer/ profile                     │
       └──────┬───────┘                                               │
              │ registra                                              │
              ▼                                                       │
       ┌──────────────┐                                               │
       │   audit/     │                                               │
       │ (audit_log)  │───────────────────────────────────────────► usa
       └──────────────┘                                               │
                                                                      │
       ┌──────────────┐                                               │
       │    gdpr/     │──── usa consumer/ + auth/ + restaurant/ ─────►│
       │ (export,     │                                               │
       │  delete,     │                                               │
       │  rectify)    │                                               │
       └──────────────┘
```

**Regla de dependencia**: Los módulos de feature (`auth`, `allergen`, `restaurant`, `menu`, `dish`, `consumer`, `gdpr`, `audit`) pueden depender de `shared/` y `config/`, pero NO deben depender entre sí excepto las relaciones explícitas de dominio descritas arriba.

---

## 7. Lista Completa de Archivos a Crear por Agente

### Agente: backend-dev-domain

**build.gradle.kts** (refactorizar):
- Corregir `group` a `com.apptolast`
- Corregir todos los errores E1–E23 documentados en sección 1

**compose.yaml** (refactorizar):
- Corregir errores E24–E27

**src/main/resources/application.yml** (nuevo, reemplaza application.properties):
- Configuración completa según spec sección 10

**src/main/kotlin/com/apptolast/menus/** (estructura completa):

```
MenusBackendApplication.kt

config/
  SecurityConfig.kt
  JwtConfig.kt
  OpenApiConfig.kt
  TenantConfig.kt          (TenantFilter — RLS)

shared/
  dto/
    ErrorResponse.kt
    PageResponse.kt
  exception/
    BusinessException.kt
    ResourceNotFoundException.kt
    ConflictException.kt
    ForbiddenException.kt
    ConsentRequiredException.kt
  security/
    JwtTokenProvider.kt
    JwtAuthenticationFilter.kt
    UserPrincipal.kt
  handler/
    GlobalExceptionHandler.kt

auth/
  controller/
    AuthController.kt
  dto/
    request/
      RegisterRequest.kt
      LoginRequest.kt
      RefreshTokenRequest.kt
      GoogleCallbackRequest.kt
    response/
      AuthResponse.kt
  service/
    AuthService.kt           (interface)
    ConsentService.kt
    impl/
      AuthServiceImpl.kt
      OAuth2ServiceImpl.kt

allergen/
  controller/
    AllergenController.kt
  dto/response/
    AllergenResponse.kt
  model/
    entity/
      Allergen.kt
      AllergenTranslation.kt
    enum/
      AllergenCode.kt
  repository/
    AllergenRepository.kt
    AllergenTranslationRepository.kt
  service/
    AllergenService.kt
    impl/
      AllergenServiceImpl.kt

restaurant/
  controller/
    RestaurantController.kt
    AdminRestaurantController.kt
  dto/
    request/
      RestaurantRequest.kt
    response/
      RestaurantResponse.kt
  model/
    entity/
      Restaurant.kt
      Subscription.kt
    enum/
      SubscriptionTier.kt
  repository/
    RestaurantRepository.kt
  service/
    RestaurantService.kt
    impl/
      RestaurantServiceImpl.kt

menu/
  controller/
    MenuController.kt
    AdminMenuController.kt
  dto/
    request/
      MenuRequest.kt
      SectionRequest.kt
    response/
      MenuResponse.kt
      SectionResponse.kt
  model/
    entity/
      Menu.kt
      MenuSection.kt
  repository/
    MenuRepository.kt
    MenuSectionRepository.kt
  service/
    MenuService.kt
    impl/
      MenuServiceImpl.kt

dish/
  controller/
    DishController.kt
    AdminDishController.kt
  dto/
    request/
      DishRequest.kt
    response/
      DishResponse.kt
      DishAllergenResponse.kt
  model/
    entity/
      Dish.kt
      DishAllergen.kt
    enum/
      ContainmentLevel.kt
  repository/
    DishRepository.kt
    DishAllergenRepository.kt
  service/
    DishService.kt
    AllergenFilterService.kt
    impl/
      DishServiceImpl.kt
      AllergenFilterServiceImpl.kt

consumer/
  controller/
    UserController.kt
  dto/
    request/
      AllergenProfileRequest.kt
    response/
      AllergenProfileResponse.kt
  model/
    entity/
      UserAccount.kt
      UserAllergenProfile.kt
      ConsentRecord.kt
      OAuthAccount.kt
    enum/
      UserRole.kt
  repository/
    UserAccountRepository.kt
    UserAllergenProfileRepository.kt
    ConsentRecordRepository.kt
    OAuthAccountRepository.kt
  service/
    UserAllergenProfileService.kt
    impl/
      UserAllergenProfileServiceImpl.kt

gdpr/
  controller/
    GdprController.kt
  dto/
    request/
      RectificationRequest.kt
    response/
      DataExportResponse.kt
  service/
    GdprService.kt
    impl/
      GdprServiceImpl.kt

audit/
  model/
    entity/
      AllergenAuditLog.kt
  repository/
    AllergenAuditLogRepository.kt
  service/
    AuditService.kt
    impl/
      AuditServiceImpl.kt
```

**src/main/resources/db/migration/**:
```
V1__create_schema_extensions.sql
V2__create_reference_tables.sql
V3__create_user_tables.sql
V4__create_restaurant_tables.sql
V5__create_menu_tables.sql
V6__create_dish_tables.sql
V7__create_audit_tables.sql
V8__enable_rls.sql
V9__seed_allergens.sql
```

**src/test/kotlin/com/apptolast/menus/**:
```
unit/
  AllergenFilterServiceTest.kt     (6 casos semáforo, sin Spring context)
  JwtTokenProviderTest.kt
integration/
  AuthControllerTest.kt            (MockMvc + Testcontainers PG)
  DishControllerTest.kt            (CRUD + auditoría)
  GdprControllerTest.kt            (Export + Delete + Rectify)
  MultiTenancyTest.kt              (RLS: tenant A no ve tenant B)
  SecurityTest.kt                  (RBAC: acceso denegado por rol)
config/
  TestContainersConfig.kt          (@TestConfiguration con @Container PG 16)
```

### Agente: infra

**Dockerfile** (nuevo):
- Multi-stage: `eclipse-temurin:21-jdk-alpine` (build) + `eclipse-temurin:21-jre-alpine` (runtime)
- Usuario no-root `appuser`
- HEALTHCHECK con wget a `/actuator/health`

**k8s/** (nuevos):
```
00-namespace.yaml
01-postgres-pvc.yaml
02-postgres-deployment.yaml
03-postgres-service.yaml
04-api-deployment.yaml
05-api-service.yaml
06-api-ingress.yaml             (Traefik IngressRoute, NO nginx)
07-secrets.yaml                 (plantilla sin valores reales)
08-configmap.yaml
```

**.github/workflows/ci-cd.yml** (nuevo):
- Jobs: test → build → push (ghcr.io) → deploy-dev (auto) → deploy-prod (manual)

---

## 8. Blockers y Decisiones Pendientes de Validación

1. **Kotlin versión**: El spec indica `2.2` pero en build.gradle.kts aparece `2.2.21`. Asumir `2.2.21` como versión exacta correcta.

2. **springdoc-openapi 3.0.1 con Spring Boot 4**: La versión 3.0.1 de springdoc-openapi está diseñada para Spring Boot 3.x. Spring Boot 4 usa Spring Framework 7.x. Verificar compatibilidad o usar la versión más reciente de springdoc compatible con Spring Boot 4.

3. **Kotlin plugin.allopen vs plugin.jpa**: El spec pide añadir `plugin.allopen` explícitamente, pero `plugin.jpa` ya implica allopen para entidades JPA. Ambos deben declararse explícitamente para mayor claridad (el spec lo requiere así).

4. **`-Xannotation-default-target=param-property`**: Esta flag del compilador está en el build actual pero no en el objetivo del spec (sección 15). Mantener si es necesaria para `@field:` annotations de Kotlin con Jakarta Validation.
