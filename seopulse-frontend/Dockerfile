# syntax=docker/dockerfile:1

# ---- Build ------------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .

# Relative by default: behind Caddy the API is same-origin, so one image
# serves staging and production.
ARG VITE_API_BASE_URL=/api/v1
ARG VITE_SENTRY_DSN=
ARG VITE_RELEASE=
ARG SENTRY_ORG=
ARG SENTRY_PROJECT=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_SENTRY_DSN=$VITE_SENTRY_DSN \
    VITE_RELEASE=$VITE_RELEASE \
    SENTRY_ORG=$SENTRY_ORG \
    SENTRY_PROJECT=$SENTRY_PROJECT

# The Sentry token is a build secret so it never lands in an image layer.
RUN --mount=type=secret,id=sentry_auth_token,env=SENTRY_AUTH_TOKEN \
    npm run build

# ---- Runtime ----------------------------------------------------------------
FROM nginxinc/nginx-unprivileged:alpine

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q --spider http://127.0.0.1:8080/healthz || exit 1
