# Restoring SEOPulse from a backup

There are two layers of backups:

1. **Nightly database dumps.** At `BACKUP_TIME` (default 02:30 UTC), the `backup` container runs `pg_dump -Fc`, encrypts the output with [age](https://age-encryption.org) and uploads it to S3-compatible storage under `<prefix>/daily/`. On Sundays the dump is also copied to `weekly/`, and on the 1st of the month to `monthly/`. The container keeps the newest 7 daily, 4 weekly and 6 monthly copies.
2. **Provider snapshots** of the whole server (enable them in the VPS provider's console). Use these when the host itself is lost; they are coarser (daily or weekly) and include Redis and the Caddy certificates.

Redis only holds the audit queue and rate-limit counters, so it is not backed up. After a restore, audits that were in progress are marked failed by the stuck-audit reaper and can be re-run.

## Backup encryption key

Dumps are encrypted to a public key; the server never holds the private key, so a compromised server or bucket cannot read old backups.

Create the key pair once, on your own machine:

```bash
age-keygen -o seopulse-backup.key
# Public key: age1...
```

- Put the `age1...` public key in `BACKUP_AGE_RECIPIENT` in the server's `.env`.
- Store `seopulse-backup.key` in your password manager (and a second offline copy). **Without it no backup can be restored.**
- Use a bucket-scoped access key for `BACKUP_S3_ACCESS_KEY_ID`. If the provider supports it, turn on object lock or versioning so a compromised server cannot delete old backups.

## Listing backups

```bash
cd /opt/seopulse
export IMAGE_TAG=$(cat .deployed-tag)
docker compose -f docker-compose.prod.yml --env-file .env exec backup restore.sh list
```

## Restoring into a scratch database (safe, no downtime)

Use this to inspect old data or to rehearse. It leaves the live database untouched.

```bash
cd /opt/seopulse
export IMAGE_TAG=$(cat .deployed-tag)
dc="docker compose -f docker-compose.prod.yml --env-file .env"

# 1. Copy the private key to the server for the duration of the restore.
scp seopulse-backup.key sunil@<server>:/tmp/age-identity     # from your machine
chmod 644 /tmp/age-identity                                  # readable by the container's postgres user

# 2. Restore the newest daily backup (or e.g. monthly/seopulse-20260901T023000Z.dump.age).
$dc run --rm --no-deps -v /tmp/age-identity:/run/secrets/age-identity:ro \
  --entrypoint restore.sh backup latest seopulse_restore

# 3. Inspect.
$dc exec postgres psql -U seopulse -d seopulse_restore -c 'SELECT count(*) FROM users'

# 4. Clean up.
shred -u /tmp/age-identity
$dc exec postgres dropdb -U seopulse seopulse_restore
```

## Restoring the live database

This replaces all current data with the backup. Everything written since that backup is lost.

1. Announce downtime, then stop everything that writes to the database:

   ```bash
   $dc stop api worker
   ```

2. Take a safety dump of the current state first, in case you need something from it:

   ```bash
   $dc exec backup backup.sh
   ```

3. Copy the private key to `/tmp/age-identity` as above. Drop the live database, so that tables from migrations newer than the backup cannot survive and confuse Flyway, then restore (`restore.sh` recreates the database):

   ```bash
   $dc exec postgres dropdb -U seopulse --force seopulse
   $dc run --rm --no-deps -v /tmp/age-identity:/run/secrets/age-identity:ro \
     --entrypoint restore.sh backup latest seopulse
   shred -u /tmp/age-identity
   ```

4. Start the application again. Flyway checks the schema history on startup and applies any migrations newer than the backup:

   ```bash
   $dc up -d --wait
   curl -fsS https://<domain>/api/v1/health
   ```

5. Everyone stays signed in only if their refresh token existed at backup time; others simply sign in again.

## Rebuilding on a new server

If the server is gone and no provider snapshot is usable:

1. Create a new server and run the setup in [README.md](README.md#one-time-server-setup), reusing the old `.env` (keep a copy in your password manager).
2. Point DNS at the new server.
3. Deploy the last good SHA: re-run the latest successful `Deploy` workflow, or on the server run `./scripts/deploy.sh <sha>` after `docker login ghcr.io`.
4. Restore the live database as above.

Target: back online in under an hour, losing at most one day of data.

## Monthly restore drill

Run this on the first working day of each month and record the result (date, backup used, duration, row counts) in your ops log. It proves the backups, the key and the procedure all still work.

1. On the **staging** server, add the production bucket's credentials to a separate file, `/opt/seopulse/restore-drill.env`, containing `BACKUP_S3_BUCKET`, `BACKUP_S3_PREFIX`, `BACKUP_S3_ENDPOINT`, `BACKUP_S3_REGION`, `BACKUP_S3_ACCESS_KEY_ID` and `BACKUP_S3_SECRET_ACCESS_KEY`. Prefer a read-only key.
2. Copy the private key to `/tmp/age-identity` (see the scratch restore above). Stop staging's writers, drop staging's database and restore last night's production backup into it:

   ```bash
   cd /opt/seopulse
   export IMAGE_TAG=$(cat .deployed-tag)
   dc="docker compose -f docker-compose.prod.yml --env-file .env --env-file restore-drill.env"
   $dc stop api worker
   $dc exec postgres dropdb -U seopulse --force seopulse
   $dc run --rm --no-deps -v /tmp/age-identity:/run/secrets/age-identity:ro \
     --entrypoint restore.sh backup latest seopulse
   shred -u /tmp/age-identity
   ```

   The second `--env-file` overrides only the bucket settings, so the restore still targets staging's own Postgres. If staging runs a newer build than production, Flyway applies the newer migrations when the API starts in the next step, which also rehearses the next production release.

3. Boot the app against it and check it end to end:

   ```bash
   docker compose -f docker-compose.prod.yml --env-file .env up -d --wait
   curl -fsS https://staging.<domain>/api/v1/health
   ```

   Sign in to staging with a production account, open a recent audit, and compare `SELECT count(*) FROM users` and `FROM audits` with production.

4. Because staging now holds a copy of production data, restrict access to it until the next deploy, or restore staging's own backup afterwards. Remove `restore-drill.env` when done.

CI runs a smaller version of this drill on every pull request (`ci.yml`, job *E2E smoke + backup drill*): it backs up the database the smoke test just wrote to, restores it into a new database from a MinIO bucket, and checks the row counts.
