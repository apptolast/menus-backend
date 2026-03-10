#!/bin/bash
# Hook: TaskCompleted — Quality gate antes de marcar tarea como completa
# Spring Boot / Kotlin / Gradle project
# Exit code 2 = previene completion y envía feedback al agente

set -e

INPUT=$(cat)
TASK_ID=$(echo "$INPUT" | jq -r '.task_id // empty' 2>/dev/null || echo "")
CWD=$(echo "$INPUT" | jq -r '.cwd // "."' 2>/dev/null || echo ".")

cd "$CWD"

echo "🔍 Ejecutando quality gate para tarea: ${TASK_ID:-unknown}"

# 1. Verificar que el proyecto Gradle compila sin errores
if [ -f "gradlew" ]; then
  echo "📦 Verificando compilación (./gradlew build -x test)..."
  if ! ./gradlew build -x test --no-daemon --quiet 2>&1; then
    echo "❌ Error de compilación detectado. Corrige los errores antes de completar la tarea." >&2
    exit 2
  fi
  echo "✅ Compilación exitosa"
else
  echo "⚠️  No se encontró gradlew — saltando verificación de compilación"
fi

# 2. Verificar que no hay TODO/FIXME en el código
TODO_COUNT=$(grep -r "TODO\|FIXME\|HACK\|XXX" src/main/ --include="*.kt" 2>/dev/null | wc -l | tr -d ' ')
if [ "$TODO_COUNT" -gt "0" ]; then
  echo "⚠️  Se encontraron $TODO_COUNT TODO/FIXME en src/main/. Reemplaza con implementación real." >&2
  grep -r "TODO\|FIXME\|HACK\|XXX" src/main/ --include="*.kt" 2>/dev/null | head -10 >&2
  exit 2
fi

# 3. Verificar que no hay secrets hardcodeados (pattern básico)
SECRET_PATTERN='(password|secret|token|api_key)\s*=\s*"[^$\{][^"]{8,}"'
if grep -r -i --include="*.kt" --include="*.yml" --include="*.properties" -E "$SECRET_PATTERN" src/ 2>/dev/null | grep -v "test\|//\|#\|@Value\|placeholder\|example\|CHANGE_ME" | grep -q .; then
  echo "🔒 Posible secret hardcodeado detectado. Usa variables de entorno." >&2
  grep -r -i --include="*.kt" --include="*.yml" -E "$SECRET_PATTERN" src/ 2>/dev/null | grep -v "test\|//\|#\|@Value\|placeholder" | head -5 >&2
  exit 2
fi

echo "✅ Quality gate passed — tarea lista para completar"
exit 0
