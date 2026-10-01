# Same major version as the database, so pg_dump/pg_restore always match.
FROM postgres:18-alpine

# gosu is only used by the Postgres server entrypoint, which this image never
# runs; it drags in its own Go runtime and CVEs.
RUN apk upgrade --no-cache \
    && apk add --no-cache aws-cli age tini \
    && rm -f /usr/local/bin/gosu

# Path-style addressing and on-demand checksums keep the AWS CLI compatible
# with S3-compatible stores (Backblaze B2, Cloudflare R2, MinIO).
COPY aws-config /etc/aws/config
ENV AWS_CONFIG_FILE=/etc/aws/config \
    AWS_REQUEST_CHECKSUM_CALCULATION=when_required \
    AWS_RESPONSE_CHECKSUM_VALIDATION=when_required

COPY --chmod=755 entrypoint.sh backup.sh restore.sh lib.sh /usr/local/bin/

USER postgres
WORKDIR /tmp

HEALTHCHECK --interval=5m --timeout=10s --start-period=30s --start-interval=5s \
    CMD test ! -f /tmp/backup-failed || exit 1

ENTRYPOINT ["/sbin/tini", "-g", "--", "/usr/local/bin/entrypoint.sh"]
