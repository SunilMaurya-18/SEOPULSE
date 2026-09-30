# SEOPulse Backend

Spring Boot API and crawl/analysis workers for the SEOPulse website SEO audit platform.

## Stack

- Java (see `pom.xml` for version)
- Spring Boot
- Spring Security + JWT (OAuth2 resource server)
- Spring Data JPA / Hibernate
- PostgreSQL
- Redis
- Flyway (profile-dependent)
- springdoc OpenAPI / Swagger UI
- Maven
- Docker Compose (Postgres + Redis)

## Features

- User register / login with JWT
- Project (workspace) management
- Website registration with URL validation
- Audit creation and status lifecycle
- Website crawling and SEO analysis
- Audit summary, pages, and issues APIs
- Swagger documentation

## Quick start

### 1) Infrastructure

```bash
docker compose up -d
```

Services:
- PostgreSQL → `localhost:5432`
- Redis → `localhost:6379`

Compose defaults:

```text
DB: seopulse
USER: seopulse
PASSWORD: seopulse_dev_password
```

### 2) Secrets

Set a JWT secret before running:

```bash
# PowerShell
$env:JWT_SECRET="your-long-dev-secret-at-least-32-chars"
```

Or provide it via `.env` / environment injection used by your run configuration.

### 3) Run API

```bash
mvn spring-boot:run
```

Defaults:
- Server port: **8082** (`application.properties`)
- API base: `http://localhost:8082/api/v1`
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`

## Configuration

Primary config files:
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application.properties` (local port / datasource overrides)

Common keys:

| Key | Description |
|---|---|
| `server.port` | API port (`8082`) |
| `DB_URL` / datasource | PostgreSQL JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | DB credentials |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis connection |
| `SPRING_PROFILES_ACTIVE` | `dev` (API + worker in one process), `prod` (API only) or `prod,worker` (worker container) |
| `JWT_SECRET` | HMAC signing secret (required) |
| `SEOPULSE_AUTH_ACCESS_TOKEN_TTL` / `SEOPULSE_AUTH_REFRESH_TOKEN_TTL` | Access token (`15m`) and refresh cookie (`30d`) lifetimes |
| `SEOPULSE_AUTH_REFRESH_COOKIE_SECURE` | `Secure` flag on the refresh cookie (`true`; `false` in `dev`) |
| `SEOPULSE_AUTH_REQUIRE_EMAIL_VERIFICATION` | Block audits for unverified accounts (`false`, currently disabled) |
| `SEOPULSE_AUTH_BREACHED_PASSWORD_CHECK` | Check new passwords against HIBP (`true`) |
| `SEOPULSE_APP_BASE_URL` | Frontend origin used in email links |
| `SEOPULSE_RATE_LIMIT_ENABLED` | Redis-backed rate limiting (`true`) |
| `SEOPULSE_WORKER_CONCURRENCY` | Audits processed in parallel per worker (`2`) |
| `SENTRY_DSN` | Optional error reporting |
| `MANAGEMENT_PORT` | Internal Actuator port in `prod` (`8081`): health groups and `/actuator/prometheus` |

> Prefer environment variables for secrets. Avoid committing production credentials.

## Auth

Public endpoints:
- `POST /api/v1/auth/register`, `POST /api/v1/auth/login` — return a 15-minute access token and set the refresh cookie
- `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout` — use the `HttpOnly; SameSite=Strict` refresh cookie (path `/api/v1/auth`) and require an `X-Requested-With` header
- `POST /api/v1/auth/verify-email`, `POST /api/v1/auth/forgot-password` (always `202`), `POST /api/v1/auth/reset-password`

Authenticated: `POST /api/v1/auth/logout-all`, `POST /api/v1/auth/resend-verification`.

Refresh tokens rotate on every use and are stored as SHA-256 hashes; presenting an already-rotated token revokes the whole family. Passwords need 10+ characters and are checked against Have I Been Pwned. Ten failed logins lock the account for 15 minutes and send an email. Emails go through a logging `EmailSender` for now (links are printed in `dev`).

