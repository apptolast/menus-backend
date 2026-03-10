---
description: Ejecuta todos los quality gates del proyecto Menus Backend
allowed-tools: Read, Bash, Grep
---

Ejecuta todos los quality gates del proyecto Menus Backend y reporta el resultado.

## Verificaciones

### 1. Compilación
```bash
./gradlew build -x test --no-daemon 2>&1 | tail -20
```
✅ Pasa / ❌ Falla — Lista de errores

### 2. Tests
```bash
./gradlew test --no-daemon 2>&1 | tail -30
```
✅ Pasa / ❌ Falla — Tests fallando

### 3. TODOs en producción
```bash
grep -r "TODO\|FIXME\|HACK" src/main/ --include="*.kt" 2>/dev/null
```
✅ 0 encontrados / ❌ N encontrados

### 4. Secrets hardcodeados
```bash
grep -r -i --include="*.kt" --include="*.yml" -E '(secret|password)\s*[:=]\s*"[^$\{][^"]{10,}"' src/ 2>/dev/null | grep -v "test\|//\|CHANGE_ME"
```
✅ 0 encontrados / ❌ N encontrados

### 5. Entidades JPA sin tenantId (riesgo de RLS)
```bash
grep -rL "tenantId\|tenant_id" src/main/kotlin/com/apptolast/menus/*/model/entity/ --include="*.kt" 2>/dev/null
```
✅ Todas tienen tenantId / ⚠️ Entidades sin tenantId (pueden ser tablas de referencia)

### 6. Migraciones Flyway
```bash
ls src/main/resources/db/migration/ 2>/dev/null
```
✅ V1-V8 presentes / ❌ Faltan migraciones

## Formato de Salida
```
# Quality Gate Report — Menus Backend
[fecha]

| Check | Estado | Detalle |
|---|---|---|
| Compilación | ✅/❌ | [mensaje] |
| Tests | ✅/❌ | [N passed / N failed] |
| TODOs | ✅/⚠️ | [N encontrados] |
| Secrets | ✅/❌ | [hallazgos] |
| RLS Entities | ✅/⚠️ | [entidades sin tenantId] |
| Flyway | ✅/❌ | [migraciones presentes] |

## Resultado Final
✅ PASSED — Listo para deploy
❌ FAILED — [N issues críticos] bloqueando
```
