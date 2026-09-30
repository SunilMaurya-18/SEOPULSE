#!/bin/sh
# shellcheck shell=busybox
# Encrypted pg_dump to S3-compatible storage with daily/weekly/monthly retention.
# Run on demand with:  docker compose exec backup backup.sh
set -eu
set -o pipefail

# shellcheck source=lib.sh
. /usr/local/bin/lib.sh

: "${BACKUP_AGE_RECIPIENT:?BACKUP_AGE_RECIPIENT is required}"
KEEP_DAILY=${BACKUP_KEEP_DAILY:-7}
KEEP_WEEKLY=${BACKUP_KEEP_WEEKLY:-4}
KEEP_MONTHLY=${BACKUP_KEEP_MONTHLY:-6}

stamp=$(date -u +%Y%m%dT%H%M%SZ)
name="${PGDATABASE}-${stamp}.dump.age"
file="/tmp/${name}"
trap 'rm -f "$file"' EXIT

log "dumping database ${PGDATABASE}"
# Encrypted as it streams: the plaintext dump never touches the disk.
pg_dump --format=custom --no-owner --no-privileges | age --encrypt --recipient "$BACKUP_AGE_RECIPIENT" > "$file"

size=$(wc -c < "$file" | tr -d ' ')
if [ "$size" -lt 1024 ]; then
    log "dump is only ${size} bytes; refusing to upload"
    exit 1
fi

log "uploading ${name} (${size} bytes)"
s3 cp --only-show-errors "$file" "${S3_ROOT}/daily/${name}"

if [ "$(date -u +%u)" = "7" ]; then
    s3 cp --only-show-errors "${S3_ROOT}/daily/${name}" "${S3_ROOT}/weekly/${name}"
fi
if [ "$(date -u +%d)" = "01" ]; then
    s3 cp --only-show-errors "${S3_ROOT}/daily/${name}" "${S3_ROOT}/monthly/${name}"
fi

prune() {
    tier=$1
    keep=$2
    list_tier "$tier" | sort -r | tail -n +"$((keep + 1))" | while read -r old; do
        log "pruning ${tier}/${old}"
        s3 rm --only-show-errors "${S3_ROOT}/${tier}/${old}"
    done
}

prune daily "$KEEP_DAILY"
prune weekly "$KEEP_WEEKLY"
prune monthly "$KEEP_MONTHLY"

if [ -n "${BACKUP_HEARTBEAT_URL:-}" ]; then
    wget -q -O /dev/null -T 10 "$BACKUP_HEARTBEAT_URL" || log "heartbeat ping failed"
fi

log "backup ${name} complete"
