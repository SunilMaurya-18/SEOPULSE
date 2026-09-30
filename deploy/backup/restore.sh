#!/bin/sh
# shellcheck shell=busybox
# Downloads, decrypts and restores a backup. See "Backups and restore" in the root README.md.
#
#   restore.sh list
#   restore.sh <daily|weekly|monthly>/<file>|latest <target-database>
#
# Needs the age private key at $BACKUP_AGE_IDENTITY (default /run/secrets/age-identity).
set -eu
set -o pipefail

# shellcheck source=lib.sh
. /usr/local/bin/lib.sh

if [ "${1:-}" = "list" ]; then
    for tier in daily weekly monthly; do
        list_tier "$tier" | sed "s|^|${tier}/|"
    done
    exit 0
fi

source=${1:?usage: restore.sh list | restore.sh <tier>/<file>|latest <target-database>}
target=${2:?usage: restore.sh <tier>/<file>|latest <target-database>}
identity=${BACKUP_AGE_IDENTITY:-/run/secrets/age-identity}

if ! echo "$target" | grep -Eq '^[a-z_][a-z0-9_]{0,62}$'; then
    log "invalid database name: ${target}"
    exit 1
fi

if [ ! -r "$identity" ]; then
    log "age identity not readable at ${identity}"
    exit 1
fi

if [ "$source" = "latest" ]; then
    latest=$(list_tier daily | tail -n 1)
    [ -n "$latest" ] || { log "no daily backups found"; exit 1; }
    source="daily/${latest}"
fi

encrypted="/tmp/restore.dump.age"
trap 'rm -f "$encrypted"' EXIT

log "downloading ${source}"
s3 cp --only-show-errors "${S3_ROOT}/${source}" "$encrypted"

if ! psql -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '${target}'" | grep -q 1; then
    log "creating database ${target}"
    createdb "$target"
fi

log "restoring into ${target}"
age --decrypt --identity "$identity" "$encrypted" \
    | pg_restore --dbname="$target" --clean --if-exists --no-owner --no-privileges --exit-on-error

log "restore of ${source} into ${target} complete"
