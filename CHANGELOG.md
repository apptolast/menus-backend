# Changelog

Todos los cambios notables del proyecto se documentan en este archivo.

Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.0.0/).
Versionado siguiendo [Semantic Versioning](https://semver.org/lang/es/).

---

## [Unreleased] — 0.0.1-SNAPSHOT

### Added
- Implementación completa del backend API REST de alérgenos

---

## [1.0.0] — Planificado

### Added

#### Infraestructura
- Spring Boot 4.0.3 + Kotlin 2.2.21 + JVM 21 Temurin
- Flyway V1–V9: schema completo PostgreSQL 16 con pgcrypto y RLS
- Docker multi-stage (eclipse-temurin:21-jdk/jre-alpine, usuario no-root)
- docker-compose con PostgreSQL 16-alpine para desarrollo local

#### Seguridad y Autenticación
- JWT stateless: access token 15 min (HS512) + refresh 7 días (jjwt 0.12.6)
- Google OAuth2: verificación ID token via google-api-client 2.7.2
- Spring Security 7.x: filtro JWT, CORS, sesión stateless
- Email cifrado AES-256 via pgcrypto + SHA-256 hash para búsquedas

#### RGPD / GDPR
- Pseudonimización: `user_account` ↔ `user_allergen_profile` via `profile_uuid` (sin FK directa)
- Consentimiento explícito Art. 9 RGPD requerido para datos de salud
- Endpoint de exportación de datos (Art. 20)
- Endpoint de borrado y anonimización (Art. 17)
- Registro de consentimientos con IP + User-Agent + timestamp

#### Dominio de Negocio
- 14 alérgenos EU (Reglamento UE 1169/2011): GLUTEN, CRUSTACEANS, EGGS, FISH, PEANUTS, SOYBEANS, MILK, NUTS, CELERY, MUSTARD, SESAME, SULPHITES, LUPIN, MOLLUSCS
- Traducciones ES/EN/CA/EU/GL
- Algoritmo semáforo: SAFE / RISK / DANGER por ContainmentLevel (CONTAINS, MAY_CONTAIN, FREE_OF)
- Multi-tenancy via Row-Level Security PostgreSQL (`app.current_tenant`)
- Soft delete de menús (`is_archived`)
- Audit log de cambios de alérgenos (usa `profile_uuid`, no `user_id`, para RGPD)
- Suscripciones BASIC / PROFESSIONAL / PREMIUM

#### API REST (32 endpoints)
- Auth: register, login, refresh, Google OAuth2, consent RGPD
- Consumer: perfil de alérgenos (GDPR-gated)
- Public: allergens, restaurants, menu con semáforo
- Admin (RESTAURANT_OWNER): gestión completa restaurante, menús, secciones, platos, alérgenos
- GDPR: data-export, data-delete
- Swagger UI en `/swagger-ui/index.html`

#### Infraestructura K8s
- Traefik IngressRoute (no nginx): menus-api-dev.apptolast.com / menus-api.apptolast.com
- Longhorn PVC 5Gi para PostgreSQL
- Namespace `menus-backend`, Rancher-managed
- Middleware Traefik: rate limiting (100 req/s) + security headers
- TLS automático via cert-manager + Let's Encrypt

#### CI/CD
- GitHub Actions CI: build + tests + quality gates (no TODOs, no secrets hardcodeados)
- GitHub Actions CD: Docker build + push a ghcr.io/apptolast/menus-backend

#### Tests
- Batería de tests unitarios planificada: semáforo (casuística principal) y auth
- Tests de integración con Testcontainers PostgreSQL previstos (requieren Docker)
- Tests de seguridad planificados: acceso público, control de roles, consent RGPD

### Technical Decisions
- ADR-01: Monolito modular (no microservicios)
- ADR-02: JWT stateless (no sesiones servidor)
- ADR-03: RLS multi-tenancy (no discriminator column)
- ADR-04: RGPD pseudonimización (profile_uuid, no FK directa)
- ADR-05: Traefik IngressRoute (no nginx Ingress)

---

[Unreleased]: https://github.com/apptolast/menus-backend/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/apptolast/menus-backend/releases/tag/v1.0.0
