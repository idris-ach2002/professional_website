#!/usr/bin/env bash
set -Eeuo pipefail

: "${AIVEN_DATABASE_URL:?Définissez AIVEN_DATABASE_URL avec une URI PostgreSQL Aiven complète.}"

if ! command -v psql >/dev/null 2>&1; then
  echo "Erreur : psql est requis (paquet postgresql-client)." >&2
  exit 1
fi

echo "Vérification TLS et connexion Aiven..."
psql "$AIVEN_DATABASE_URL" -X -v ON_ERROR_STOP=1 <<'SQL'
SELECT current_database() AS database,
       current_user AS database_user,
       version() AS server_version;
SELECT ssl, version, cipher
FROM pg_stat_ssl
WHERE pid = pg_backend_pid();
SHOW max_connections;
SQL
