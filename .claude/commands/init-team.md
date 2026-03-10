---
description: Inicializa el equipo de agentes para el proyecto Menus Backend
allowed-tools: Read, Bash, Write, Edit
---

Lee el documento técnico en @docs/technical-spec.md y crea el agent team completo para el proyecto Menus Backend.

## Pasos de Orquestación

1. **Lee** @CLAUDE.md y @docs/technical-spec.md completamente
2. **Verifica** el estado actual del proyecto: `./gradlew build -x test 2>&1 | tail -20`
3. **Crea el equipo** con TeamCreate
4. **Crea las tareas** con TaskCreate siguiendo las waves de CLAUDE.md:

### Wave 1 — Domain (sin dependencias)
- T01: "Validar build.gradle.kts y documentar arquitectura" → architect
- T02: "Refactorizar paquete + build.gradle.kts + application.yml" → backend-dev-domain (blockedBy: [T01])
- T03: "Crear migraciones Flyway V1-V8" → backend-dev-domain (blockedBy: [T02])
- T04: "Crear entidades JPA + enums" → backend-dev-domain (blockedBy: [T03])
- T05: "Crear repositorios Spring Data JPA" → backend-dev-domain (blockedBy: [T04])

### Wave 2 — Security + Auth (después de T05)
- T06: "Implementar JWT + Spring Security config" → backend-dev-auth (blockedBy: [T05])
- T07: "Implementar Google OAuth2 + ConsentService" → backend-dev-auth (blockedBy: [T06])

### Wave 3 — Services (después de T07)
- T08: "Implementar AllergenService + AllergenFilterService" → backend-dev-services (blockedBy: [T07])
- T09: "Implementar RestaurantService + MenuService + DishService" → backend-dev-services (blockedBy: [T07])
- T10: "Implementar GdprService + AuditService + UserAllergenProfileService" → backend-dev-services (blockedBy: [T07])
- T11: "Implementar Dockerfile + compose.yaml" → devops-engineer (blockedBy: [T05])

### Wave 4 — API Layer (después de T08-T10)
- T12: "Implementar GlobalExceptionHandler + shared DTOs + OpenApiConfig" → backend-dev-api (blockedBy: [T08, T09, T10])
- T13: "Implementar AuthController + UserController" → backend-dev-api (blockedBy: [T12])
- T14: "Implementar RestaurantController + AdminController" → backend-dev-api (blockedBy: [T12])
- T15: "Implementar GdprController + todos los DTOs + mappers" → backend-dev-api (blockedBy: [T12])

### Wave 5 — Testing (después de T13-T15)
- T16: "Tests AllergenFilterService (casos semáforo)" → qa-engineer (blockedBy: [T13, T14, T15])
- T17: "Tests Auth + Security (RBAC + JWT + OAuth2)" → qa-engineer (blockedBy: [T13])
- T18: "Tests Admin API + Consumer API + RGPD" → qa-engineer (blockedBy: [T14, T15])

### Wave 6 — Review (después de T16-T18)
- T19: "Code review completo" → code-reviewer (blockedBy: [T16, T17, T18])
- T20: "Security audit (OWASP + RGPD)" → security-reviewer (blockedBy: [T16, T17, T18])

### Wave 7 — DevOps (después de T18)
- T21: "K8s manifests (namespace, postgres, api, ingress Traefik)" → devops-engineer (blockedBy: [T18])
- T22: "GitHub Actions CI/CD pipeline" → devops-engineer (blockedBy: [T21])

### Wave 8 — Docs (después de T19-T22)
- T23: "README.md + docs/deploy-guide.md + CHANGELOG.md" → tech-writer (blockedBy: [T19, T20, T22])

5. **Spawna** los teammates en el orden correcto (Wave 1 primero)
6. **Activa Delegate Mode** (Shift+Tab) para que el lead solo coordine

Usar $ARGUMENTS para instrucciones adicionales.