Errors are RFC 9457 problem responses (`application/problem+json`) with `requestId` and `timestamp`; the same ID is returned as `X-Request-Id`. Requests are rate-limited through Bucket4j on Redis, with `429`, `Retry-After` and `X-RateLimit-*` headers.

Protected endpoints require:

```http
Authorization: Bearer <accessToken>
```

## Core API groups

### Projects
- `GET/POST /api/v1/projects`
- `GET /api/v1/projects/{projectId}`
- `GET /api/v1/projects/{projectId}/summary`
- `DELETE /api/v1/projects/{projectId}`

### Websites
- `GET/POST /api/v1/projects/{projectId}/websites`
- `GET /api/v1/projects/{projectId}/websites/{websiteId}`

Create website body:

```json
{
  "name": "Example",
  "url": "https://example.com"
}
```

### Audits
- `POST /api/v1/projects/{projectId}/audits?websiteId={websiteId}`
- `GET /api/v1/projects/{projectId}/audits?websiteId={websiteId}`
- `GET /api/v1/projects/{projectId}/audits/{auditId}`
- `GET /api/v1/projects/{projectId}/audits/{auditId}/summary`
- `GET /api/v1/projects/{projectId}/audits/{auditId}/pages`
- `GET /api/v1/projects/{projectId}/audits/{auditId}/issues`
- `POST /api/v1/projects/{projectId}/audits/{auditId}/cancel`
- `GET /api/v1/projects/{projectId}/audits/{auditId}/events` — Server-Sent Events (`event: audit`) fed by Redis pub/sub from the worker

## Audit lifecycle

```text
QUEUED → CRAWLING → ANALYZING → COMPLETED
   ↘         ↘           ↘
    CANCELLED / FAILED (timeout, retries exhausted, invalid target)
```

The worker runs under the `worker` profile (included in `dev`). Each instance registers a unique consumer name (`HOSTNAME-uuid`) and reads the Redis Stream with blocking `XREADGROUP` on a bounded pool (`seopulse.worker.concurrency`). Audits are claimed atomically and have a 30-minute budget. The worker checks for cancellation every 2 s and re-queues retryable failures up to `max-retries`. It also reclaims stale pending jobs, reaps audits stuck in an active state, and removes idle consumers. Outbox publishing uses `FOR UPDATE SKIP LOCKED`, so several workers can run.

URL validation allows only public HTTP/HTTPS hosts and blocks private/loopback/link-local targets.

## Build & test

```bash
mvn verify    # unit + Testcontainers integration tests, JaCoCo report in target/site/jacoco
mvn package
```

`mvn verify` fails below 60% line coverage on `website.seo`, `website.service`, `website.crawler` and `auth`. Integration tests need Docker.

## Docker image

```bash
docker build -t seopulse-backend .
```

A multi-stage build (Maven with a cached dependency layer, then Spring Boot's layered jar extraction) on `eclipse-temurin:25-jre`, running as a non-root user with `-XX:MaxRAMPercentage=75`. The image defaults to the `prod` profile, serves the API on 8082 and Actuator on 8081, and its `HEALTHCHECK` calls `/actuator/health/liveness`. The same image is the worker when started with `SPRING_PROFILES_ACTIVE=prod,worker` (set `SPRING_FLYWAY_ENABLED=false` there so only the API migrates). See [`deploy/`](../deploy/README.md) for the production stack.

## Observability

- `prod` logs are JSON (logstash encoder) with `requestId`, plus `userId` and `auditId` inside the worker.
- Actuator on the management port: `health` (with `liveness` and `readiness` groups; readiness checks the DB and Redis), `info`, `prometheus`.
- Metrics: `audits_started_total`, `audits_completed_total{status}`, `audit_duration_seconds`, `crawl_pages_total{outcome}`, `outbox_lag_seconds`, `redis_stream_pending`.
- Sentry via `SENTRY_DSN`, with personal data collection off.

## Frontend pairing

The React app expects this API on port **8082**.

See the root [README.md](../README.md) and [frontend README](../seopulse-frontend/README.md).

## Status

Under active development.
