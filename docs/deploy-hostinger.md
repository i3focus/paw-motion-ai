# Deploying PawMotion AI on Hostinger

This guide deploys the backend (Spring Boot 4 / Java 21), PostgreSQL 17 + PostGIS,
and the React frontend (nginx) to a single **Hostinger VPS** using Docker Compose,
with HTTPS via Caddy. The mobile app is not deployed to Hostinger — it is built and
distributed through Expo/EAS and pointed at the public API URL (see §10).

> Scope: the repository ships only a `docker-compose.yml`, so a VPS (full OS + Docker)
> is the correct Hostinger product. Shared / "Premium" web hosting cannot run Java,
> Docker, or PostgreSQL — do not attempt it there.

---

## 1. What you need

| Item | Recommendation |
|---|---|
| Hostinger product | **VPS KVM 2** (2 vCPU / 8 GB RAM / 100 GB NVMe). KVM 1 (4 GB) works for a demo but is tight once Gradle builds images locally — prefer building images in CI and pulling, or add swap (§2.4). |
| OS image | Ubuntu 24.04 LTS |
| Domain | Any domain pointing to the VPS (Hostinger domain or external registrar) |
| Outbound access | AbacatePay API, Resend (optional), Expo push (optional), Docker Hub pulls |
| Secrets | Strong random `JWT_SECRET`, `POSTGRES_PASSWORD`, `FAKE_WEBHOOK_SECRET`, `ABACATEPAY_*` |

Suggested DNS layout (replace `example.com`):

```
A     app.example.com        <VPS IPv4>
AAAA  app.example.com        <VPS IPv6>   (optional)
```

One hostname serves the owner portal (`/`), the partner portal (`/partner`), the
admin review UI (`/app/admin`), and the API (`/api/*`).

---

## 2. One-time VPS setup

```bash
# 2.1 Update and create a deploy user
sudo apt update && sudo apt -y upgrade
sudo adduser deploy && sudo usermod -aG sudo deploy

# 2.2 Install Docker Engine + Compose plugin
sudo apt -y install ca-certificates curl gnupg ufw
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list
sudo apt update && sudo apt -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin git
sudo usermod -aG docker deploy   # log out/in after this

# 2.3 Firewall — only SSH, HTTP, HTTPS. Backend (8080) and Postgres (5432) stay private.
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable

# 2.4 (KVM 1 only) 4 GB swap so local image builds don't OOM
sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

Verify: `docker run --rm hello-world`.

---

## 3. Get the code onto the VPS

```bash
sudo mkdir -p /opt/pawmotion && sudo chown deploy:deploy /opt/pawmotion
cd /opt/pawmotion
git clone https://github.com/i3focus/paw-motion-ai.git .
```

If the repo is private, use a GitHub **deploy key** (read-only) on the VPS.

---

## 4. Production environment file

Create `/opt/pawmotion/.env` (never commit it — `.env` is already gitignored):

```bash
# --- Database (kept on the Docker network only) ---
POSTGRES_PASSWORD=<generate: openssl rand -base64 48>
POSTGRES_DB=pawmotion
POSTGRES_USER=pawmotion

# --- JWT: REQUIRED, no default. 32+ random bytes. ---
JWT_SECRET=<generate: openssl rand -base64 48>
JWT_ISSUER=https://api.pawmotion.ai
JWT_TTL_SECONDS=3600

# --- Public URLs / CORS (your real domain, https only) ---
CORS_ALLOWED_ORIGINS=https://app.example.com
PARTNER_PORTAL_BASE_URL=https://app.example.com/partner
APP_BASE_URL=https://app.example.com

# --- Proxy trust: requests reach the backend only via the frontend nginx on the
#     compose network (172.18.0.0/16 default) and Caddy in front of it. ---
TRUSTED_PROXY_CIDRS=172.16.0.0/12
TRUSTED_PROXIES=172.16.0.0/12

# --- Payments: REAL provider for production ---
PAYMENT_PROVIDER=abacatepay
ABACATEPAY_API_KEY=<live abc_... key>
ABACATEPAY_WEBHOOK_SECRET=<secret you register in the AbacatePay dashboard>
ABACATEPAY_CREDIT_PRODUCT_EXTERNAL_ID=pawmotion-credits-brl
BILLING_CURRENCY=BRL
BILLING_CREDITS_PER_CURRENCY_UNIT=10
BILLING_MIN_PURCHASE_AMOUNT=10.00
BILLING_MAX_PURCHASE_AMOUNT=10000.00
BILLING_PENDING_TTL=24h
FAKE_WEBHOOK_SECRET=<generate anyway: openssl rand -base64 32>

# --- Campaign pricing ---
CAMPAIGN_PRICE_AMOUNT=50.00
CAMPAIGN_CREATION_COST_CREDITS=500
CAMPAIGN_DURATION_DAYS=30

