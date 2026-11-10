# Open Questions

1. Where is the authoritative product PRD with formal requirement IDs and acceptance criteria?
2. Should authentication use Keycloak immediately, or is a development JWT issuer acceptable for the hackathon demo?
3. Which AI provider should be the primary demo provider: Gemini or OpenAI?
4. What are the exact species, size, activity type, and challenge enums?
5. What are the weekly goal defaults and badge/challenge reward rules?
6. Should activities include GPS traces in MVP, or is manual distance/duration sufficient for the demo?
7. What campaign analytics fields are considered aggregate and safe to expose to partners?
8. What account deletion retention period should be implemented for LGPD compliance?
9. Which deployment targets are mandatory for submission: Render, Fly.io, Vercel, or local Docker only?
10. Is PostGIS required for the first demo or can GeoJSON be stored until map visualization is implemented?

## CR-2026-11 (credits, discovery, live tracking)

11. Which payment provider will be used (Stripe, Mercado Pago, Pagar.me, other), and are both PIX and card required for the MVP demo?
12. What are the initial values for CPP (`BILLING_CREDITS_PER_CURRENCY_UNIT`), `CAMPAIGN_PRICE_AMOUNT`, `CAMPAIGN_CREATION_COST_CREDITS`, and `BILLING_MIN_PURCHASE_AMOUNT`? Should suggested credit packs exist?
13. Should `CAMPAIGN_CREATION_COST_CREDITS` always equal `CAMPAIGN_PRICE_AMOUNT × CPP` (derive one from the other and fail on mismatch), or are they intentionally independent (e.g. discounted packs)? Current spec: independent with a startup warning.
14. When a wallet has multiple accounts, which account is debited by default (MAIN only, BONUS first, or partner choice)? What account types exist besides `MAIN`?
15. On refunds/chargebacks when credits were already spent: block the partner, allow negative balance, or absorb the loss?
16. Should archiving or rejecting a campaign refund credits (fully/partially)? Do campaigns have an expiry/duration tied to the credits paid?
17. Do credits expire? Are invoices/fiscal documents (NFS-e) required for credit purchases?
18. Is "CPP" intended as credits-per-currency-unit (assumed) or cost-per-click pricing in the future (campaign debits per click instead of per creation)?
19. Which partner categories and service taxonomy should be offered? Is geocoding required or is a manual map pin enough?
20. Should impressions be tracked in addition to clicks, and what minimum aggregation (k-anonymity) is acceptable for partner breakdowns?
21. Is background location tracking required for the demo, or is foreground tracking with the screen on acceptable? Is an Expo development build acceptable for the mobile demo?
22. Should Strava auto-publish be on by default after connecting, and which Strava sport types map to walk/run/play?
