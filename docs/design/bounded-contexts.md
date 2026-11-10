# Bounded Contexts

| Context | Purpose | Aggregate roots | Owns | Does not own |
|---|---|---|---|---|
| Identity | Authentication, authorization, account lifecycle | Owner | credentials, roles, account status | pets, activities |
| Pet | Pet profiles and ownership | Pet | pet attributes, lifecycle, preferences | activity history |
| Activity | Activity records, live-tracked routes, goals input, statistics | Activity, ActivityStatistics | activity facts, routes, derived stats | AI text, rewards, Strava tokens |
| AI Insights | AI summaries and generation policy | Insight | prompts, provider metadata, fallback policy | raw activity ownership |
| Engagement | Goals, streaks, badges, challenges, rewards | WeeklyGoal, Challenge, Reward | progress and completion rules | partner campaign definitions |
| Partner | Business profiles, public profile/location, campaigns, interactions, analytics, offers, coupons | BusinessProfile, Campaign, Coupon | campaign rules, public profile, interaction aggregates, coupon inventory | owner activity histories, credit balances |
| Billing | Fiat payments for credit purchases | PaymentTransaction | payment lifecycle, provider references, CPP snapshot | credit balances |
| Ledger | Partner wallets and credit balances | Wallet, LedgerAccount | accounts, append-only ledger entries, balances | payment processing, campaign content |
| Integration | Third-party account links (Strava) | StravaConnection, StravaPublication | OAuth tokens (encrypted), publication status | activity facts |
| Notification | In-app notification generation/delivery | Notification | notification preferences and delivery state | business rules of other contexts |

## Context rules

- Cross-context references use stable IDs, not JPA relationships.
- Each context owns its tables and migrations.
- REST endpoints may compose read models, but writes cross contexts through application services or events.
- Partner analytics must be aggregate-only.
- AI Insights receives validated activity aggregates, not repository access.
- Every credit balance change goes through the Ledger inbound port; no other context writes ledger tables (ADR-0014).
- Campaign creation and its credit debit, and payment confirmation and its credit, are the only cross-context writes allowed inside one local transaction (ADR-0014).
- Owner routes and locations never leave the Activity context except to the owner and opt-in Strava publication.
