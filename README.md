# SEOPulse

SEOPulse is a full-stack website SEO audit platform. Connect websites, run crawls, score on-page SEO health, review ranked issues, and download printable audit reports.

```
Landing → Register / Login → Connect website → Run audit → Crawl / Analyze → Report → Download
```

## Repository structure

```
SEOPULSE/
├── seopulse-frontend/     # React + Vite + TypeScript UI (Dockerfile: nginx image)
├── seopulse-backend/      # Spring Boot API + crawler workers (Dockerfile: one image, two roles)
├── deploy/                # Production Compose stack, Caddy, backups, deploy scripts
├── .github/               # CI, deploy, CodeQL and Dependabot
└── README.md              # This file
```

## Features

### Product workflow
- **Connect** — add websites with name + URL (HTTP/HTTPS validated)
- **Crawl** — queue and run SEO audits with live status (`QUEUED` → `CRAWLING` → `ANALYZING` → `COMPLETED`)
- **Analyze** — on-page checks (title, meta, H1, canonical, links, images, content, HTTP status)
- **Report** — health score, issue severities, page inventory, downloadable HTML/JSON reports

### Frontend experience
- Marketing landing page with quick-start URL handoff into auth
- JWT auth (register / login) with protected app routes
- Workspace auto-created after first login
- Dashboard overview with overall KPIs, website focus switcher, score distribution, leaderboard
- Websites, Audits, Issues, Pages, Settings
- Live crawl telemetry and polling while audits run
- Toast feedback and clear next-step banners
- Download audit reports (HTML printable / JSON export)
- Light / dark theme, fixed sidebar layout, back navigation

### Backend capabilities
- JWT authentication (`/api/v1/auth/register`, `/api/v1/auth/login`)
- Project / website / audit REST APIs
- Async crawl + analysis workers (Redis-backed queue patterns)
- Swagger UI / OpenAPI docs
- PostgreSQL persistence + Flyway migrations (profile-dependent)
- Docker Compose for Postgres + Redis

## Tech stack

| Layer | Stack |
|---|---|
| Frontend | React 19, TypeScript, Vite 8, Tailwind CSS 4, React Router 7, Axios, Lucide |
| Backend | Java, Spring Boot, Spring Security (JWT / OAuth2 resource server), Spring Data JPA, Hibernate |
| Data | PostgreSQL, Redis |
| Docs | springdoc OpenAPI / Swagger UI |
| Tooling | Maven, Docker Compose, Oxlint |

## Prerequisites

- Node.js 20+ (frontend)
- Java 21+ / 25 (backend — see `pom.xml`)
- Maven
- Docker Desktop (recommended for Postgres + Redis)
- Or local PostgreSQL + Redis

## Quick start

### 1) Start infrastructure

```bash
cd seopulse-backend
docker compose up -d
```

This starts:
- PostgreSQL on `localhost:5432`
- Redis on `localhost:6379`

