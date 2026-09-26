# SEOPulse Frontend

React + TypeScript UI for the SEOPulse SEO audit platform.

## Stack

- React 19
- TypeScript
- Vite 8
- Tailwind CSS 4
- React Router 7
- Axios
- Lucide icons

## Setup

```bash
npm install
cp .env.example .env
npm run dev
```

App runs at [http://localhost:5173](http://localhost:5173).

### Environment

```env
VITE_API_BASE_URL=http://localhost:8082/api/v1
```

Vite also proxies `/api` to `http://localhost:8082`.

## Scripts

| Command | Description |
|---|---|
| `npm run dev` | Start Vite development server |
| `npm run build` | Typecheck + production build |
| `npm run preview` | Preview production build |
| `npm run lint` | Oxlint |

## App routes

| Path | Purpose |
|---|---|
| `/` | Landing |
| `/login`, `/register` | Authentication |
| `/dashboard` | Overview / command center |
| `/websites` | Add and manage websites |
| `/audits` | Run and list audits |
| `/audits/:auditId` | Audit detail + download report |
| `/audits/:auditId/pages` | Page inventory |
| `/audits/:auditId/issues` | SEO issues |
| `/issues`, `/pages` | Cross-site explorers |
| `/settings` | Theme, account, workspace |

## Key product behaviors

- JWT login/register with protected app shell
- Auto workspace (project) creation after auth
- Landing URL handoff → connect website → start audit
- Dashboard overall metrics + per-website focus
- Live crawl polling while audits run
- Download report as HTML (printable/PDF) or JSON
- Light/dark theme, fixed sidebar, topbar back button, active workspace badge

## Source layout

```
src/
├── api/                 # Axios client + auth/projects/websites/audits APIs
├── components/          # UI primitives + layout
├── features/            # Landing, dashboard, websites features
├── lib/                 # auth, workspace, toast, audit report helpers
├── pages/               # Route-level pages
├── routes/              # AppRoutes + ProtectedRoute + onboarding
└── layouts/             # App shell layout
```

## Backend dependency

This UI expects the SEOPulse backend on port **8082**.

See the root [README.md](../README.md) for full-stack setup.
