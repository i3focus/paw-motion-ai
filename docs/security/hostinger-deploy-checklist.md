# Checklist de deploy na Hostinger (borda e actuator)

Complementa o guia de deploy local da equipe (`docs/deploy-hostinger.md`, não versionado). Topologia: `Internet -> Caddy (TLS) -> nginx do frontend -> backend:8080 -> postgres`. Só o Caddy publica portas.

## 1. Sub-rede e IPs fixos (N13)

O `docker-compose.yml` fixa a rede `172.28.0.0/24` e o IP do nginx do frontend em `172.28.0.10`. O backend confia em `X-Forwarded-For` só desse IP (`TRUSTED_PROXIES` padrão `172.28.0.10/32`). O nginx confia só no Caddy (`TRUSTED_PROXY_CIDRS`). Não use faixas amplas (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`): qualquer contêiner ou proxy dessa faixa passaria a escolher a chave do limite de taxa.

`docker-compose.prod.yml` (override no VPS):

```yaml
services:
  postgres:
    restart: unless-stopped
    ports: !reset []          # sem publicação, nem em 127.0.0.1
  backend:
    restart: unless-stopped
    ports: !reset []          # alcançável só pela rede do compose (via nginx)
    volumes:
      - media-data:/app/data/media
  frontend:
    restart: unless-stopped
    ports: !reset []
    expose: ["80"]
  caddy:
    image: caddy:2-alpine
    restart: unless-stopped
    ports: ["80:80", "443:443"]
    networks:
      default:
        ipv4_address: 172.28.0.2   # IP fixo, usado em TRUSTED_PROXY_CIDRS
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

`.env` de produção:

```bash
# nginx do frontend confia só no Caddy (IP fixo acima)
TRUSTED_PROXY_CIDRS=172.28.0.2/32
# backend confia só no nginx do frontend (já é o padrão do compose; explicite para auditoria)
TRUSTED_PROXIES=172.28.0.10/32
```

Se mudar a sub-rede, ajuste juntos: `networks.default.ipam`, `ipv4_address` do frontend e do Caddy e as duas variáveis. Se o Caddy ficar atrás de outro proxy/CDN (Cloudflare), o Caddy precisa de `trusted_proxies` próprio, senão o IP do cliente vira o do CDN.

## 2. Actuator (N4)

- Só `/actuator/health` é público, sem detalhes. `/actuator/info`, `/actuator/metrics` e demais exigem token de ADMIN.
- O nginx só encaminha `/api/`. Não crie regra para `/actuator` no nginx nem no Caddy.
- Para métricas, acesse de dentro do VPS: `docker compose exec backend curl -H "Authorization: Bearer <token admin>" http://localhost:8080/actuator/metrics`.

## 3. Lista de verificação antes de liberar

- [ ] `docker compose -f docker-compose.yml -f docker-compose.prod.yml ps`: só o `caddy` com portas publicadas (80 e 443); backend, postgres e frontend sem `0.0.0.0` nem `127.0.0.1`.
- [ ] `ufw status`: só 22, 80 e 443.
- [ ] `docker compose ... config | grep -E "TRUSTED_PROXY|TRUSTED_PROXIES"`: `172.28.0.2/32` no frontend e `172.28.0.10/32` no backend, sem faixas amplas.
- [ ] `curl -s https://app.example.com/actuator/health` não responde JSON do backend (o nginx devolve o SPA); `curl -s https://app.example.com/api/actuator/info` responde 401/403/404, nunca 200.
- [ ] Dentro do backend: `curl -s localhost:8080/actuator/health` retorna `{"status":"UP"}` sem `components`; `curl -s localhost:8080/actuator/metrics` retorna 401 sem token e 403 com token de dono.
- [ ] Spoof de XFF: 12 logins seguidos em `https://app.example.com/api/auth/login` com `X-Forwarded-For` diferente em cada um devem terminar em 429 (o Caddy sobrescreve o cabeçalho do cliente).
- [ ] Logs do backend mostram IPs de clientes reais (não `172.28.0.2`) nas chaves de limite de taxa. Se aparecer sempre o IP do Caddy, `TRUSTED_PROXY_CIDRS` está vazio ou errado: todos os usuários dividiriam um único bucket.
- [ ] `PAYMENT_PROVIDER=abacatepay`, `API_DOCS_ENABLED=false`, `JWT_SECRET` forte, `ADMIN_EMAILS` correto, `CORS_ALLOWED_ORIGINS` só `https://`.
- [ ] `.env` com `chmod 600`.

## 4. Rollback

Restaurar o `TRUSTED_PROXIES` / `TRUSTED_PROXY_CIDRS` anteriores e `docker compose up -d`. A restrição do actuator é código (`SecurityConfiguration`); reverter exige novo deploy.
## 5. Pendências antes de publicar (fechamento da auditoria)
- [ ] `docs/deploy-hostinger.md` (linhas ~102-103) ainda sugere `TRUSTED_PROXY_CIDRS=172.16.0.0/12` e `TRUSTED_PROXIES=172.16.0.0/12`. Use os valores da seção 1 (`172.28.0.2/32` e `172.28.0.10/32`) e corrija o guia.
- [ ] `RATE_LIMIT_MODE=shadow` (padrão) só afeta o bucket `webhook`. Observe `ratelimit.shadow_violations` e passe para `enforce`.
- [ ] `JWT_SECRET` exclusivo deste ambiente. Defina `JWT_LEGACY_COMPAT_UNTIL` (data passada ou `JWT_LEGACY_ISSUERS` vazio) após a janela de migração; a janela reabre a cada reinício.
- [ ] `ADMIN_EMAILS` correto. Para revogar admins removidos da lista, ligar `ADMIN_RECONCILE=true` uma vez (e voltar a `false`).
- [ ] Backup do banco e do volume de mídia antes de aplicar V25 e V26; mover a mídia para o volume `media-data`.
- [ ] `MAIL_ENABLED` e `EXPO_PUSH_ENABLED` (desligados por padrão): ao ligar, aceitar envio best effort (sem outbox durável).
- [ ] `INSIGHTS_RETENTION_ENABLED` decidido (desligado por padrão); `MEDIA_ORPHAN_SWEEP_ENABLED=false` mantém só dry-run.
- [ ] Rodar o SQL de inventário de URLs herdadas (`validation-results.md`, 11.5) e anotar o resultado.
- [ ] Teste de restore de backup Android com o token em `expo-secure-store`.
- [ ] Decisões de produto abertas: gate de membership do RSVP; moderação de comunidades/eventos; quotas por pessoa (não só por conta).
- [ ] Mergear a pilha na ordem de `remediation-summary.md` seção 4; lotes 0-2 antes de qualquer exposição pública.
