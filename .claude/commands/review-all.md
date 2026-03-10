---
description: Lanza code review + security audit en paralelo para Menus Backend
allowed-tools: Read, Bash
---

Lanza los agentes de code review y security audit en paralelo sobre el código implementado.

## Instrucciones

1. Verifica que la implementación está completa (Waves 1-5 completadas)
2. Spawna los dos reviewers en paralelo:
   - `code-reviewer`: ejecutar checklist completo Kotlin + Spring Boot
   - `security-reviewer`: ejecutar checklist OWASP + RGPD
3. Espera sus reportes en docs/code-review-report.md y docs/security-audit-report.md
4. Sintetiza los findings críticos (🔴) en un resumen ejecutivo
5. Crea tareas de fix para los findings Must Fix

## Criterios para Aprobar
- 0 findings 🔴 CRÍTICOS
- Todos los checks RGPD en verde
- RLS multi-tenancy verificado

Usar $ARGUMENTS para enfocar el review en módulos específicos.
