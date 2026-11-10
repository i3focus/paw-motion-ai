# ADR-0017: Partner Geolocation and Discovery Maps

- **Status**: Proposed
- **Context**: Owners must see partner details (area of operation, services, website, location) when clicking an ad and browse partners on a map besides the campaign list. The declared stack is PostgreSQL + PostGIS and MapLibre + OpenStreetMap.
- **Decision**: Store partner location as PostGIS `geography(Point, 4326)` in a Partner-owned `partner_public_profiles` table with categories and services. Nearby search uses `ST_DWithin` with a GiST index. Web uses MapLibre GL JS; mobile uses a MapLibre React Native binding (requires an Expo development build). Tiles come from an OSM-based tile provider configured by environment (`MAP_TILE_URL`, `MAP_STYLE_URL`) respecting its usage policy. Address geocoding is optional behind `GeocodingPort` (e.g. Nominatim) with manual map pin as fallback.
- **Alternatives**: Google Maps (rejected: cost/licensing, not in stack); storing lat/long as plain doubles (rejected: no spatial index/queries); `react-native-maps` (acceptable fallback if MapLibre native setup blocks the demo).
- **Rationale**: Reuses the planned spatial stack and keeps maps vendor-neutral.
- **Positive consequences**: Same spatial foundation can later serve activity routes (ADR-0018).
- **Negative consequences**: Mobile needs a dev build; public OSM tile servers are not for production traffic.
- **Risks**: Geocoding accuracy in Brazil; mitigated by manual pin fallback.
- **Decision update (2026-11-10)**: Addresses are geocoded automatically (`GeocodingPort`, Nominatim adapter by default) so partners appear on the map without manual steps; manual pin only when geocoding fails. Activity routes do not use PostGIS in the first demo (GeoJSON, ADR-0018).
- **Implementation guidance**: Public endpoints expose only public profile fields; owner location is a query parameter and never persisted.
- **Migration/reversal**: Swap tile/geocoding providers via configuration and adapters.
