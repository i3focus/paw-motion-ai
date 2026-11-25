#!/usr/bin/env bash
# Regenerates docs/api/openapi.yaml from the live backend (requires Docker for Testcontainers)
# and rebuilds the TypeScript types in src/schema.d.ts.
# The generator is the exact openapi-typescript version pinned in package.json/package-lock.json
# (installed with `npm ci`, run with `npx --no-install`, so no floating download happens).
set -euo pipefail
root="$(cd "$(dirname "$0")/../../.." && pwd)"
(cd "$root/backend" && EXPORT_OPENAPI=true ./gradlew test --tests 'OpenApiExportIT' -q)
# The exported `servers` URL carries the random test port; normalize it so regeneration is deterministic.
sed -i.bak -E 's#^- url: http://localhost:[0-9]+#- url: http://localhost:8080#' "$root/docs/api/openapi.yaml"
rm -f "$root/docs/api/openapi.yaml.bak"
cd "$root/packages/api-client"
npm ci
npx --no-install openapi-typescript ../../docs/api/openapi.yaml -o src/schema.d.ts
echo "generated packages/api-client/src/schema.d.ts"