# --- Admin bootstrap: comma-separated emails promoted to ADMIN at login ---
ADMIN_EMAILS=you@example.com

# --- API docs: OFF in production ---
API_DOCS_ENABLED=false

# --- Email (optional; leave MAIL_ENABLED=false to log-stub) ---
MAIL_ENABLED=false
RESEND_API_KEY=
RESEND_FROM_EMAIL=Paw Motion AI <noreply@example.com>
SUPPORT_EMAIL=support@example.com

# --- Expo push (optional) ---
EXPO_PUSH_ENABLED=false

# --- Frontend build arg: empty = same-origin /api via nginx ---
VITE_API_BASE_URL=
```

> ⚠️ `PAYMENT_PROVIDER=fake` + the `/api/partner/billing/purchases/{id}/fake-complete`
> endpoint let any registered partner mint unlimited credits. Only deploy `fake`
> in disposable demos; production must use `abacatepay` (or add an explicit guard).

---

## 5. Production compose file (TLS + persistence)

The repo's `docker-compose.yml` is dev-shaped (SPA on `:5173`, no TLS, no media
volume). Add `docker-compose.prod.yml` and `Caddyfile` at the repo root on the VPS:

**`docker-compose.prod.yml`**

```yaml
services:
  postgres:
    restart: unless-stopped
    # ports stay unpublished — reachable only on the compose network

  backend:
    restart: unless-stopped
    volumes:
      - media-data:/app/data/media        # MEDIA_ROOT default ./data/media under WORKDIR /app
    # ports stay unpublished — reachable only via frontend nginx

  frontend:
    restart: unless-stopped
    expose:
      - "80"                             # internal only; Caddy is the public edge
    ports: !reset []                     # drop the dev 5173:80 publish

  caddy:
    image: caddy:2-alpine
    container_name: pawmotion-caddy
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy-data:/data
      - caddy-config:/config
    depends_on:
      frontend:
        condition: service_started

volumes:
  media-data:
  caddy-data:
  caddy-config:
```

**`Caddyfile`**

```caddy
app.example.com {
    encode zstd gzip
    reverse_proxy frontend:80
    header {
        X-Content-Type-Options nosniff
        X-Frame-Options DENY
        Referrer-Policy strict-origin-when-cross-origin
        -Server
    }
}
```

Caddy obtains and renews the Let's Encrypt certificate automatically once DNS
points at the VPS. HTTP→HTTPS redirect is automatic.

Chain of trust: `Internet → Caddy (TLS) → frontend nginx → /api → backend:8080 → postgres:5432`.
Only Caddy is published. nginx sets `X-Forwarded-For=$remote_addr` (the Caddy hop);
`TRUSTED_PROXY_CIDRS`/`TRUSTED_PROXIES` covering `172.16.0.0/12` let both layers
recover the real client IP for rate limiting.

---

## 6. Build & start

```bash
cd /opt/pawmotion
docker compose -f docker-compose.yml -f docker-compose.prod.yml build
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
```

Smoke checks (from the VPS):

```bash
curl -fsS http://localhost/actuator/health 2>/dev/null || \
  curl -fsS https://app.example.com/api/v1 2>/dev/null; true
curl -fsSI https://app.example.com/ | head -1          # expect 200 + TLS
curl -fsSI https://app.example.com/partner | head -1   # SPA fallback → 200
curl -fsS  https://app.example.com/actuator/health     # backend health via proxy
```

Wait — `/actuator/health` is on the backend, so it must be fetched through
`https://app.example.com/actuator/health` (nginx proxies only `/api/`!). If you
want public health checks, prefer `https://app.example.com/api/...` or restrict:
actually the compose healthcheck already runs **inside** the backend container, so
the canonical external check is simply that the site loads and logins work. For
external uptime monitoring, use `https://app.example.com/` (200) — that's enough.

> Note: the frontend nginx only proxies `location /api/` to the backend.
> `/actuator/*` and `/v3/api-docs` are NOT reachable publicly — good. Keep it that
> way; do not add public proxy rules for actuator endpoints.

---

## 7. AbacatePay webhook registration

1. In the AbacatePay dashboard, register the webhook URL:
   `https://app.example.com/api/webhooks/payments/abacatepay`
2. Set the same secret in `.env` → `ABACATEPAY_WEBHOOK_SECRET`.
3. The backend verifies `x-webhook-secret` + `x-webhook-signature` (HMAC-SHA256),
   re-fetches the checkout server-side, and reconciles pending purchases every
   5 min — so a missed webhook self-heals via `ReconcilePendingPaymentsService`.
4. Test with a real small purchase, then check:
   `docker compose logs backend | grep -i webhook`.

---

## 8. Backups

