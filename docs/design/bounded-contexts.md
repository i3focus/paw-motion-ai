# Bounded Contexts

| Context | Purpose | Aggregate roots | Owns | Does not own |
|---|---|---|---|---|
| Identity | Authentication, authorization, account lifecycle | Owner | credentials, roles, account status | pets, activities |
| Pet | Pet profiles and ownership | Pet | pet attributes, lifecycle, preferences | activity history |
| Activity | Activity records, goals input, statistics | Activity, ActivityStatistics | activity facts, derived stats | AI text, rewards |
| AI Insights | AI summaries and generation policy | Insight | prompts, provider metadata, fallback policy | raw activity ownership |
| Engagement | Goals, streaks, badges, challenges, rewards | WeeklyGoal, Challenge, Reward | progress and completion rules | partner campaign definitions |
| Partner | Business profiles, campaigns, offers, coupons | Campaign, Coupon | campaign rules and coupon inventory | owner activity histories |
| Notification | In-app notification generation/delivery | Notification | notification preferences and delivery state | business rules of other contexts |

## Context rules

- Cross-context references use stable IDs, not JPA relationships.
- Each context owns its tables and migrations.
- REST endpoints may compose read models, but writes cross contexts through application services or events.
- Partner analytics must be aggregate-only.
- AI Insights receives validated activity aggregates, not repository access.
