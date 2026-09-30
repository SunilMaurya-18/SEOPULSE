#!/usr/bin/env bash
# Deploys an image tag on the server and rolls back if it does not come up healthy.
#
#   ./deploy.sh <image-tag>      deploy a tag (normally the commit SHA)
#   ./deploy.sh --rollback       redeploy the previously deployed tag
#
# The server keeps the repo's deploy/ layout (default /opt/seopulse):
# docker-compose.prod.yml, Caddyfile, monitoring/, scripts/deploy.sh and .env.
set -euo pipefail

cd "$(dirname "$0")/.."

COMPOSE=(docker compose -f docker-compose.prod.yml --env-file .env)
CURRENT_FILE=.deployed-tag
PREVIOUS_FILE=.previous-tag
DEFAULT_WAIT_TIMEOUT=300
WAIT_TIMEOUT=${DEPLOY_WAIT_TIMEOUT:-$DEFAULT_WAIT_TIMEOUT}

log() { echo "$(date -u +%H:%M:%S) [deploy] $*"; }

[[ -f .env ]] || { log ".env is missing"; exit 1; }
APP_DOMAIN=$(grep -E '^APP_DOMAIN=' .env | tail -n 1 | cut -d= -f2-)

current=$(cat "$CURRENT_FILE" 2>/dev/null || true)

if [[ "${1:-}" == "--rollback" ]]; then
    target=$(cat "$PREVIOUS_FILE" 2>/dev/null || true)
    [[ -n "$target" ]] || { log "no previous tag recorded"; exit 1; }
else
    target=${1:?usage: deploy.sh <image-tag> | --rollback}
fi

[[ "$target" =~ ^[A-Za-z0-9._-]+$ ]] || { log "invalid tag: $target"; exit 1; }

# Checks the public path end to end (Caddy -> API), not just container health.
smoke_test() {
    local _
    for _ in $(seq 1 20); do
        if curl -fsS --max-time 5 --resolve "${APP_DOMAIN}:443:127.0.0.1" \
                "https://${APP_DOMAIN}/api/v1/health" > /dev/null \
            && curl -fsS --max-time 5 --resolve "${APP_DOMAIN}:443:127.0.0.1" \
                "https://${APP_DOMAIN}/" > /dev/null; then
            return 0
        fi
        sleep 3
    done
    return 1
}

up() {
    local tag=$1 timeout=$2
    log "deploying ${tag}"
    IMAGE_TAG="$tag" "${COMPOSE[@]}" pull --quiet \
        && IMAGE_TAG="$tag" "${COMPOSE[@]}" up -d --remove-orphans --wait --wait-timeout "$timeout" \
        && smoke_test
}

# --wait blocks until every service with a healthcheck reports healthy.
if up "$target" "$WAIT_TIMEOUT"; then
    if [[ -n "$current" && "$current" != "$target" ]]; then
        echo "$current" > "$PREVIOUS_FILE"
    fi
    echo "$target" > "$CURRENT_FILE"
    docker image prune -f --filter "until=168h" > /dev/null || true
    log "deployed ${target}"
    exit 0
fi

log "deploy of ${target} FAILED"
IMAGE_TAG="$target" "${COMPOSE[@]}" ps || true
IMAGE_TAG="$target" "${COMPOSE[@]}" logs --tail 80 api worker || true

if [[ -n "$current" && "$current" != "$target" ]]; then
    log "rolling back to ${current}"
    if up "$current" "$DEFAULT_WAIT_TIMEOUT"; then
        log "rolled back to ${current}"
    else
        log "ROLLBACK FAILED; manual intervention needed"
    fi
fi
exit 1
