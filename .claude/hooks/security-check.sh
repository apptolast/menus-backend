#!/bin/bash
# Hook: PreToolUse (Write|Edit) — Bloquea escritura de secrets y archivos sensibles
# Recibe JSON en stdin con tool_input

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // .tool_input.filePath // empty' 2>/dev/null || echo "")
CONTENT=$(echo "$INPUT" | jq -r '.tool_input.content // .tool_input.new_string // empty' 2>/dev/null || echo "")

# 1. Bloquear escritura en archivos sensibles
BLOCKED_PATTERNS=(".env" "secrets/" ".key" ".pem" "id_rsa" "credentials" "*.keystore" "*.jks")
for pattern in "${BLOCKED_PATTERNS[@]}"; do
  if [[ "$FILE_PATH" == *"$pattern"* ]]; then
    echo "🔒 Escritura bloqueada: no se permite modificar archivos sensibles ($FILE_PATH)" >&2
    echo "   Usa variables de entorno y Kubernetes Secrets en su lugar." >&2
    exit 2
  fi
done

# 2. Detectar secrets hardcodeados en el contenido (para .kt y .yml)
if [[ "$FILE_PATH" == *.kt || "$FILE_PATH" == *.yml || "$FILE_PATH" == *.yaml || "$FILE_PATH" == *.properties ]]; then
  # Patrón: secret/password/token = "valor_real" (no variables, no placeholders)
  if echo "$CONTENT" | grep -qiE '(secret|password|jwt[_-]?secret|client[_-]?secret)\s*[:=]\s*"[^$\{][^"]{10,}"' 2>/dev/null; then
    # Excepciones: líneas de test, comentarios, placeholders conocidos
    ACTUAL_SECRETS=$(echo "$CONTENT" | grep -iE '(secret|password|jwt[_-]?secret|client[_-]?secret)\s*[:=]\s*"[^$\{][^"]{10,}"' | grep -v "test\|//\|#\|CHANGE_ME\|example\|placeholder\|dev-secret" || true)
    if [ -n "$ACTUAL_SECRETS" ]; then
      echo "🔒 Posible secret hardcodeado detectado. Usa \${ENV_VAR} o Kubernetes Secrets." >&2
      echo "$ACTUAL_SECRETS" | head -3 >&2
      exit 2
    fi
  fi
fi

# 3. Prevenir sobrescritura de K8s secrets con valores reales
if [[ "$FILE_PATH" == *"k8s/secret"* || "$FILE_PATH" == *"k8s/secrets"* ]]; then
  if echo "$CONTENT" | grep -qE 'stringData:|^\s+[a-zA-Z_]+:\s+[A-Za-z0-9+/=]{10,}' 2>/dev/null; then
    echo "🔒 No incluyas valores reales en k8s/secrets.yaml." >&2
    echo "   Usa: kubectl create secret generic menus-secrets --from-literal=KEY=VALUE" >&2
    exit 2
  fi
fi

exit 0
