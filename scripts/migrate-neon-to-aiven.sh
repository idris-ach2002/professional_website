#!/usr/bin/env bash
set -Eeuo pipefail

: "${NEON_DATABASE_URL:?Définissez NEON_DATABASE_URL avec une URI PostgreSQL Neon complète.}"
: "${AIVEN_DATABASE_URL:?Définissez AIVEN_DATABASE_URL avec une URI PostgreSQL Aiven complète.}"

for binary in pg_dump pg_restore psql; do
  if ! command -v "$binary" >/dev/null 2>&1; then
    echo "Erreur : $binary est requis (installez postgresql-client)." >&2
    exit 1
  fi
done

BACKUP_DIR="${BACKUP_DIR:-migration-backups}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
DUMP_FILE="$BACKUP_DIR/neon-to-aiven-$TIMESTAMP.dump"
SOURCE_COUNTS="$BACKUP_DIR/source-counts-$TIMESTAMP.tsv"
DEST_COUNTS="$BACKUP_DIR/aiven-counts-$TIMESTAMP.tsv"
ALLOW_NON_EMPTY_AIVEN="${ALLOW_NON_EMPTY_AIVEN:-NO}"
CLEAN_RESTORE="${CLEAN_RESTORE:-NO}"

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

echo "[1/7] Vérification des connexions..."
psql "$NEON_DATABASE_URL" -X -v ON_ERROR_STOP=1 -Atqc "SELECT 'Neon: ' || current_database() || ' / ' || current_user;"
psql "$AIVEN_DATABASE_URL" -X -v ON_ERROR_STOP=1 -Atqc "SELECT 'Aiven: ' || current_database() || ' / ' || current_user;"

echo "[2/7] Vérification de la destination..."
DEST_TABLE_COUNT="$(psql "$AIVEN_DATABASE_URL" -X -v ON_ERROR_STOP=1 -Atqc "SELECT count(*) FROM pg_tables WHERE schemaname = 'public';")"
if [[ "$DEST_TABLE_COUNT" != "0" && "$ALLOW_NON_EMPTY_AIVEN" != "YES" ]]; then
  echo "La destination Aiven contient déjà $DEST_TABLE_COUNT table(s) dans public." >&2
  echo "Utilisez une base vide, ou relancez explicitement avec ALLOW_NON_EMPTY_AIVEN=YES." >&2
  exit 1
fi

echo "[3/7] Sauvegarde Neon vers $DUMP_FILE..."
pg_dump \
  --dbname="$NEON_DATABASE_URL" \
  --format=custom \
  --compress=6 \
  --no-owner \
  --no-privileges \
  --file="$DUMP_FILE"
chmod 600 "$DUMP_FILE"

echo "[4/7] Inventaire des lignes source..."
count_rows() {
  local uri="$1"
  local output="$2"
  psql "$uri" -X -v ON_ERROR_STOP=1 -At -F $'\t' > "$output" <<'SQL'
SELECT format(
  'SELECT %L AS table_name, count(*) AS row_count FROM %I.%I;',
  schemaname || '.' || tablename,
  schemaname,
  tablename
)
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;
\gexec
SQL
  sort -o "$output" "$output"
}
count_rows "$NEON_DATABASE_URL" "$SOURCE_COUNTS"

RESTORE_OPTIONS=(
  --dbname="$AIVEN_DATABASE_URL"
  --exit-on-error
  --no-owner
  --no-privileges
  --no-comments
)
if [[ "$CLEAN_RESTORE" == "YES" ]]; then
  RESTORE_OPTIONS+=(--clean --if-exists)
fi

echo "[5/7] Restauration vers Aiven..."
pg_restore "${RESTORE_OPTIONS[@]}" "$DUMP_FILE"

echo "[6/7] Mise à jour des statistiques PostgreSQL..."
psql "$AIVEN_DATABASE_URL" -X -v ON_ERROR_STOP=1 -c "ANALYZE;"

echo "[7/7] Comparaison des nombres de lignes..."
count_rows "$AIVEN_DATABASE_URL" "$DEST_COUNTS"
if diff -u "$SOURCE_COUNTS" "$DEST_COUNTS"; then
  echo "Migration vérifiée : les nombres de lignes correspondent."
else
  echo "Attention : des différences de lignes ont été détectées." >&2
  echo "Consultez : $SOURCE_COUNTS et $DEST_COUNTS" >&2
  exit 2
fi

echo
echo "Sauvegarde conservée : $DUMP_FILE"
echo "Étape suivante : configurez les variables SPRING_DATASOURCE_* dans Render, puis redéployez."
