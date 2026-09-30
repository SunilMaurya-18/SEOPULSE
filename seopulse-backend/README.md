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
| `REDIS_HOST` / `REDIS_PORT` | Redis connection |
| `JWT_SECRET` | HMAC signing secret (required) |
| `JWT_EXPIRATION` | Token lifetime in ms |

> Prefer environment variables for secrets. Avoid committing production credentials.

## Auth

Public endpoints:
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

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

## Audit lifecycle

```text
QUEUED → CRAWLING → ANALYZING → COMPLETED
                              ↘ FAILED
```

URL validation allows only public HTTP/HTTPS hosts and blocks private/loopback/link-local targets.

## Build & test

```bash
mvn test
mvn package
```

## Frontend pairing

The React app expects this API on port **8082**.

See the root [README.md](../README.md) and [frontend README](../seopulse-frontend/README.md).

## Status

Under active development.
