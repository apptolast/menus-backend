#!/bin/bash
# Hook: PostToolUse (Write|Edit) — Auto-format después de cada edición
# Spring Boot / Kotlin / Gradle project

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // .tool_input.filePath // empty' 2>/dev/null || echo "")

if [ -z "$FILE_PATH" ] || [ ! -f "$FILE_PATH" ]; then
  exit 0
fi

EXTENSION="${FILE_PATH##*.}"

case "$EXTENSION" in
  kt|kts)
    # Kotlin formatting con ktlint (si está disponible como gradle task)
    if [ -f "gradlew" ] && grep -q "ktlint\|ktfmt" build.gradle.kts 2>/dev/null; then
      ./gradlew ktlintFormat --quiet 2>/dev/null || true
    fi
    ;;
  yml|yaml)
    # Validar YAML (no formatear — demasiado agresivo para application.yml)
    if command -v python3 &>/dev/null; then
      python3 -c "import yaml; yaml.safe_load(open('$FILE_PATH'))" 2>/dev/null || \
        echo "⚠️  YAML inválido en $FILE_PATH — verifica la sintaxis" >&2
    fi
    ;;
  json)
    # Formatear JSON si es válido
    if command -v jq &>/dev/null; then
      FORMATTED=$(jq '.' "$FILE_PATH" 2>/dev/null) && echo "$FORMATTED" > "$FILE_PATH" || true
    fi
    ;;
  sql)
    # Sin auto-formato para SQL — demasiado riesgo de romper RLS policies
    ;;
esac

exit 0
