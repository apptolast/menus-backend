# Menus Backend — CLAUDE.md

Sistema de gestión de alérgenos para restauración. Backend API REST con Spring Boot 4.0 + Kotlin 2.2 + PostgreSQL 16 + Kubernetes (Rancher/Traefik).

# Instrucciones , para el uso del equipo de agentes y subagentes

Tienes que utilizar siempre tu equipo de agentes y subagentes , A SU VEZ TIENES QUE BUSCAR SIEMPRE DOCUMENTACION OFICIAL DE LA TECNOLOGIA , que estés utilizando , para minimizar los errores al máximo

## Descripción del Proyecto
Plataforma B2B/B2C que digitaliza la declaración obligatoria de los **14 alérgenos EU** (Reglamento UE 1169/2011) para restaurantes españoles. Los consumidores reciben filtrado de menú tipo semáforo (SAFE/RISK/DANGER). Los restaurantes gestionan menús, platos y alérgenos desde un admin dashboard.

**Solo backend + infra.** No hay frontend en este repo.

## Stack Tecnológico
- **Lenguaje**: Kotlin 2.2 (JVM 21 Temurin)
- **Framework**: Spring Boot 4.0.3
- **BD**: PostgreSQL 16 con pgcrypto (AES-256) y RLS (multi-tenancy)
- **ORM**: Spring Data JPA + Hibernate
- **Seguridad**: Spring Security 7.x + JWT (jjwt 0.12.6) + Google OAuth2
- **Docs API**: springdoc-openapi 3.0.1 (Swagger UI)
- **Migraciones**: Flyway
- **Build**: Gradle Kotlin DSL 8.10+
- **Infra**: Kubernetes (Rancher), Traefik ingress, Longhorn storage, cert-manager

## Comandos Esenciales
```bash
./gradlew build              # Compilar y empaquetar
./gradlew test               # Ejecutar tests
./gradlew bootRun            # Arrancar localmente
./gradlew build -x test      # Compilar sin tests
./gradlew dependencies       # Ver árbol de dependencias
docker compose up -d         # Arrancar PostgreSQL local
kubectl get pods -n apptolast-menus-dev   # Ver pods en dev
```

## Paquete Base
`com.apptolast.menus` (IMPORTANTE: el código inicial usa `com.example.menubackend` — debe ser refactorizado)

## Estructura de Módulos
```
com.apptolast.menus/
├── config/           # SecurityConfig, JwtConfig, OpenApiConfig, TenantConfig
├── shared/           # ErrorResponse, PageResponse, excepciones globales, seguridad compartida
├── auth/             # Autenticación: registro, login, JWT, Google OAuth2, consentimiento
├── allergen/         # Catálogo 14 alérgenos EU + traducciones
├── restaurant/       # CRUD restaurante + suscripción
├── menu/             # CRUD menús + secciones
├── dish/             # CRUD platos + alérgenos de platos (CRÍTICO)
├── consumer/         # Perfil de alérgenos del consumidor
├── gdpr/             # Derechos RGPD: export, delete, rectify
└── audit/            # allergen_audit_log

Cada módulo tiene: controller/ | dto/request/ | dto/response/ | service/interface/ | service/impl/ | repository/ | model/entity/ | model/enum/ | mapper/ | exception/
```

## Ownership de Archivos por Agente (NO EDITAR FUERA DE TU ZONA)