```bash
# /opt/pawmotion/backup.sh (chmod +x, add to deploy's crontab: 17 3 * * *)
#!/usr/bin/env bash
set -euo pipefail
DEST=/opt/pawmotion/backups/$(date +%F_%H%M)
mkdir -p "$DEST"
docker exec pawmotion-postgres pg_dump -U pawmotion -Fc pawmotion > "$DEST/pawmotion.dump"
docker run --rm -v paw-motion-ai_media-data:/src -v "$DEST":/dst alpine \
  tar -czf /dst/media.tar.gz -C /src .
find /opt/pawmotion/backups -mindepth 1 -maxdepth 1 -type d -mtime +14 -exec rm -rf {} +
```

Also enable **VPS snapshots** in hPanel (weekly). For real redundancy, copy
`backups/` off-box (rsync to another host or S3-compatible storage).

Restore: `docker exec -i pawmotion-postgres pg_restore -U pawmotion -d pawmotion --clean < pawmotion.dump`.

---

## 9. Updating

```bash
cd /opt/pawmotion
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f --tail=100 backend
```

Flyway runs migrations automatically on backend start (`ddl-auto: validate`).
If a release is bad: `git checkout <prev-tag> && docker compose ... up -d --build`.
DB rollback requires the pre-upgrade `pg_dump` — take one before each upgrade:

```bash
docker exec pawmotion-postgres pg_dump -U pawmotion -Fc pawmotion > /opt/pawmotion/backups/pre-upgrade-$(date +%F).dump
```

---

## 10. Mobile app (Expo) — build once, point at prod

The mobile app is a client; it does not run on the VPS.

```bash
cd mobile
EXPO_PUBLIC_API_BASE_URL=https://app.example.com eas build --platform android --profile production
EXPO_PUBLIC_API_BASE_URL=https://app.example.com eas build --platform ios --profile production
```

`mobile/src/api/client.ts` reads `EXPO_PUBLIC_API_BASE_URL` at build time; without
it the app falls back to dev `10.0.2.2`/`localhost` URLs. Distribute via Google
Play / TestFlight through EAS Submit. Push notifications need `extra.eas.projectId`
in `app.json` (currently absent) plus `EXPO_PUSH_ENABLED=true` on the backend.

---

## 11. Production security checklist (verify before launch)

- [ ] `PAYMENT_PROVIDER=abacatepay` (never `fake` with real users).
- [ ] `API_DOCS_ENABLED=false` (swagger/api-docs are `permitAll` when enabled).
- [ ] `JWT_SECRET`, `POSTGRES_PASSWORD`, `FAKE_WEBHOOK_SECRET` are ≥32 random bytes, unique per environment, and `.env` is `chmod 600` on the VPS.
- [ ] `ADMIN_EMAILS` contains only intended operator emails.
- [ ] `CORS_ALLOWED_ORIGINS` is exactly your `https://` origin(s) — no wildcard, no http.
- [ ] `TRUSTED_PROXIES`/`TRUSTED_PROXY_CIDRS` = docker overlay range only (`172.16.0.0/12`), not the permissive all-RFC1918 default.
- [ ] `ufw status` shows only 22/80/443; `docker compose ps` shows only `caddy` published.
- [ ] Postgres and backend have no `ports:` publish in the prod override.
- [ ] `.env` is not in git (`git check-ignore .env` → prints `.env`).
- [ ] AbacatePay webhook secret matches the dashboard registration.
- [ ] Backups scheduled + one restore tested.
- [ ] `media-data` volume mounted (uploaded photos survive container rebuilds).

---

## 12. Operating notes & limits of this layout

- **Single point of failure** — one VPS, no replicas. Fine for MVP; the rate
  limiters are in-memory per instance, so horizontal backend replicas would also
  need a shared limiter to stay effective.
- **In-memory fake gateway & rate limits** reset on backend restart — harmless in prod.
- **Scheduled jobs** (payment reconciliation, campaign expiry, health reminders)
  run in the single backend — do not scale backend replicas without leader election.
- **Logs**: `docker compose logs -f backend|frontend|postgres|caddy`; consider
  `docker compose logs --since 24h | grep -i "error\|webhook"` in a cron report.
- **Metrics**: `/actuator/metrics` requires any authenticated JWT — acceptable for
  MVP, but don't expose it beyond `/api/` proxying.
- **Larger scale path**: managed Postgres (or a second VPS) + image registry pulls
  + a real APM; the compose layout already isolates the data tier.

---

## Appendix — architecture on the VPS

```
                 ┌──────────────────────── Hostinger KVM VPS ────────────────────────┐
Internet 80/443─▶│  ufw → caddy (TLS, auto-cert)                                      │
                 │        └─▶ frontend nginx :80 (SPA + /api proxy)                   │
                 │                     └─▶ backend Spring Boot :8080 ──▶ postgres:5432 │
                 │  volumes: postgres-data, media-data, caddy-data                    │
                 └────────────────────────────────────────────────────────────────────┘
Outbound only:  api.abacatepay.com · api.resend.com · exp.host (push) · registry/docker pulls
```
