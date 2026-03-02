# ============================================================
# Finans Portalı – Frontend Dockerfile
# Multi-stage build: Node build → Nginx serve
# ============================================================

# ── Stage 1: Build ───────────────────────────────────────────
FROM node:20-alpine AS build

WORKDIR /app

# Bağımlılık cache'i
COPY package.json package-lock.json* ./
RUN npm ci --silent

# Kaynak kodu kopyala ve build al
COPY . .
RUN npm run build

# ── Stage 2: Serve with Nginx ────────────────────────────────
FROM nginx:alpine

LABEL maintainer="Finans Portalı Ekibi"
LABEL description="Finance Portal Frontend – React + Nginx"

# Custom nginx config (SPA routing desteği)
RUN echo 'server { \
    listen 3000; \
    server_name localhost; \
    root /usr/share/nginx/html; \
    index index.html; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
    location /api/ { \
        proxy_pass http://backend:8080; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
    } \
}' > /etc/nginx/conf.d/default.conf

# Build çıktısını kopyala
COPY --from=build /app/build /usr/share/nginx/html

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -qO- http://localhost:3000 || exit 1

CMD ["nginx", "-g", "daemon off;"]
