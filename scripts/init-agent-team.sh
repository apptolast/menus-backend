#!/bin/bash
set -euo pipefail

# ============================================================================
# MENUS BACKEND — Agent Team Initializer
# Uso: ./scripts/init-agent-team.sh
# Prerrequisito: Claude Code instalado (npm install -g @anthropic-ai/claude-code)
# ============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SPEC_FILE="$PROJECT_DIR/docs/technical-spec.md"
CLAUDE_DIR="$PROJECT_DIR/.claude"

print_header() {
  echo -e "\n${BLUE}╔══════════════════════════════════════════════════╗${NC}"
  echo -e "${BLUE}║     🍽️  Menus Backend — Agent Team Launcher      ║${NC}"
  echo -e "${BLUE}╚══════════════════════════════════════════════════╝${NC}\n"
}

print_step() {
  echo -e "${YELLOW}[$(printf '%02d' $1)/07] $2${NC}"
}

print_ok() {
  echo -e "  ${GREEN}✅ $1${NC}"
}

print_warn() {
  echo -e "  ${YELLOW}⚠️  $1${NC}"
}

print_err() {
  echo -e "  ${RED}❌ $1${NC}"
}

print_header

# ─── 1. Verificar prerrequisitos ─────────────────────────────────────────────
print_step 1 "Verificando prerrequisitos..."

if ! command -v claude &>/dev/null; then
  print_err "Claude Code no instalado."
  echo -e "  Instala con: ${CYAN}npm install -g @anthropic-ai/claude-code${NC}"
  exit 1
fi
print_ok "Claude Code $(claude --version 2>/dev/null || echo 'disponible')"

if ! command -v java &>/dev/null; then
  print_warn "Java no encontrado en PATH. El agente backend-dev-domain lo necesitará."
else
  print_ok "Java $(java -version 2>&1 | head -1 | awk -F '"' '{print $2}')"
fi

if ! command -v docker &>/dev/null; then
  print_warn "Docker no encontrado. El devops-engineer lo necesitará."
else
  print_ok "Docker disponible"
fi

# ─── 2. Verificar estructura del proyecto ────────────────────────────────────
print_step 2 "Verificando estructura del proyecto..."

if [ ! -f "$PROJECT_DIR/build.gradle.kts" ]; then
  print_err "No se encontró build.gradle.kts en $PROJECT_DIR"
  exit 1
fi
print_ok "Proyecto Gradle encontrado"

if [ ! -f "$SPEC_FILE" ]; then
  print_err "Documento técnico no encontrado: $SPEC_FILE"
  echo -e "  Crea docs/technical-spec.md con el spec técnico del proyecto."
  exit 1
fi
print_ok "Technical spec encontrado: $SPEC_FILE"

if [ ! -f "$CLAUDE_DIR/settings.json" ]; then
  print_err ".claude/settings.json no encontrado"
  exit 1
fi
print_ok "settings.json con AGENT_TEAMS habilitado"

# ─── 3. Verificar que AGENT_TEAMS está habilitado ────────────────────────────
print_step 3 "Verificando configuración de Agent Teams..."

if ! grep -q "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS" "$CLAUDE_DIR/settings.json" 2>/dev/null; then
  print_err "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS no está en settings.json"
  exit 1
fi
print_ok "Agent Teams habilitado en settings.json"

# ─── 4. Hacer hooks ejecutables ──────────────────────────────────────────────
print_step 4 "Configurando permisos de hooks..."

for hook in "$CLAUDE_DIR/hooks/"*.sh; do
  if [ -f "$hook" ]; then
    chmod +x "$hook"
    print_ok "chmod +x $(basename "$hook")"
  fi
done

for script in "$PROJECT_DIR/scripts/"*.sh; do
  if [ -f "$script" ]; then
    chmod +x "$script"
  fi
done

# ─── 5. Crear .env si no existe ──────────────────────────────────────────────
print_step 5 "Verificando variables de entorno..."

