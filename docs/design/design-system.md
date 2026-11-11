# PETLIFE design system

Source of truth: `packages/design-tokens/src/index.ts`. Values are taken from `docs/design-reference/app.html` and `landing.html`.

## Consumption

| Platform | How |
|---|---|
| Web (Tailwind v4) | `frontend/src/index.css` imports `@petlife/design-tokens/tokens.css`, a generated `@theme` block. It resets Tailwind's default palette (`--color-*: initial`), so only PETLIFE colors exist as utilities (`bg-cream`, `text-teal`, `bg-coral`, `rounded-card`, `shadow-card`, `text-body`…). |
| Mobile (Expo) | `mobile/src/theme.ts` re-exports `colors`, `fontSize`, `radius`, `spacing`, `layout`, plus `fonts` (Nunito family per weight) and `shadow` (native shadow objects). |

After changing tokens, run `npm run build` in `packages/design-tokens`. CI fails if `tokens.css` is stale (`npm run check`).

## Color

| Token | Hex | Use |
|---|---|---|
| `cream` | `#FFF8EF` | App/page background |
| `sand` / `sandDeep` | `#F6EBDA` / `#EBDCC4` | Secondary cards, tracks, inactive story rings |
| `white` | `#FFFFFF` | Primary cards |
| `line` | `#E7D9C3` | Borders, dividers |
| `teal` / `tealDark` / `tealLight` / `mint` | `#0F5C5C` / `#093F3F` / `#DDEDEA` / `#9ED8CC` | Brand, headings, secondary actions, selected states, new-story glow |
| `coral` / `coralInk` | `#FF6B57` / `#3A0F09` | **The only primary action color.** Text on coral is always `coralInk`. |
| `amber` / `amberInk` / `amberSoft` | `#F2A33A` / `#7A4300` / `#FDF1DC` | Streak flame, notification dots, **Sponsored** label and outline |
| `ink` / `muted` | `#1B2D2D` / `#586767` | Body text / secondary text |
| `danger`, `success` (+ `Soft`) | `#B83A2B`, `#1F7A52` | Errors, confirmations |

Verified contrast (WCAG AA ≥ 4.5:1 for body text):

| Pair | Ratio |
|---|---|
| coralInk on coral | 5.98 |
| white on teal | 7.76 |
| teal on cream | 7.36 |
| muted on cream / white / sand | 5.61 / 5.91 / 5.02 |
| amberInk on amberSoft | 7.14 |
| danger on white / dangerSoft | 5.71 / 4.99 |
| success on white / successSoft | 5.30 / 4.61 |

Not allowed: white on coral (2.8), `muted` on `sandDeep` (4.38), and coral as a text color on light backgrounds.

## Typography

Nunito 600/700/800/900 (`@fontsource/nunito` on web, `@expo-google-fonts/nunito` on mobile), falling back to `ui-rounded`. Default weight is 600, headings are 800, and the logo and buttons are 900.

| Token | px | Use |
|---|---|---|
| `caption` | 13 | Meta labels, tab labels, badges only |
| `label` | 14 | Form labels, chips |
| `body` | 15 | **Minimum for body copy** |
| `bodyLg` | 17 | Landing body |
| `lead` | 19 | Intro paragraphs |
| `title` / `heading` / `display` / `hero` | 21 / 26 / 34 / 46 | Card titles → hero headline |

## Shape, elevation, spacing

- Radius: `sm 12`, `md 16`, `lg 20`, **`card 24`**, `sheet 32`, `pill 999`.
- Shadow: `card` (soft teal-tinted), `raised` (sheets and the phone frame), `coral` (primary button glow only).
- Spacing: 4px scale (`1 = 4px` … `16 = 64px`). Touch targets are ≥ 44px.

## Components

Web: `frontend/src/components/ui.tsx`. Mobile: `mobile/src/components.tsx`. Both expose the same vocabulary:

| Component | Notes |
|---|---|
| `Button` | `primary` (coral + coralInk), `secondary` (teal + white), `outline` (teal border), `ghost`; pill-shaped, min height 48 |
| `Card` | `white` / `sand` / `sponsored` (amber outline; always pair with `SponsoredLabel`) |
| `Badge`, `SponsoredLabel` | tones: neutral, accent, success, warning, danger, sponsored |
| `Chip` | Filter chips with `aria-pressed` / `accessibilityState.selected` |
| `ReactionBar` | Typed reactions `PAW` 🐾, `LAUGH` 😂, `HEART` ❤️ from `reactions` in the tokens package |
| `StoryRing` (web) | `new` (conic teal→amber + mint glow), `unseen`, `seen`, `add` |
| `BottomTabBar` (web) | Feed · Map · centered coral **+** · AI · Profile |
| `BottomSheet` (web) | Modal dialog, Escape/overlay closes |
| `EmptyState`, `LoadingState`, `ErrorMessage`/`ErrorText` | Required for every data screen |

Focus: every interactive element shows a 3px teal outline on `:focus-visible`.
