#!/usr/bin/env bash
# Regenerates docs/api/openapi.yaml from the live backend (requires Docker for Testcontainers)
# and rebuilds the TypeScript types in src/schema.d.ts.
set -euo pipefail
cd "$(dirname "$0")/../../.."

(cd backend && EXPORT_OPENAPI=true ./gradlew test --tests 'OpenApiExportIT' -q)

npx -y openapi-typescript@7 docs/api/openapi.yaml -o packages/api-client/src/schema.d.ts
echo "generated packages/api-client/src/schema.d.ts"
