# Builds the React app and serves it behind Caddy (SDD section 11, TK-06).
FROM node:22-alpine AS build
WORKDIR /app
COPY src/frontend/package.json src/frontend/package-lock.json ./
RUN npm ci
COPY src/frontend/ ./
RUN npm run build

FROM caddy:2.10-alpine
COPY infra/proxy/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist /srv
