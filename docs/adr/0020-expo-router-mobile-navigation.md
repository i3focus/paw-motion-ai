# ADR 0020 — expo-router file-based navigation for the mobile app

Status: accepted (2026-11-11, refactor phase 4)

## Context

The pre-refactor mobile app used a state-based shell (`App.tsx` switching screens).
That could not provide deep links (`petlife://pet/123`, notification taps), a native
modal for create-post, or an auth guard without hand-rolled routing logic.

## Decision

Mobile uses expo-router with the `mobile/app/` file tree: `(tabs)/` for the five-tab
layout (Feed, Map, centered create `+`, AI, Profile), `(auth)/` for login/signup, and
stack routes (`pet/[id]`, `notifications`, `discover`, `create-post` modal).
Auth state gates the tree in `app/_layout.tsx`.

## Consequences

- Deep links and notification-tap navigation work without extra code.
- The legacy `src/screens/*` tree was deleted; there is exactly one navigation model.
- Route changes are file moves, visible in diffs and reviews.