| Agente | Owns |
|---|---|
| architect | `docs/architecture.md`, `docs/api-contracts.md` — SOLO LECTURA en src/ |
| backend-dev-domain | `src/main/kotlin/.../model/`, `src/main/kotlin/.../repository/`, `src/main/resources/db/migration/`, `build.gradle.kts`, `src/main/resources/application.yml` |
| backend-dev-auth | `src/main/kotlin/.../auth/`, `src/main/kotlin/.../config/SecurityConfig.kt`, `src/main/kotlin/.../config/JwtConfig.kt`, `src/main/kotlin/.../shared/security/` |
| backend-dev-services | `src/main/kotlin/.../*/service/`, `src/main/kotlin/.../shared/exception/`, `src/main/kotlin/.../config/TenantConfig.kt` |
| backend-dev-api | `src/main/kotlin/.../*/controller/`, `src/main/kotlin/.../*/dto/`, `src/main/kotlin/.../*/mapper/`, `src/main/kotlin/.../config/OpenApiConfig.kt` |
| qa-engineer | `src/test/**` — NUNCA editar src/main/ |
| security-reviewer | READ ONLY en todo el proyecto |
| devops-engineer | `Dockerfile`, `compose.yaml`, `.github/workflows/`, `k8s/`, `scripts/` |
| tech-writer | `docs/`, `README.md` |
| code-reviewer | READ ONLY |
| mentor | READ ONLY |

## API REST — Resumen de Endpoints

### Auth (`/api/v1/auth/`)
- POST `/register` — Registro email/password
- POST `/login` — Login, devuelve JWT
- POST `/refresh` — Refresh token
- GET `/oauth2/google` — Iniciar OAuth2 Google
- POST `/oauth2/google/callback` — Callback Google, devuelve JWT
- POST `/consent` — Consentimiento RGPD (datos de salud)
- DELETE `/consent` — Revocar consentimiento

### Consumer (`/api/v1/`)
- GET `/users/me/allergen-profile` — Perfil alérgenos (JWT + Consent)
- PUT `/users/me/allergen-profile` — Actualizar perfil
- GET `/restaurants` — Buscar restaurantes
- GET `/restaurants/{id}` — Detalle restaurante
- GET `/restaurants/{id}/menu` — Menú con filtrado semáforo
- POST `/scan/barcode` — Escanear código de barras

### Admin (`/api/v1/admin/`) — JWT (RESTAURANT_OWNER)
- GET/PUT `/restaurant` — Datos del restaurante
- GET/POST `/menus` — Listar/crear menús
- PUT/DELETE `/menus/{id}` — Actualizar/archivar (soft delete)
- POST `/menus/{id}/sections` — Crear sección
- GET/POST `/dishes` — Platos
- PUT/DELETE `/dishes/{id}` — Actualizar/eliminar plato
- POST `/qr/generate` — Generar QR
- GET `/analytics` — Estadísticas
- GET `/subscription` — Suscripción actual

### RGPD (`/api/v1/users/me/`)
- GET `/data-export` — Exportar datos (JSON/ZIP)
- DELETE `/data-delete` — Eliminar cuenta
- PUT `/data-rectification` — Rectificar datos

## Base de Datos — Entidades Principales

| Tabla | Descripción |
|---|---|
| allergen | 14 alérgenos EU (GLUTEN, EGGS, FISH...) — tabla de referencia |
| allergen_translation | Traducciones por locale (es, en, ca, eu, gl) |
| restaurant | Restaurante con tenant_id para RLS |
| menu | Menú (soft delete via is_archived) |
| menu_section | Sección del menú (display_order) |
| dish | Plato con precio e imagen |
| dish_allergen | CRÍTICA: dish + allergen + ContainmentLevel (CONTAINS/MAY_CONTAIN/FREE_OF) |
| user_account | Datos personales (email cifrado AES-256) |
| user_allergen_profile | Datos de salud SEPARADOS — link por profile_uuid |
| consent_record | Auditoría de consentimiento RGPD |
| oauth_account | Cuentas OAuth2 (Google, extensible) |
| allergen_audit_log | Auditoría de cambios en alérgenos |
| subscription | Suscripción del restaurante (BASIC/PROFESSIONAL/PREMIUM) |

## Multi-tenancy con RLS
- Todas las tablas de restaurante tienen `tenant_id UUID`
- PostgreSQL Row-Level Security aísla datos por tenant
- Al inicio de cada request: `SET app.current_tenant = '<uuid>'`
- El `tenant_id` del restaurant ES el `tenant_id` de todos sus recursos

## Pseudonimización RGPD (OBLIGATORIO)
- `user_account.profile_uuid` (UUID aleatorio) vincula con `user_allergen_profile.profile_uuid`
- NO hay FK directa entre datos personales y datos de salud
- Los logs de auditoría usan profile_uuid, nunca datos personales

