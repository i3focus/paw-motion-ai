# ADR 0019 — Shared design tokens as the single visual source of truth

Status: accepted (2026-11-11, refactor phase 1)

## Context

The redesign (`docs/design-reference/landing.html`, `app.html`) had to land on both
web (Tailwind) and mobile (React Native) with identical colors, radii, shadows, and
typography. Duplicating constants per app guarantees drift.

## Decision

A single `packages/design-tokens` package exports the token set consumed by the web
Tailwind theme and the RN theme object. `docs/design/design-system.md` documents
values and usage; both apps consume the package — no per-app color literals for
token-covered values.

## Consequences

- One change propagates to both platforms.
- Components (`Button`, `Card`, `TextInput`, `Badge`, `Avatar`, `AppShell` variants)
  exist per platform but share token inputs.
- New visual values must go through the package; ad-hoc literals in screens are
  a review defect.
