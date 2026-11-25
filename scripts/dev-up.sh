#!/usr/bin/env bash
# Local bootstrap: generates .env with random secrets (only if missing) and starts the stack.
# Secret values are never printed.
set -euo pipefail
cd "$(dirname "$0")/.."

OLD_JWT_LITERAL='replace-with-at-least-32-random-bytes-for-local-demo-only'

env_value() { grep -E "^$1=" .env | tail -n1 | cut -d= -f2- || true; }

if [ -f .env ]; then
  jwt="$(env_value JWT_SECRET)"
  if [ -z "$jwt" ] || [ "$jwt" = "$OLD_JWT_LITERAL" ]; then
    echo "error: .env has an empty or placeholder JWT_SECRET. Set a random value (openssl rand -base64 48) or remove .env to regenerate it." >&2
    exit 1
  fi
else
  command -v openssl >/dev/null || { echo "error: openssl is required to generate secrets" >&2; exit 1; }
  umask 077
  JWT_SECRET_GEN="$(openssl rand -base64 48)"
  FAKE_WEBHOOK_SECRET_GEN="$(openssl rand -base64 32)"
  POSTGRES_PASSWORD_GEN="$(openssl rand -hex 24)"
  export JWT_SECRET_GEN FAKE_WEBHOOK_SECRET_GEN POSTGRES_PASSWORD_GEN
  awk '
    /^JWT_SECRET=/ { print "JWT_SECRET=" ENVIRON["JWT_SECRET_GEN"]; next }
    /^FAKE_WEBHOOK_SECRET=/ { print "FAKE_WEBHOOK_SECRET=" ENVIRON["FAKE_WEBHOOK_SECRET_GEN"]; next }
    /^POSTGRES_PASSWORD=/ { print "POSTGRES_PASSWORD=" ENVIRON["POSTGRES_PASSWORD_GEN"]; next }
    /^SPRING_PROFILES_ACTIVE=/ { print "SPRING_PROFILES_ACTIVE=local"; next }
    /^PAYMENT_PROVIDER=/ { print "PAYMENT_PROVIDER=fake"; next }
    { print }
  ' .env.example > .env
  unset JWT_SECRET_GEN FAKE_WEBHOOK_SECRET_GEN POSTGRES_PASSWORD_GEN
  chmod 600 .env
  echo ".env generated with random local secrets (profile local, fake payments)."
fi

docker compose up --build
