# Open Questions

## Resolved decisions (2026-11-10)

| # | Question | Decision |
|---|---|---|
| 1 | Authoritative product PRD? | No separate PRD exists; `requirements.md` is the authoritative requirements source, evolved through change requests. |
| 2 | Keycloak or development JWT issuer? | Platform's own JWT issuer for the hackathon (ADR-0008). |
| 3 | Primary AI provider? | OpenAI (ADR-0009); deterministic fallback kept for local/CI. |
| 4 | Species, size, activity type enums? | As implemented: species `DOG`, `CAT`, `BIRD`, `RABBIT`, `OTHER`; size `SMALL`, `MEDIUM`, `LARGE`; activity `WALK`, `RUN`, `HIKE`, `BIKE_RIDE`, `SWIM`, `PLAY`. |
| 5 | Goal defaults and badge/challenge rules? | Admin or AI create challenges (e.g. "Walk 3 km this week → Walker badge"); challenges are one-to-many; one badge can be awarded to many owners (REQ-CHAL-002). |
| 6 | GPS traces in MVP? | Yes, mobile app only (REQ-TRACK-001). |
| 7 | Safe partner analytics fields? | Counters of owner views (clicks) per campaign; no owner identity (REQ-CAMP-002). |
| 8 | Account deletion retention? | Recommendation documented in REQ-NFR-001 (30-day grace, 6-month access logs, 5-year financial records anonymized) — pending legal review. |
| 9 | Deployment target? | Docker (Compose) first. |
| 10 | PostGIS required for first demo? | No: activity routes stored as GeoJSON and rendered from GeoJSON (REQ-TRACK-002). PostGIS used for partner nearby search. |
| 11 | Payment provider? | AbacatePay hosted checkout with PIX and card (ADR-0015). |
| 12 | Initial pricing values? | Defined per environment through environment variables; no credit packs. Partners get a credits explainer (REQ-LEDGER-004). |
| 13 | Campaign cost vs. price × CPP? | `credits = floor(amountPaid × CPP)` (integer). Campaign price and credit cost stay independent env vars with a startup warning on mismatch. |
| 14 | Accounts and debit order? | Wallet created at partner registration with `MAIN`, `BONUS`, `PARTNER`; debit order `MAIN` → `BONUS` → `PARTNER` (split when needed). |
| 15 | Refund/chargeback after credits spent? | Block the partner (wallet `BLOCKED`) until an Admin settles. |
| 16 | Archive refund and campaign expiry? | Archiving refunds credits; all campaigns expire after `CAMPAIGN_DURATION_DAYS` regardless of credits. |
| 17 | Credit expiry and fiscal documents? | Credits never expire; no fiscal documents required. |
| 18 | Meaning of CPP? | Credits per currency unit. |
| 19 | Partner taxonomy and geocoding? | Categories include pet vet, pet shop, feed store (casa de ração), pet laboratory, etc.; automatic geocoding puts the partner on the map. |
| 20 | Impressions and k-anonymity? | Clicks only for now; no breakdowns by owner attributes. |
| 21 | Background tracking / Expo dev build? | Background tracking required; mobile stores GPS data locally until sent to the backend (GeoJSON) and to Strava; app asks the owner to connect Strava. Expo development build accepted implicitly. |

## Still open

1. Strava: confirm proposed defaults — auto-publish **on** after connecting, and `sport_type` mapping `WALK→Walk`, `RUN→Run`, `HIKE→Hike`, `BIKE_RIDE→Ride`, `SWIM→Swim`, `PLAY→Workout`.
2. Archive refund: prorated by unused campaign time (current spec) or full refund of `creditsCharged`? Full refund allows running a campaign almost to expiry and recovering all credits.
3. What do `BONUS` and `PARTNER` accounts represent exactly, and who can credit them (Admin only, promotions, referral)? Current spec: Admin adjustments only.
4. When a partner is blocked after a chargeback, how is the debt settled (new purchase automatically offsets the negative difference, or manual Admin action only)?
5. Does a dispute (`checkout.disputed`) block the partner immediately (current spec) or only after the dispute is lost?
6. Does a partner need a CPF/CNPJ registered in the profile so checkouts skip the customer-data step (AbacatePay `customerId`), or is it collected on the checkout page?
7. Recommended retention periods need validation by legal counsel before production.

## Post-refactor additions (2026-11)

| # | Question | Decision |
|---|---|---|
| — | Real payment gateway? | Fake provider for MVP; AbacatePay env vars wired but unconfigured (ADR-0015) |
| — | Native maps library? | Positional placeholder pins; a maps lib needs an EAS dev build — deferred |
| — | Browser e2e suite? | Not built; API flows covered by Testcontainers, UI logic by vitest/jest |
