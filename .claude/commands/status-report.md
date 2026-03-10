---
description: Genera un reporte de estado del equipo Menus Backend
allowed-tools: Read, Bash, Grep
---

Genera un reporte completo del estado actual del proyecto Menus Backend.

## Pasos

1. Lista todas las tareas con TaskList y su estado (pending/in_progress/completed)
2. Ejecuta verificaciones rápidas:
   ```bash
   ./gradlew build -x test --quiet 2>&1 | tail -5
   ```
3. Cuenta los archivos implementados por módulo:
   ```bash
   ls src/main/kotlin/com/apptolast/menus/ 2>/dev/null
   ```
4. Busca TODOs pendientes:
   ```bash
   grep -r "TODO\|FIXME" src/main/ --include="*.kt" 2>/dev/null | wc -l
   ```

## Formato del Reporte

```
# Estado del Proyecto Menus Backend
Fecha: [fecha]

## Tareas
| ID | Descripción | Estado | Agente |
|---|---|---|---|
[lista de tareas]

## Progreso por Wave
- Wave 1 (Domain): X/5 tareas completadas
- Wave 2 (Auth): X/2 tareas completadas
- Wave 3 (Services): X/4 tareas completadas
- Wave 4 (API): X/4 tareas completadas
- Wave 5 (Testing): X/3 tareas completadas
- Wave 6 (Review): X/2 tareas completadas
- Wave 7 (DevOps): X/2 tareas completadas
- Wave 8 (Docs): X/1 tareas completadas

## Estado del Build
[output de gradlew build]

## TODOs Pendientes: [N]

## Progreso General: [X]%

## Blockers Activos
[lista de bloqueos conocidos]
```
