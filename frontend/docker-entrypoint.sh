#!/bin/sh
set -eu

TRUSTED_PROXY_CIDRS="${TRUSTED_PROXY_CIDRS:-}"
BLOCK=""

if [ -n "$TRUSTED_PROXY_CIDRS" ]; then
  OLD_IFS=$IFS
  IFS=','
  for cidr in $TRUSTED_PROXY_CIDRS; do
    cidr=$(printf '%s' "$cidr" | tr -d '[:space:]')
    if [ -n "$cidr" ]; then
      BLOCK="$BLOCK
  set_real_ip_from $cidr;"
    fi
  done
  IFS=$OLD_IFS
fi

awk -v block="$BLOCK" '
  index($0, "# __TRUSTED_PROXY_CIDRS__") { printf "%s\n", block; next }
  { print }
' /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf

exec nginx -g 'daemon off;'
