# Menus Backend

API REST para la gestión de alérgenos en restaurantes — **AppToLast**

[![CI](https://github.com/apptolast/menus-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/apptolast/menus-backend/actions/workflows/ci.yml)

---

## Descripción

Backend del sistema de declaración obligatoria de **14 alérgenos EU** (Reglamento UE 1169/2011) para restaurantes españoles. Los consumidores obtienen filtrado de menú tipo semáforo (**SAFE / RISK / DANGER**) según su perfil de alergias personal.

**Modelo de negocio**: B2B (restaurantes, suscripción) + B2C (consumidores, gratuito).

---

## Stack Tecnológico

| Componente | Versión |
|---|---|
| Kotlin | 2.2 (JVM 21 Temurin) |
| Spring Boot | 4.0.3 |
| PostgreSQL | 16 (pgcrypto, RLS) |
| Flyway | 11.x (migraciones V1–V9) |
| JWT (jjwt) | 0.12.6 — HS512 |
| Google OAuth2 | google-api-client 2.7.2 |
| springdoc-openapi | 3.0.2 |
| Docker | Multi-stage (eclipse-temurin:21) |
| Kubernetes | Rancher + Traefik (NO nginx) |

---

## Inicio Rápido

### Prerrequisitos

- JDK 21 (Temurin recomendado)
- Docker Desktop (para PostgreSQL local)
- Git

### 1. Clonar y configurar

```bash
git clone https://github.com/apptolast/menus-backend.git
cd menus-backend
cp .env.example .env
# Editar .env con tus valores locales
```

### 2. Levantar base de datos

```bash
docker compose up -d
# PostgreSQL 16 disponible en localhost:5432
```

### 3. Ejecutar la aplicación

```bash
./gradlew bootRun
# API disponible en http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui/index.html
```

### 4. Ejecutar tests

```bash
# Tests unitarios (sin Docker)
./gradlew test --no-daemon

# Build completo
./gradlew build --no-daemon
```

---

## Estructura del Proyecto

```
src/main/kotlin/com/apptolast/menus/
├── MenusBackendApplication.kt
├── config/          # SecurityConfig, AppConfig, OpenApiConfig, TenantConfig
├── shared/          # DTOs comunes, excepciones, JWT, GlobalExceptionHandler
├── auth/            # AuthController, AuthService, ConsentService
├── allergen/        # AllergenController, AllergenService, AllergenFilterService
├── consumer/        # UserController, UserAllergenProfileService
├── restaurant/      # RestaurantController, RestaurantService
├── menu/            # MenuController, AdminMenuController, MenuService
├── dish/            # DishController, AdminDishController, DishService
├── gdpr/            # GdprController, GdprService
└── audit/           # AuditService, AllergenAuditLog
```

---

## API Endpoints Principales

| Método | Endpoint | Auth | Descripción |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Pública | Registro email+password |
| POST | `/api/v1/auth/login` | Pública | Login, devuelve JWT |
| POST | `/api/v1/auth/oauth2/google/callback` | Pública | Login Google |
| GET | `/api/v1/allergens` | Pública | 14 alérgenos EU |
| GET | `/api/v1/restaurants` | Pública | Buscar restaurantes |
| GET | `/api/v1/restaurants/{id}/menu` | Opt. JWT | Menú con semáforo |
| GET | `/api/v1/admin/restaurant` | JWT (Owner) | Gestión restaurante |
| POST | `/api/v1/admin/dishes` | JWT (Owner) | Crear plato |
| GET | `/api/v1/users/me/allergen-profile` | JWT+Consent | Perfil alérgenos |
| DELETE | `/api/v1/users/me/data-delete` | JWT | Borrar cuenta (RGPD) |

Documentación completa: [Swagger UI](http://localhost:8080/swagger-ui/index.html) o [api-contracts.md](docs/api-contracts.md)

---

## Seguridad y RGPD

- **JWT stateless**: Access token 15 min (HS512) + Refresh 7 días
- **Google OAuth2**: Verificación ID token via google-api-client
- **Email cifrado**: AES-256 en base de datos, SHA-256 hash para búsquedas
- **RGPD Art. 9**: `user_allergen_profile` separado de `user_account` via `profile_uuid` (sin FK directa)
- **Consentimiento explícito**: requerido antes de procesar datos de salud
- **RLS (Row-Level Security)**: aislamiento multi-tenant por `tenant_id` a nivel PostgreSQL

---

## Multi-tenancy

Cada restaurante es su propio tenant. Spring establece `SET app.current_tenant = '<uuid>'` antes de cada query, y PostgreSQL aplica RLS automáticamente.

---

## Algoritmo Semáforo

```
DANGER → algún alérgeno del usuario aparece como CONTAINS
RISK   → algún alérgeno del usuario aparece como MAY_CONTAIN (y ninguno CONTAINS)
SAFE   → ningún alérgeno del usuario aparece (o solo FREE_OF)
```

---

## Variables de Entorno

Ver [`.env.example`](.env.example) para la lista completa. Variables requeridas en producción:

| Variable | Descripción |
|---|---|
| `DATABASE_URL` | JDBC URL de PostgreSQL |
| `JWT_SECRET` | Mínimo 64 bytes para HS512 |
| `ENCRYPTION_KEY` | 32 bytes para AES-256 |
| `GOOGLE_CLIENT_ID` | OAuth2 client ID |

---

## Despliegue

Ver [docs/deploy-guide.md](docs/deploy-guide.md) para instrucciones completas de K8s.

---

## Ramas Git

| Rama | Propósito |
|---|---|
| `main` | Producción |
| `develop` | Integración |
| `feature/ws*` | Workstreams de desarrollo |

PRs: `feature/*` → `develop` → `main`

---

## Licencia

Propietaria — AppToLast. Todos los derechos reservados.
