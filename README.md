# SEOPulse

SEOPulse is a full-stack website SEO audit platform. Connect websites, run crawls, score on-page SEO health, review ranked issues, and download printable audit reports.

```
Landing → Register / Login → Connect website → Run audit → Crawl / Analyze → Report → Download
```

## Repository structure

```
SEOPULSE/
├── seopulse-frontend/     # React + Vite + TypeScript UI
├── seopulse-backend/      # Spring Boot API + crawler workers
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

### Auth (public)
- `POST /auth/register` — `{ name, email, password }`
- `POST /auth/login` — `{ email, password }` → JWT access token

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

Send auth header:

```http
Authorization: Bearer <accessToken>
```

## Configuration

### Frontend (`seopulse-frontend/.env`)

```env
VITE_API_BASE_URL=http://localhost:8082/api/v1
```

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
| `JWT_SECRET` | long random secret, at least 32 bytes (required) |
| `JWT_EXPIRATION` | `3600000` (ms) |
| `CORS_ALLOWED_ORIGINS` | comma-separated origins; defaults to `localhost:5173/5174` in `dev` only |
| `SEOPULSE_BOT_INFO_URL` | public URL of the frontend `/bot` page, embedded in the crawler user agent (`https://` in prod) |

Crawler limits and politeness (`seopulse.crawler.*` in `application.yml`): `max-pages`, `max-depth`, `concurrency`, `min-delay-ms`, `max-crawl-delay-ms`, `max-retries`, `max-duration-minutes`, `max-body-size-bytes`, `allowed-ports`, `respect-robots-txt` and `robots-cache-ttl-hours`.

> Do not commit real production secrets. Prefer environment variables over hardcoding credentials.

## Scripts

### Frontend

```bash
npm run dev       # Vite dev server (port 5173)
npm run build     # Typecheck + production build
npm run preview   # Preview production build
npm run lint      # Oxlint
```

### Backend

```bash
mvn spring-boot:run
mvn test       # integration tests use Testcontainers and are skipped if Docker is not running
mvn package
```

## Architecture notes

- Frontend talks to backend over REST with Axios; JWT stored locally and attached via request interceptor.
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

## License / status

Under active development.
