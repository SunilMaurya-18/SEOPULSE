#!/bin/sh
# shellcheck shell=busybox
# Runs backup.sh once a day at BACKUP_TIME (UTC). A plain loop rather than
# crond, because busybox crond does not pass the container environment on.
set -eu

# shellcheck source=lib.sh
. /usr/local/bin/lib.sh

run_backup() {
    if backup.sh; then
        rm -f /tmp/backup-failed
    else
        touch /tmp/backup-failed
        log "backup FAILED"
    fi
}

time_of_day=${BACKUP_TIME:-02:30}
hours=${time_of_day%%:*}
minutes=${time_of_day##*:}
# Strip one leading zero so "08" is not read as an invalid octal number.
hours=${hours#0}
minutes=${minutes#0}
target=$((hours * 3600 + minutes * 60))

if [ "${BACKUP_ON_START:-false}" = "true" ]; then
    run_backup
fi

log "scheduled daily at ${time_of_day} UTC"
while true; do
    now=$(( $(date -u +%s) % 86400 ))
    wait=$(( (target - now + 86400) % 86400 ))
    [ "$wait" -eq 0 ] && wait=86400
    sleep "$wait"
    run_backup
done