This Compose file is for local development only: it runs just the databases, with fixed dev credentials and ports published to your machine. Production uses `deploy/docker-compose.prod.yml`, which runs the whole stack from images behind Caddy with nothing but ports 80 and 443 exposed (see [Deployment](#deployment)).

Default Compose credentials:

| Setting | Value |
|---|---|
| Database | `seopulse` |
| User | `seopulse` |
| Password | `seopulse_dev_password` |

> If you use local Postgres instead, set `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`, or copy `application-local.yml.example` to `application-local.yml` (git-ignored) in `seopulse-backend/src/main/resources`.

### 2) Start backend API

```bash
cd seopulse-backend
# Ensure JWT_SECRET is set (required)
# PowerShell example:
$env:JWT_SECRET="your-long-dev-secret-at-least-32-chars"

mvn spring-boot:run
```

Backend defaults:
- API base: `http://localhost:8082/api/v1`
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- Health: `http://localhost:8082/api/v1/health` (auth may apply depending on security config)

### 3) Start frontend

```bash
cd seopulse-frontend
npm install
cp .env.example .env   # if needed
npm run dev
```

Frontend defaults:
- App: `http://localhost:5173`
- API: `VITE_API_BASE_URL=http://localhost:8082/api/v1`
- Vite also proxies `/api` → `http://localhost:8082`

## User flow

1. Open `http://localhost:5173`
2. Enter a URL on the landing page **or** go to **Register / Sign in**
3. After auth, SEOPulse creates a default **Workspace** project if needed
4. If a landing URL was provided, onboarding connects the site and starts an audit
5. Use **Dashboard** for overall + per-website views
6. Use **Websites** to add more properties (`name` + `url` required)
7. Use **Audits** to run crawls and open live reports
8. Review **Issues** / **Pages**, then **Download report** (HTML or JSON)

## Frontend routes

| Route | Description |
|---|---|
| `/` | Landing page |
| `/terms` | Terms |
| `/bot` | Public SEOPulseBot crawler information (linked from the user agent) |
| `/login` | Sign in |
| `/register` | Create account |
| `/verify-email?token=` | Confirms an email address from the verification link |
| `/forgot-password` | Request a password reset link |
| `/reset-password?token=` | Choose a new password from the reset link |
| `/dashboard` | Overview / command center |
| `/websites` | Manage websites |
| `/audits` | Run / list audits |
| `/audits/:auditId` | Audit detail + download report |
| `/audits/:auditId/pages` | Crawled page inventory |
| `/audits/:auditId/issues` | SEO issues for an audit |
| `/issues` | Cross-site issues explorer |
| `/pages` | Pages inventory explorer |
| `/settings` | Theme, account, active workspace |

## Main API surface

Base path: `/api/v1`

### Auth
- `POST /auth/register` — `{ name, email, password }` → access token + refresh cookie
- `POST /auth/login` — `{ email, password }` → access token (15 min) + refresh cookie
- `POST /auth/refresh` — rotates the refresh cookie, returns a new access token (needs `X-Requested-With`)
- `POST /auth/logout` — revokes the refresh token and clears the cookie (needs `X-Requested-With`)
- `POST /auth/logout-all` — revokes every session of the signed-in user (JWT required)
- `POST /auth/verify-email` — `{ token }`
- `POST /auth/resend-verification` — JWT required
- `POST /auth/forgot-password` — `{ email }`, always `202`
- `POST /auth/reset-password` — `{ token, newPassword }`

The refresh token is an `HttpOnly; SameSite=Strict` cookie scoped to `/api/v1/auth`; reusing a rotated token revokes the whole session family. Passwords need 10+ characters and are checked against Have I Been Pwned (k-anonymity). Ten failed logins lock the account for 15 minutes. Unverified accounts can sign in but cannot start audits. Errors are RFC 9457 problem responses with a `requestId` that matches the `X-Request-Id` header and the server logs.

### Projects (JWT required)
- `GET /projects`
- `POST /projects`
- `GET /projects/{projectId}`
- `GET /projects/{projectId}/summary`
- `DELETE /projects/{projectId}`

### Websites
- `GET /projects/{projectId}/websites`
- `POST /projects/{projectId}/websites` — `{ name, url }`
- `GET /projects/{projectId}/websites/{websiteId}`

### Audits
- `POST /projects/{projectId}/audits?websiteId={id}`
- `GET /projects/{projectId}/audits?websiteId={id}`
- `GET /projects/{projectId}/audits/{auditId}`
- `GET /projects/{projectId}/audits/{auditId}/summary`
- `GET /projects/{projectId}/audits/{auditId}/pages`
- `GET /projects/{projectId}/audits/{auditId}/issues`
- `POST /projects/{projectId}/audits/{auditId}/cancel`
- `GET /projects/{projectId}/audits/{auditId}/events` — Server-Sent Events (`event: audit`, JSON audit body) until the audit finishes

Rate limits (Redis-backed, `429` with `Retry-After` and `X-RateLimit-*`): login 5/min and 20/hour per IP and per IP + email, register 5/hour per IP, forgot-password 3/hour, audit creation 10/hour per user, everything else 300/min.

Send auth header:

```http
Authorization: Bearer <accessToken>
```

## Configuration

### Frontend (`seopulse-frontend/.env`)

```env
VITE_API_BASE_URL=http://localhost:8082/api/v1
VITE_SENTRY_DSN=            # optional browser error reporting
```

Production builds fail if `VITE_API_BASE_URL` is missing; see `seopulse-frontend/.env.production.example` (also covers `VITE_RELEASE` and Sentry source map upload).

### Backend

Important settings live in:
- `seopulse-backend/src/main/resources/application.yml` (defaults, all overridable by environment variables)
- `seopulse-backend/src/main/resources/application-local.yml` (optional, git-ignored personal overrides; see `application-local.yml.example`)

The database schema is managed only by Flyway (`db/migration`); Hibernate runs with `ddl-auto: validate`. The `prod` profile refuses to start with a weak `JWT_SECRET`, the default `DB_PASSWORD`, missing/non-HTTPS `CORS_ALLOWED_ORIGINS`, a non-HTTPS `SEOPULSE_BOT_INFO_URL`, or `seopulse.crawler.allow-private-networks=true`.

Common values:

| Key | Typical value |
|---|---|
| `server.port` | `8082` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/seopulse` |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` |
| `REDIS_PASSWORD` | empty locally |
| `SPRING_PROFILES_ACTIVE` | `dev` locally (API + worker in one process); `prod` for the API and `prod,worker` for worker containers |
| `JWT_SECRET` | long random secret, at least 32 bytes (required) |
| `SEOPULSE_AUTH_ACCESS_TOKEN_TTL` / `SEOPULSE_AUTH_REFRESH_TOKEN_TTL` | `15m` / `30d` |
| `SEOPULSE_AUTH_REFRESH_COOKIE_SECURE` | `true` (must stay `true` in prod; `false` in `dev`) |
| `SEOPULSE_AUTH_REQUIRE_EMAIL_VERIFICATION` | `true`; set `false` for smoke tests |
| `SEOPULSE_AUTH_BREACHED_PASSWORD_CHECK` | `true` (HIBP range API, fails open) |
| `SEOPULSE_APP_BASE_URL` | frontend origin used in email links (`https://` in prod) |
| `SEOPULSE_RATE_LIMIT_ENABLED` | `true` |
| `SEOPULSE_WORKER_CONCURRENCY` | audits processed in parallel per worker (`2`) |
| `SENTRY_DSN` / `SENTRY_ENVIRONMENT` / `SENTRY_RELEASE` | optional error reporting |
| `MANAGEMENT_PORT` | `8081` in prod: health and `/actuator/prometheus`, internal only |
| `CORS_ALLOWED_ORIGINS` | comma-separated origins; defaults to `localhost:5173/5174` in `dev` only |
| `SEOPULSE_BOT_INFO_URL` | public URL of the frontend `/bot` page, embedded in the crawler user agent (`https://` in prod) |

Emails (verification, password reset, lockout) go through a logging `EmailSender` until Phase 4; the `dev` profile prints the links to the console.

Crawler limits and politeness (`seopulse.crawler.*` in `application.yml`): `max-pages`, `max-depth`, `concurrency`, `min-delay-ms`, `max-crawl-delay-ms`, `max-retries`, `max-duration-minutes`, `max-body-size-bytes`, `allowed-ports`, `respect-robots-txt` and `robots-cache-ttl-hours`.

> Do not commit real production secrets. Prefer environment variables over hardcoding credentials.

## Scripts

### Frontend

```bash
npm run dev            # Vite dev server (port 5173)
npm run build          # Typecheck + production build
npm run preview        # Preview production build
npm run lint           # Oxlint
npm run typecheck      # tsc -b
npm test               # Vitest + Testing Library + MSW
npm run test:coverage  # Vitest with V8 coverage
npm run test:e2e       # Playwright smoke test (needs API + worker running, see below)
```

The Playwright smoke test registers, adds a website, runs an audit to completion and downloads the report. Run the backend with `SEOPULSE_AUTH_REQUIRE_EMAIL_VERIFICATION=false`; set `E2E_BASE_URL` to test a deployed frontend and `E2E_TARGET_URL` to change the audited site (it must be public). Install the browser once with `npx playwright install chromium`.

### Backend

```bash
mvn spring-boot:run
mvn verify     # tests + JaCoCo gate (60% lines on seo, website.service, crawler, auth); needs Docker for Testcontainers
mvn package
```

## Deployment

Production is a single VPS running Docker Compose; the full runbook is in [`deploy/README.md`](deploy/README.md) and restores are in [`deploy/RESTORE.md`](deploy/RESTORE.md).

- **Images:** `seopulse-backend` (runs as the API with `prod`, or as a worker with `prod,worker`), `seopulse-frontend` (nginx serving the build, calling the API at the relative `/api/v1`), and `seopulse-backup` (nightly encrypted `pg_dump` to S3-compatible storage).
- **Stack:** Caddy terminates TLS (automatic Let's Encrypt) and routes `/api/*` to the API; Postgres and Redis sit on an internal network. Only the API runs Flyway migrations.
- **CI/CD:** pull requests run `.github/workflows/ci.yml` (backend `mvn verify`, frontend lint/typecheck/test/build, then the production stack with the Playwright smoke test and a backup/restore drill). Pushing to `main` builds, Trivy-scans and pushes images to GHCR and deploys to staging; a `v*` tag deploys to production after approval. `deploy/scripts/deploy.sh` waits for health checks and rolls back to the previous tag on failure.

To run the production stack on your machine (served on `https://localhost:18443` with a local certificate):

```bash
docker compose -f deploy/docker-compose.prod.yml -f deploy/docker-compose.ci.yml --env-file deploy/ci.env build
export BACKUP_AGE_RECIPIENT=$(docker run --rm --entrypoint age-keygen seopulse-ci/seopulse-backup:ci 2>/dev/null | grep 'public key' | awk '{print $4}')
docker compose -f deploy/docker-compose.prod.yml -f deploy/docker-compose.ci.yml --env-file deploy/ci.env up -d --wait
cd seopulse-frontend && E2E_BASE_URL=https://localhost:18443 E2E_IGNORE_HTTPS_ERRORS=1 npx playwright test
```

## Architecture notes

- Frontend talks to backend over REST with Axios. The access token lives only in memory; on load the app calls `/auth/refresh` with the HttpOnly cookie, and a 401 triggers one shared refresh while other requests wait, then retries them.
- Server state uses TanStack Query (`src/api/queries/`). The audit detail page follows live progress over Server-Sent Events and falls back to polling if the stream drops. Routes are lazy-loaded behind error boundaries.
- Browser errors go to Sentry when `VITE_SENTRY_DSN` is set, tagged with the API's `X-Request-Id` so they can be matched to backend log lines.
- The backend worker (`prod,worker` profile) consumes the Redis Stream with a unique consumer name, a bounded pool and blocking `XREADGROUP`. It enforces a 30-minute audit budget, honours user cancellation, re-queues on retryable failures, reclaims stale pending jobs and reaps stuck audits. Outbox publishing uses `FOR UPDATE SKIP LOCKED`, so several workers can run.
- Production logs are JSON with `requestId`, `userId` and `auditId` in the MDC; Prometheus metrics (`audits_*`, `crawl_pages_total`, `outbox_lag_seconds`, `redis_stream_pending`) are on the management port.
- Protected UI routes wrap the app shell (`Sidebar` + `Topbar`) and auto-resolve the active project.
- Dashboard keeps **overall workspace** metrics and a **website focus** mode for per-site inspection.
- Audit report downloads are generated client-side from audit summary + pages + issues (HTML/JSON).
- The backend crawler (`website/crawler`) uses Jetty `HttpClient` with a validating resolver. Every connection's resolved IPs are checked against private, reserved and embedded-IPv4 ranges, which also defeats DNS rebinding. Only ports 80 and 443 are allowed.
- The crawler honours robots.txt (RFC 9309, cached in Redis for 24h) and seeds from sitemaps. It follows redirects hop by hop, treating `example.com` and `www.example.com` as one site, and spaces requests per host across concurrent virtual-thread workers. It backs off on 429/503 using `Retry-After`, and stops at a page and time budget.
- Redirects, robots-blocked URLs, oversized responses and fetch failures are stored as page records (`REDIRECT`, `SKIPPED_ROBOTS`, `TOO_LARGE`, `FAILED`). Only `CRAWLED` pages are analyzed and scored.

## Troubleshooting

| Problem | Likely cause | Fix |
|---|---|---|
| Dashboard “Workspace unavailable” | Wrong API port / backend down / missing JWT | Confirm API on `8082`, sign in again |
| API 401 everywhere | Calling wrong process on `8080` (e.g. Oracle TNS) or no token | Use `8082`, complete login |
| Cannot add website | Missing `name` field or invalid URL | Send both `name` and `url`; use `https://example.com` |
| Hostname could not be resolved / restricted | URL validator blocked host | Use a public resolvable domain |
| Frontend not picking env changes | Vite needs restart | Restart `npm run dev` |
| Signed out after every reload | Refresh cookie not stored: `Secure` cookie over plain http, or API on a different site | Run the `dev` profile (non-secure cookie) and keep frontend and API on the same site (e.g. both `localhost`) |
| "Verify your email" when starting an audit | Email not verified | Use the link printed in the backend console (`dev`), or set `SEOPULSE_AUTH_REQUIRE_EMAIL_VERIFICATION=false` locally |
| `429 Too Many Requests` | Rate limit hit | Wait for `Retry-After`, or set `SEOPULSE_RATE_LIMIT_ENABLED=false` locally |
| `http://localhost:5173` suddenly redirects to https | You opened the local production stack (`https://localhost:18443`) in your own browser, and its HSTS header now applies to `localhost` | Clear it at `chrome://net-internals/#hsts` (delete domain `localhost`); only open the local stack from Playwright |
| Caddy keeps restarting in production | Bad `Caddyfile` or a required variable such as `ACME_EMAIL` missing | `docker compose logs caddy`; validate with `docker run --rm -e APP_DOMAIN=example.com -e ACME_EMAIL=ops@example.com -v ./Caddyfile:/etc/caddy/Caddyfile caddy:2 caddy validate --config /etc/caddy/Caddyfile` |

## License / status

Under active development.