## Roles de Usuario (RBAC)
- `CONSUMER` — Acceso a su perfil y menús públicos
- `RESTAURANT_OWNER` — Gestión completa de su restaurante
- `KITCHEN_STAFF` — Solo lectura de alérgenos de platos
- `ADMIN` — Plataforma, sin acceso a datos de salud individuales

## Lógica de Filtrado de Alérgenos (Semáforo)
```kotlin
// Para cada plato, comparar dish_allergen con user_allergen_profile:
SAFE   = ningún alérgeno del perfil aparece en el plato
RISK   = algún alérgeno del perfil tiene MAY_CONTAIN
DANGER = algún alérgeno del perfil tiene CONTAINS
```

## Convenciones de Código Kotlin
- Entities JPA: `open class` (plugin `allOpen` lo gestiona)
- UUIDs como PK en entidades de negocio: `val id: UUID = UUID.randomUUID()`
- IDs autoincrementales en tablas de referencia/join: `@GeneratedValue(IDENTITY)`
- Extension functions para mappers: `fun Entity.toResponse() = ResponseDto(...)`
- Coroutines NO — Spring MVC sincrónico (no WebFlux)
- `@field:NotBlank` en DTOs (no `@NotBlank` — Kotlin annotation target)
- `FetchType.LAZY` por defecto en todas las asociaciones
- `@Transactional` en la capa de servicio, nunca en controllers
- Null safety: `?` explícito, nunca `!!` salvo en código de tests

## Infraestructura K8s (Rancher/Traefik)
- Ingress controller: **Traefik** (NO nginx)
- Persistent storage: **Longhorn** (PVC 5GB para PostgreSQL)
- TLS: **cert-manager + Let's Encrypt**
- Namespaces: `apptolast-menus-dev` y `apptolast-menus-prod`
- Registry: `ghcr.io/apptolast/menus-backend`
- Base image: `eclipse-temurin:21-jre-alpine`
- URLs: `menus-api-dev.apptolast.com` / `menus-api.apptolast.com`

## Formato de Error Estándar (SIEMPRE)
```json
{
  "error": {
    "code": "ALLERGEN_PROFILE_CONSENT_REQUIRED",
    "message": "You must provide explicit consent...",
    "status": 403,
    "timestamp": "2026-03-10T10:30:00Z"
  }
}
```

## Reglas del Agent Team

### Tareas
- Cada task debe ser completable en 10-20 min
- Verificar `./gradlew build -x test` antes de marcar como completa
- Commit atómico por task con mensaje descriptivo en inglés
- Notificar al team-lead con SendMessage al completar

### Quality Gates
- Build limpio: `./gradlew build`
- Tests: `./gradlew test` — coverage mínimo 80% en código nuevo
- No hay TODO en código enviado
- GlobalExceptionHandler cubre todas las excepciones personalizadas

### Waves de ejecución
1. **Domain** (architect + backend-dev-domain): entidades, repos, migraciones Flyway
2. **Security + Auth** (backend-dev-auth): JWT, Spring Security, OAuth2
3. **Services** (backend-dev-services): lógica de negocio de todos los módulos
4. **API** (backend-dev-api): controllers, DTOs, mappers, OpenAPI
5. **Testing** (qa-engineer): unit + integration tests
6. **Review** (code-reviewer + security-reviewer): audit
7. **DevOps** (devops-engineer): Dockerfile, K8s, CI/CD
8. **Docs** (tech-writer): README, API docs

### Compaction — Preservar siempre
- Lista de archivos modificados y su estado
- Módulos completados vs pendientes
- Comandos de build y test
- Tenant actual del contexto de request (RLS)

## Referencia a Documentación
- @docs/technical-spec.md — Documento técnico completo
- @docs/architecture.md — Decisiones de arquitectura (lo crea el architect)
- Confluence: https://apptolast.atlassian.net/wiki/spaces/SD/pages/190709761/Menus