if [ ! -f "$PROJECT_DIR/.env" ] && [ -f "$PROJECT_DIR/.env.example" ]; then
  print_warn ".env no encontrado. Recuerda crearlo desde .env.example:"
  echo -e "    ${CYAN}cp .env.example .env && nano .env${NC}"
elif [ -f "$PROJECT_DIR/.env" ]; then
  print_ok ".env encontrado"
fi

# ─── 6. Verificar git ────────────────────────────────────────────────────────
print_step 6 "Verificando repositorio git..."

if [ ! -d "$PROJECT_DIR/.git" ]; then
  print_warn "Inicializando repositorio git..."
  git -C "$PROJECT_DIR" init
fi

# Añadir entradas a .gitignore
GITIGNORE="$PROJECT_DIR/.gitignore"
ENTRIES=("CLAUDE.local.md" ".claude/settings.local.json" ".env" ".env.*" "!.env.example" "docs/security-audit-report.md")
for entry in "${ENTRIES[@]}"; do
  if ! grep -qF "$entry" "$GITIGNORE" 2>/dev/null; then
    echo "$entry" >> "$GITIGNORE"
  fi
done
print_ok "Git configurado con .gitignore actualizado"

# ─── 7. Lanzar Claude Code con prompt maestro ────────────────────────────────
print_step 7 "Lanzando Team Lead de Claude Code..."

echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🚀 Iniciando el equipo agéntico Menus Backend...${NC}\n"
echo -e "${CYAN}Controles en Claude Code:${NC}"
echo -e "  ${CYAN}Shift+Tab${NC}    → Activar Delegate Mode (solo coordinación)"
echo -e "  ${CYAN}Shift+Up/Down${NC}→ Navegar entre teammates"
echo -e "  ${CYAN}Ctrl+T${NC}       → Ver/ocultar task list"
echo -e "  ${CYAN}/project:status-report${NC} → Ver estado del equipo"
echo -e "  ${CYAN}/project:quality-gate${NC}  → Ejecutar quality gates"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

SYSTEM_PROMPT="
Eres el Team Lead del proyecto Menus Backend de AppToLast.
Tu rol es EXCLUSIVAMENTE coordinar — NO implementes código directamente.
Activa Delegate Mode inmediatamente presionando Shift+Tab.

PROYECTO: Backend API REST de gestión de alérgenos para restauración.
Stack: Spring Boot 4.0 + Kotlin 2.2 + PostgreSQL 16 + Kubernetes (Rancher/Traefik)
Paquete base: com.apptolast.menus
Confluence: https://apptolast.atlassian.net/wiki/spaces/SD/pages/190709761/Menus

PRIMERA ACCIÓN — ejecuta /project:init-team para:
1. Crear el agent team con TeamCreate
2. Definir las 23 tareas con sus dependencias (waves 1-8)
3. Spawnar los teammates en el orden correcto:
   - Wave 1: architect + backend-dev-domain
   - Wave 2: backend-dev-auth (tras domain)
   - Wave 3: backend-dev-services + devops-engineer (en paralelo, tras auth)
   - Wave 4: backend-dev-api (tras services)
   - Wave 5: qa-engineer (tras api)
   - Wave 6: code-reviewer + security-reviewer (en paralelo, tras testing)
   - Wave 7: devops-engineer continúa con K8s + CI/CD (tras testing)
   - Wave 8: tech-writer (última wave)

REGLA DE ORO: Cada teammate recibe en su spawn prompt:
- Su rol específico y ownership de archivos
- El WORKER PREAMBLE (NO spawnes otros agentes)
- Resumen del contexto técnico del proyecto
- Criterios de aceptación de sus tareas

RECURSOS:
- @CLAUDE.md — Convenciones completas del proyecto
- @docs/technical-spec.md — Spec técnico completo
- .claude/agents/ — Definiciones de cada agente
- .claude/commands/init-team.md — Prompt detallado para inicializar
"

cd "$PROJECT_DIR"
claude --append-system-prompt "$SYSTEM_PROMPT"
