# Changelog

Todos los cambios notables del proyecto se documentan en este archivo.

Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.0.0/).
Versionado siguiendo [Semantic Versioning](https://semver.org/lang/es/).

---

## [Unreleased]

### Added

#### Infraestructura
- Spring Boot 4.0.3 + Kotlin 2.2.21 + JVM 21 Temurin
- Flyway V1-V9: schema completo PostgreSQL 16 con pgcrypto y RLS
- Docker multi-stage (eclipse-temurin:21-jdk/jre-alpine, usuario no-root)
- docker-compose con PostgreSQL 16-alpine para desarrollo local

#### Seguridad y Autenticacion
- JWT stateless: access token 15 min (HS512) + refresh 7 dias (jjwt 0.12.6)
- Google OAuth2: verificacion ID token via google-api-client 2.7.2
- Spring Security 7.x: filtro JWT, CORS, sesion stateless
- Email cifrado AES-256 via pgcrypto + SHA-256 hash para busquedas

#### RGPD / GDPR
- Pseudonimizacion: `user_account` <-> `user_allergen_profile` via `profile_uuid` (sin FK directa)
- Consentimiento explicito Art. 9 RGPD requerido para datos de salud
- Endpoint de exportacion de datos (Art. 20)
- Endpoint de borrado y anonimizacion (Art. 17)
- Registro de consentimientos con IP + User-Agent + timestamp

#### Dominio de Negocio
- 14 alergenos EU (Reglamento UE 1169/2011): GLUTEN, CRUSTACEANS, EGGS, FISH, PEANUTS, SOYBEANS, MILK, NUTS, CELERY, MUSTARD, SESAME, SULPHITES, LUPIN, MOLLUSCS
- Traducciones ES/EN/CA/EU/GL
- Algoritmo semaforo: SAFE / RISK / DANGER por ContainmentLevel (CONTAINS, MAY_CONTAIN, FREE_OF)
- Multi-tenancy via Row-Level Security PostgreSQL (`app.current_tenant`)
- Soft delete de menus (`is_archived`)
- Audit log de cambios de alergenos (usa `profile_uuid`, no `user_id`, para RGPD)
- Suscripciones BASIC / PROFESSIONAL / PREMIUM

#### API REST (32 endpoints)
- Auth: register, login, refresh, Google OAuth2, consent RGPD
- Consumer: perfil de alergenos (GDPR-gated)
- Public: allergens, restaurants, menu con semaforo
- Admin (RESTAURANT_OWNER): gestion completa restaurante, menus, secciones, platos, alergenos
- GDPR: data-export, data-delete
- Swagger UI en `/swagger-ui/index.html`

#### Infraestructura K8s
- Traefik IngressRoute (no nginx): menus-api-dev.apptolast.com / menus-api.apptolast.com
- Longhorn PVC 5Gi para PostgreSQL
- Namespaces `apptolast-menus-dev` y `apptolast-menus-prod`, Rancher-managed
- Middleware Traefik: rate limiting (100 req/s) + security headers
- TLS automatico via cert-manager + Let's Encrypt

#### CI/CD
- GitHub Actions CI: build + tests + quality gates (no TODOs, no secrets hardcodeados)
- GitHub Actions CD: Docker build + push a ghcr.io/apptolast/menus-backend

#### Tests
- 15 tests unitarios: semaforo (11 casos), auth (4 casos) -- todos PASS
- Tests de integracion con Testcontainers PostgreSQL (requieren Docker)
- Tests de seguridad: acceso publico, control de roles, consent RGPD

### Technical Decisions
- ADR-01: Monolito modular (no microservicios)
- ADR-02: JWT stateless (no sesiones servidor)
- ADR-03: RLS multi-tenancy (no discriminator column)
- ADR-04: RGPD pseudonimizacion (profile_uuid, no FK directa)
- ADR-05: Traefik IngressRoute (no nginx Ingress)

---

[Unreleased]: https://github.com/apptolast/menus-backend/compare/main...HEAD
