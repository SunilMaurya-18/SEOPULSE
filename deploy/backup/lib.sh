#!/bin/sh
# shellcheck shell=busybox
# Shared helpers for backup.sh and restore.sh.

: "${S3_BUCKET:?S3_BUCKET is required}"
S3_PREFIX=${S3_PREFIX:-seopulse}
S3_ROOT="s3://${S3_BUCKET}/${S3_PREFIX}"

log() {
    echo "$(date -u +%Y-%m-%dT%H:%M:%SZ) [backup] $*"
}

# aws CLI against AWS or any S3-compatible endpoint.
s3() {
    if [ -n "${S3_ENDPOINT:-}" ]; then
        aws --endpoint-url "$S3_ENDPOINT" s3 "$@"
    else
        aws s3 "$@"
    fi
}

# Object names in a tier (daily|weekly|monthly), oldest first. Names embed a
# UTC timestamp, so lexical order is chronological.
list_tier() {
    s3 ls "${S3_ROOT}/$1/" | awk '{print $4}' | grep '\.dump\.age$' | sort || true
}
