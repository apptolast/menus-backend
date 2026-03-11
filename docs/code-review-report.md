## Code Review Report
Fecha: 2026-03-10
Branch: `feature/ws4-services` (PR #2) + sub-PR `copilot/sub-pr-2` (PR #8)
Archivos revisados: 53 changed files (+1,479 lines, -110 lines)
Archivos unicos del PR (no en develop): 45
Archivos con conflictos potenciales (overlapping con develop): 49 files shared, but only 5 have actual content changes (entity modifications)

---

### 🔴 Must Fix (bloquea merge)

#### CR-001: EncryptionConfig usa AES/ECB (inseguro) — PR #2
- Archivo: `src/main/kotlin/com/apptolast/menus/config/EncryptionConfig.kt`
- Problema: La rama `feature/ws4-services` usa `AES/ECB/PKCS5Padding`, que es criptograficamente inseguro. ECB no proporciona confidencialidad semantica — bloques identicos producen ciphertext identico, lo cual permite ataques de patron. Esto cifra emails de usuarios (datos personales RGPD).
- Fix: El sub-PR #8 (`copilot/sub-pr-2`) ya corrige esto usando `AES/GCM/NoPadding` con IV aleatorio de 12 bytes. **El sub-PR #8 DEBE mergearse antes o junto con PR #2.**

#### CR-002: GoogleTokenVerifier no valida la firma del token — PR #2
- Archivo: `src/main/kotlin/com/apptolast/menus/auth/service/GoogleTokenVerifier.kt`
- Problema: El metodo `verify()` usa `GoogleIdToken.parse()` que solo parsea el JWT pero NO verifica la firma criptografica contra las claves publicas de Google. Un atacante puede fabricar un token arbitrario con cualquier email/googleId.
- Fix: El sub-PR #8 corrige esto usando `GoogleIdTokenVerifier.Builder(...).setAudience(...).build().verify()` que verifica firma + audience. **Critico para seguridad.**

#### CR-003: TenantConfig no ejecuta `SET app.current_tenant` en PostgreSQL — PR #2
- Archivo: `src/main/kotlin/com/apptolast/menus/config/TenantConfig.kt`
- Problema: El `TenantFilter` solo gestiona un `ThreadLocal` pero nunca ejecuta `SET app.current_tenant = '<uuid>'` en la conexion de base de datos. Esto significa que las politicas RLS de PostgreSQL (V8 migration) NO se aplican. Todos los datos de todos los tenants son accesibles para cualquier usuario autenticado.
- Fix: El sub-PR #8 anade `TenantTransactionConfig.kt` con un `TenantAwareJpaTransactionManager` que ejecuta `set_config('app.current_tenant', ?, true)` al inicio de cada transaccion. **Ambos archivos son necesarios para que RLS funcione.**

#### CR-004: AllergenAuditLog — entity degradada de enums tipados a Strings sin razon
- Archivo: `src/main/kotlin/com/apptolast/menus/audit/model/entity/AllergenAuditLog.kt`
- Problema: PR #2 cambia `action: AuditAction` (enum) a `action: String` y `oldLevel/newLevel: ContainmentLevel?` a `String?`. Esto elimina type safety en tiempo de compilacion. Un typo como `"UPDAT"` en vez de `"UPDATE"` no sera detectado. El enum `AuditAction` y la referencia a `ContainmentLevel` del dominio existente (en develop) eran correctos.
- Fix: Restaurar los enums tipados. Si `ContainmentLevel` estaba en el archivo equivocado de audit, importarlo desde `com.apptolast.menus.dish.model.enum.ContainmentLevel`. Crear un enum `AuditAction` en `audit/model/enum/`:
```kotlin
@Column(name = "action", nullable = false, length = 20)
@Enumerated(EnumType.STRING)
val action: AuditAction = AuditAction.ADD,

@Enumerated(EnumType.STRING)
@Column(name = "old_level", length = 20)
val oldLevel: ContainmentLevel? = null,
```

#### CR-005: DishAllergenRequest.containmentLevel es String sin validacion
- Archivo: `src/main/kotlin/com/apptolast/menus/dish/dto/request/DishRequest.kt:18-20`
- Problema: `DishAllergenRequest.containmentLevel` es `String` sin `@field:NotBlank` y sin validacion de que sea un valor valido del enum `ContainmentLevel`. En `DishServiceImpl.addAllergen()` se hace `ContainmentLevel.valueOf(request.containmentLevel)` que lanza `IllegalArgumentException` no controlada si el valor es invalido. El `GlobalExceptionHandler` no captura `IllegalArgumentException`, asi que devolvera un 500 generico.
- Fix: Anadir `@field:NotBlank` a `allergenCode` y `containmentLevel`. Ademas, o bien (a) usar el enum `ContainmentLevel` directamente en el DTO, o bien (b) capturar `IllegalArgumentException` en el handler o validar en el servicio con una excepcion de negocio.

#### CR-006: AuthServiceImpl.login() usa ResourceNotFoundException para credenciales invalidas
- Archivo: `src/main/kotlin/com/apptolast/menus/auth/service/impl/AuthServiceImpl.kt:50-55`
- Problema: Cuando el password no coincide, se lanza `ResourceNotFoundException("INVALID_CREDENTIALS", ...)` que devuelve HTTP 404. Segun el spec, login fallido debe devolver 401. Igualmente, cuando el usuario no existe, devuelve 404 — lo cual permite enumeracion de emails (un atacante puede distinguir "email no existe" de "password incorrecto").
- Fix: Usar una excepcion con HTTP 401 para ambos casos (usuario no encontrado y password incorrecto). No hay `UnauthorizedException` definida — hay que crearla o usar `ForbiddenException` con codigo 401 personalizado:
```kotlin
class UnauthorizedException(
    errorCode: String = "UNAUTHORIZED",
    message: String
) : BusinessException(errorCode, message, 401)
```

---

### 🟡 Should Fix (recomendado)

#### CR-007: N+1 en DishServiceImpl.findByRestaurant() — sin JOIN FETCH
- Archivo: `src/main/kotlin/com/apptolast/menus/dish/service/impl/DishServiceImpl.kt:41`
- Problema: `findByRestaurant()` usa `dishRepository.findBySectionMenuRestaurantId()` (query derivada sin JOIN FETCH) y luego accede a `dish.allergens` y `allergen.translations` en `toResponse()`. Esto causa N+1+1 queries: 1 para dishes, N para allergens de cada dish, y N para translations de cada allergen.
- Fix: Crear un `@Query` con `JOIN FETCH` similar a `findWithAllergensBySectionId`:
```kotlin
@Query("SELECT DISTINCT d FROM Dish d LEFT JOIN FETCH d.allergens da LEFT JOIN FETCH da.allergen a LEFT JOIN FETCH a.translations WHERE d.section.menu.restaurantId = :restaurantId")
fun findByRestaurantWithAllergens(restaurantId: UUID): List<Dish>
```

#### CR-008: N+1 en MenuServiceImpl.findByRestaurant() — sections cargadas lazy
- Archivo: `src/main/kotlin/com/apptolast/menus/menu/service/impl/MenuServiceImpl.kt:31-36`
- Problema: `findByRestaurant()` carga menus con queries derivadas y luego accede a `menu.sections` en `toResponse()`. Las sections son `FetchType.LAZY`, causando N+1.
- Fix: Anadir `@Query("SELECT DISTINCT m FROM Menu m LEFT JOIN FETCH m.sections WHERE m.restaurantId = :restaurantId AND m.isArchived = false ORDER BY m.displayOrder")` al repositorio.

#### CR-009: N+1 en DishServiceImpl.toResponse() — allergen.translations
- Archivo: `src/main/kotlin/com/apptolast/menus/dish/service/impl/DishServiceImpl.kt:145`
- Problema: Dentro de `toResponse()`, se accede a `da.allergen.translations.firstOrNull { t -> t.locale == "es" }` para cada allergen. Aunque `findWithAllergensBySectionId` hace JOIN FETCH de `allergens` y `allergen`, NO hace JOIN FETCH de `allergen.translations`. Esto causa un query adicional por cada allergen distinto.
- Fix: Extender el JPQL query para incluir translations:
```kotlin
@Query("SELECT d FROM Dish d LEFT JOIN FETCH d.allergens da LEFT JOIN FETCH da.allergen a LEFT JOIN FETCH a.translations WHERE d.section.id = :sectionId AND d.isAvailable = true")
```

#### CR-010: StringArrayConverter simplificado no maneja escape de comillas ni comas en valores
- Archivo: `src/main/kotlin/com/apptolast/menus/consumer/model/entity/StringArrayConverter.kt`
- Problema: La simplificacion de `convertToDatabaseColumn` y `convertToEntityAttribute` rompe el manejo de valores que contengan comas o comillas dobles. Por ejemplo, un allergen code hipotetico `"A,B"` se parsearia como dos elementos. La implementacion original en develop manejaba esto correctamente con escape de caracteres.
- Fix: Restaurar el parser robusto de la version en develop, o al menos documentar que los valores nunca contendran comas/comillas (lo cual es cierto para allergen codes como `GLUTEN`, `EGGS`, etc., pero es fragil ante futuros cambios en `severity_notes` si se usara el mismo converter).

#### CR-011: Restaurant entity — id cambiado de `var` a `val` con default `UUID.randomUUID()`
- Archivo: `src/main/kotlin/com/apptolast/menus/restaurant/model/entity/Restaurant.kt`
- Problema: El cambio de `var id: UUID? = null` a `val id: UUID = UUID.randomUUID()` con `@GeneratedValue(strategy = GenerationType.UUID)` es correcto conceptualmente (spec dice UUIDs como PK). Sin embargo, `tenantId` y `ownerId` tambien cambiaron a defaults de `UUID.randomUUID()` — esto significa que si alguien construye un `Restaurant()` sin argumentos, tendra valores aleatorios silenciosos en campos criticos como `ownerId` en lugar de fallar con un error.
- Fix: Considerar hacer `ownerId` y `tenantId` parametros obligatorios sin default, o al menos no usar `UUID.randomUUID()` como default para campos que deben ser provistos explicitamente.

#### CR-012: GdprServiceImpl.deleteUserData() no es atomico con la anonimizacion
- Archivo: `src/main/kotlin/com/apptolast/menus/gdpr/service/impl/GdprServiceImpl.kt:42-58`
- Problema: Se crea un nuevo `UserAccount` con `id = user.id` y se hace `userAccountRepository.save(anonymized)`. Esto ejecuta un INSERT en lugar de un UPDATE porque JPA ve un objeto nuevo (no managed). Si la transaccion falla parcialmente despues de borrar el perfil pero antes del save, se pierden datos.
- Fix: Modificar directamente el objeto `user` existente (que ya esta managed) en lugar de crear uno nuevo:
```kotlin
user.email = anonymizedHash.toByteArray()
user.emailHash = anonymizedHash
user.passwordHash = null
user.isActive = false
// No need to call save — dirty checking will flush
```

#### CR-013: ConsentService no verifica consentimiento existente antes de crear duplicado
- Archivo: `src/main/kotlin/com/apptolast/menus/auth/service/ConsentService.kt:25-31`
- Problema: `grantConsent()` siempre crea un nuevo `ConsentRecord` sin verificar si ya existe uno activo. Multiples llamadas crean multiples registros activos. Aunque `hasActiveConsent()` funciona correctamente, tener multiples registros activos complica la auditoria RGPD.
- Fix: Verificar si ya existe consentimiento activo antes de crear uno nuevo, o revocar el anterior antes de crear el nuevo.

#### CR-014: Falta `@Transactional(readOnly = true)` en ConsentService.hasActiveConsent()
- Archivo: `src/main/kotlin/com/apptolast/menus/auth/service/ConsentService.kt:22-24`
- Problema: `hasActiveConsent()` es un metodo de solo lectura pero no tiene `@Transactional(readOnly = true)`. La clase tiene `@Transactional` a nivel de clase (read-write), asi que este metodo abre una transaccion de escritura innecesariamente.
- Fix: Anadir `@Transactional(readOnly = true)` al metodo.

#### CR-015: DishServiceImpl.create() tiene N+1 al recargar plato con findById
- Archivo: `src/main/kotlin/com/apptolast/menus/dish/service/impl/DishServiceImpl.kt:55-65`
- Problema: Despues de guardar el dish y sus allergens, se hace `dishRepository.findById(dish.id).get()` para recargar. Ese `.get()` puede lanzar `NoSuchElementException` (no controlada) si algo falla, y ademas el `findById` no hace JOIN FETCH, causando queries adicionales en `toResponse()`.
- Fix: Usar un query dedicado con JOIN FETCH para la recarga, o devolver el plato ya construido sin recarga.

#### CR-016: Falta excepcion para aceptacion de terminos en RegisterRequest
- Archivo: `src/main/kotlin/com/apptolast/menus/auth/service/impl/AuthServiceImpl.kt:28-38`
- Problema: `RegisterRequest` tiene campo `acceptTerms: Boolean = false` pero `AuthServiceImpl.register()` nunca verifica que sea `true`. Segun spec, el registro requiere aceptacion de terminos (RGPD compliance).
- Fix: Validar `acceptTerms` en el servicio o con una custom validator:
```kotlin
if (!request.acceptTerms) throw ForbiddenException("TERMS_NOT_ACCEPTED", "You must accept terms and conditions")
```

---

### 💡 Suggestions (opcional)

#### CR-017: AllergenProfileRequest carece de validaciones
- Archivo: `src/main/kotlin/com/apptolast/menus/consumer/dto/request/AllergenProfileRequest.kt`
- Problema: No hay `@field:` validation annotations. `allergenCodes` acepta cualquier string, incluyendo codigos invalidos. Deberia validar que los codigos sean parte de los 14 alergenos EU.
- Sugerencia: Anadir una custom validator o validar en el servicio contra `AllergenCode` enum.

#### CR-018: Extension functions para mappers estan dentro de los ServiceImpl — deberian estar en archivos separados
- Archivo: Varios `*ServiceImpl.kt`
- Problema: Las extension functions `Dish.toResponse()`, `Menu.toResponse()`, `Restaurant.toResponse()` estan definidas como metodos privados dentro de los ServiceImpl. El spec indica que deberian estar en archivos de extension functions separados (`mapper/` package) para ser reutilizables.
- Sugerencia: Mover a `mapper/EntityExtensions.kt` por modulo, e.g., `dish/mapper/DishMappers.kt`.

#### CR-019: AuditService devuelve entidades JPA directamente
- Archivo: `src/main/kotlin/com/apptolast/menus/audit/service/AuditService.kt:7`
- Problema: `fun findByDish(dishId: UUID): List<AllergenAuditLog>` devuelve entidades JPA. Cuando un controller lo use, expondra entidades directamente, violando la regla "No se exponen entidades JPA desde controllers".
- Sugerencia: Crear `AuditLogResponse` DTO y mapear en el servicio.

#### CR-020: GdprService.exportUserData() hardcodea el email decryption fallback
- Archivo: `src/main/kotlin/com/apptolast/menus/gdpr/service/impl/GdprServiceImpl.kt:27`
- Problema: `runCatching { encryptionConfig.decryptEmail(user.email) }.getOrElse { "[encrypted]" }` silencia errores de decifrado. Si la clave de cifrado cambia, el export RGPD devolvera "[encrypted]" sin que nadie lo detecte.
- Sugerencia: Loguear el error y devolver un mensaje mas informativo, o fallar con una excepcion explicita.

#### CR-021: Falta HEALTHCHECK en Dockerfile
- Archivo: `Dockerfile`
- Problema: El spec requiere `HEALTHCHECK` con `wget` a `/actuator/health`. El Dockerfile actual no lo tiene.
- Sugerencia: Anadir:
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:8080/actuator/health || exit 1
```

#### CR-022: AppConfig no tiene google.clientId en PR #2 (solo en sub-PR #8)
- Archivo: `src/main/kotlin/com/apptolast/menus/config/AppConfig.kt`
- Problema: PR #2 no incluye `GoogleProperties` en `AppConfig`, pero `GoogleTokenVerifier` en sub-PR #8 lo necesita. Si se mergea PR #2 sin sub-PR #8, la configuracion de Google OAuth2 no esta tipada.

#### CR-023: DishServiceImpl.delete() no hace cascade delete de dish_allergens ni audit log entries
- Archivo: `src/main/kotlin/com/apptolast/menus/dish/service/impl/DishServiceImpl.kt:74-78`
- Problema: `dishRepository.deleteById(id)` confia en cascade de DB. Sin embargo, `allergen_audit_log` NO tiene FK a dish (por diseno RGPD), asi que los audit logs quedan huerfanos. Esto puede ser aceptable segun requisitos de retencion, pero deberia documentarse.

---

### Resumen del Sub-PR #8 (copilot/sub-pr-2)

El sub-PR #8 corrige 3 problemas criticos de seguridad del PR #2:
1. **EncryptionConfig**: ECB -> GCM (CR-001)
2. **GoogleTokenVerifier**: Parse -> Verify con audience (CR-002)
3. **TenantConfig + TenantTransactionConfig**: RLS realmente aplicado (CR-003)

Ademas anade mejoras menores en DishServiceImpl, MenuServiceImpl, y repositories.

**Recomendacion**: El sub-PR #8 DEBE mergearse en `feature/ws4-services` ANTES de mergear PR #2 a develop.

---

### Checklist de Conformidad

| Check | Estado | Notas |
|---|---|---|
| No `!!` en produccion | PASS | No se encontro ningun uso |
| No `var` donde podria ser `val` en DTOs | PASS | Todos los DTOs usan `val` |
| Extension functions para mappers | PARTIAL | Existen pero como metodos privados en ServiceImpl, no en archivos separados (CR-018) |
| `@field:` prefix en constraint annotations | PASS | Todos los DTOs usan `@field:` correctamente |
| `FetchType.LAZY` en todas las asociaciones | PASS | Todas las relaciones son LAZY |
| Data classes para DTOs | PASS | Todos los DTOs son data classes |
| `@Transactional` solo en service layer | PASS | No hay @Transactional en controllers ni repos |
| `@Transactional(readOnly = true)` en lecturas | PARTIAL | `RestaurantServiceImpl` correcto a nivel de clase. `ConsentService.hasActiveConsent()` falta (CR-014) |
| No se exponen entidades JPA desde controllers | FAIL | `AuditService` devuelve entidades (CR-019) |
| Inyeccion por constructor | PASS | No hay `@Autowired` en campos |
| Interface + Impl pattern | PASS | Todos los servicios siguen el patron |
| No hay imports cruzados indebidos entre modules | ACCEPTABLE | `dish` importa de `menu.repository` y `allergen.repository` (relaciones de dominio legitimas segun architecture.md) |
| No hay logica de negocio en controllers | PASS | (No hay controllers en este PR, solo services) |
| No hay N+1 queries | FAIL | Multiples N+1 detectados (CR-007, CR-008, CR-009) |
| AllergenFilterService semaforo correcto | PASS | Logica correcta: DANGER > RISK > SAFE, FREE_OF filtrado |

---

### Veredicto

**NO READY para merge a develop** en su estado actual. Se requiere:
1. Mergear sub-PR #8 en `feature/ws4-services` (critico: seguridad)
2. Resolver CR-004 (entity degradation), CR-005 (validation gap), CR-006 (auth 401 vs 404)
3. Resolver al menos CR-007/CR-008/CR-009 (N+1 queries) antes de produccion

Los 🟡 Should Fix pueden abordarse en un PR de seguimiento, pero los 🔴 Must Fix bloquean la integracion.
