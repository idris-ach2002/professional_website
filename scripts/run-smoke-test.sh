#!/usr/bin/env bash

set -euo pipefail

# =========================
# Configuration
# =========================

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
API_TOKEN="${API_TOKEN:-}"
RUN_PROD="${RUN_PROD:-false}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SMOKE_TEST_FILE="$SCRIPT_DIR/smoke-test-website-version.js"

# =========================
# Vérifications
# =========================

if ! command -v node >/dev/null 2>&1; then
  echo "Erreur : Node.js n'est pas installé ou n'est pas accessible dans le PATH."
  exit 1
fi

if [ ! -f "$SMOKE_TEST_FILE" ]; then
  echo "Erreur : fichier introuvable : $SMOKE_TEST_FILE"
  exit 1
fi

if [[ "$API_BASE_URL" == *"prod"* || "$API_BASE_URL" == *"https://"* ]]; then
  if [ "$RUN_PROD" != "true" ]; then
    echo "Sécurité : URL potentiellement production détectée."
    echo "Pour lancer quand même : RUN_PROD=true API_BASE_URL=$API_BASE_URL ./scripts/run-smoke-test.sh"
    exit 1
  fi
fi

# =========================
# Lancement
# =========================

echo "Lancement du smoke test"
echo "API_BASE_URL=$API_BASE_URL"
echo "RUN_PROD=$RUN_PROD"

export API_BASE_URL
export API_TOKEN
export RUN_PROD

node "$SMOKE_TEST_FILE"