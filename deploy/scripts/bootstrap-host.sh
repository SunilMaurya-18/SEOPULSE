#!/usr/bin/env bash
# One-time setup of a fresh Ubuntu 24.04 VPS. Run as root:
#
#   ADMIN_USER=sunil \
#   ADMIN_SSH_KEY="ssh-ed25519 AAAA... you@laptop" \
#   DEPLOY_SSH_KEY="ssh-ed25519 AAAA... github-actions-deploy" \
#   bash bootstrap-host.sh
#
# Installs Docker, creates an admin (sudo) user and a deploy user, locks SSH to
# keys only with no root login, and enables UFW, fail2ban and unattended
# security upgrades. Safe to re-run.
set -euo pipefail

: "${ADMIN_USER:?ADMIN_USER is required}"
: "${ADMIN_SSH_KEY:?ADMIN_SSH_KEY is required (you would be locked out without it)}"
: "${DEPLOY_SSH_KEY:?DEPLOY_SSH_KEY is required}"
DEPLOY_USER=${DEPLOY_USER:-deploy}
DEPLOY_PATH=${DEPLOY_PATH:-/opt/seopulse}
SSH_PORT=${SSH_PORT:-22}

[[ $EUID -eq 0 ]] || { echo "run as root" >&2; exit 1; }

log() { echo "==> $*"; }

log "Updating packages"
export DEBIAN_FRONTEND=noninteractive
apt-get update -q
apt-get upgrade -yq
apt-get install -yq ca-certificates curl gnupg ufw fail2ban python3-systemd unattended-upgrades

log "Installing Docker Engine and the Compose plugin"
if ! command -v docker > /dev/null; then
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc
    # shellcheck source=/dev/null
    . /etc/os-release
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" \
        > /etc/apt/sources.list.d/docker.list
    apt-get update -q
    apt-get install -yq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

log "Docker log rotation"
install -m 0755 -d /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "5" },
  "live-restore": true
}
EOF
systemctl enable --now docker
systemctl restart docker

add_user_with_key() {
    local user=$1 key=$2
    id "$user" > /dev/null 2>&1 || adduser --disabled-password --gecos "" "$user"
    install -d -m 700 -o "$user" -g "$user" "/home/$user/.ssh"
    printf '%s\n' "$key" > "/home/$user/.ssh/authorized_keys"
    chown "$user:$user" "/home/$user/.ssh/authorized_keys"
    chmod 600 "/home/$user/.ssh/authorized_keys"
}

log "Admin user ${ADMIN_USER} (sudo)"
add_user_with_key "$ADMIN_USER" "$ADMIN_SSH_KEY"
usermod -aG sudo,docker "$ADMIN_USER"
# The account has no password (key-only login), so sudo must not ask for one.
echo "${ADMIN_USER} ALL=(ALL) NOPASSWD:ALL" > "/etc/sudoers.d/90-${ADMIN_USER}"
chmod 440 "/etc/sudoers.d/90-${ADMIN_USER}"
visudo -cf "/etc/sudoers.d/90-${ADMIN_USER}"

# Membership of the docker group is root-equivalent; this key is only for CI deploys.
log "Deploy user ${DEPLOY_USER}"
add_user_with_key "$DEPLOY_USER" "$DEPLOY_SSH_KEY"
usermod -aG docker "$DEPLOY_USER"
install -d -m 750 -o "$DEPLOY_USER" -g "$DEPLOY_USER" "$DEPLOY_PATH"

log "SSH: keys only, no root login"
cat > /etc/ssh/sshd_config.d/99-seopulse.conf <<EOF
Port ${SSH_PORT}
PermitRootLogin no
PasswordAuthentication no
KbdInteractiveAuthentication no
PubkeyAuthentication yes
AllowUsers ${ADMIN_USER} ${DEPLOY_USER}
MaxAuthTries 3
X11Forwarding no
EOF
sshd -t
# Ubuntu 24.04 socket-activates sshd; the socket picks up Port on reload.
systemctl daemon-reload
if systemctl is-enabled ssh.socket > /dev/null 2>&1; then
    systemctl restart ssh.socket
fi
systemctl restart ssh

log "Firewall: only SSH, HTTP and HTTPS"
# Docker publishes ports around UFW; only Caddy publishes any (80/443), and
# the monitoring profile binds to 127.0.0.1 only.
ufw default deny incoming
ufw default allow outgoing
ufw allow "${SSH_PORT}/tcp"
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 443/udp
ufw --force enable

log "fail2ban for SSH"
cat > /etc/fail2ban/jail.d/sshd.local <<EOF
[sshd]
enabled = true
port = ${SSH_PORT}
backend = systemd
maxretry = 5
findtime = 10m
bantime = 1h
EOF
systemctl enable --now fail2ban
systemctl restart fail2ban

log "Unattended security upgrades"
cat > /etc/apt/apt.conf.d/20auto-upgrades <<'EOF'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
APT::Periodic::AutocleanInterval "7";
EOF
systemctl enable --now unattended-upgrades

log "Done. Before closing this session, confirm in a NEW terminal that"
log "  ssh -p ${SSH_PORT} ${ADMIN_USER}@<server>  works and that root login is refused."
