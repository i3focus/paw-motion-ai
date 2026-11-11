/**
 * PETLIFE design tokens: the single source of truth for web and mobile.
 * Values come from docs/design-reference/{app,landing}.html.
 */

export const colors = {
  cream: '#FFF8EF',
  sand: '#F6EBDA',
  sandDeep: '#EBDCC4',
  white: '#FFFFFF',
  line: '#E7D9C3',

  teal: '#0F5C5C',
  tealDark: '#093F3F',
  tealLight: '#DDEDEA',
  mint: '#9ED8CC',

  coral: '#FF6B57',
  coralInk: '#3A0F09',
  coralSoft: '#FDECE8',

  amber: '#F2A33A',
  amberInk: '#7A4300',
  amberSoft: '#FDF1DC',

  ink: '#1B2D2D',
  muted: '#586767',

  danger: '#B83A2B',
  dangerSoft: '#FDECE8',
  success: '#1F7A52',
  successSoft: '#E3F3EA',

  overlay: 'rgba(9, 63, 63, 0.45)',
} as const;

export type ColorToken = keyof typeof colors;

export const fontFamily = {
  /** CSS font stack (web). */
  web: "'Nunito', ui-rounded, 'SF Pro Rounded', 'Segoe UI', system-ui, -apple-system, sans-serif",
  /** Expo Google Fonts family per weight (native ignores fontWeight for custom fonts). */
  native: {
    semibold: 'Nunito_600SemiBold',
    bold: 'Nunito_700Bold',
    extrabold: 'Nunito_800ExtraBold',
    black: 'Nunito_900Black',
  },
} as const;

export const fontWeight = {
  semibold: 600,
  bold: 700,
  extrabold: 800,
  black: 900,
} as const;

export type FontWeightToken = keyof typeof fontWeight;

/** Type scale in px. Body copy never goes below `body` (15px). */
export const fontSize = {
  caption: 13,
  label: 14,
  body: 15,
  bodyLg: 17,
  lead: 19,
  title: 21,
  heading: 26,
  display: 34,
  hero: 46,
} as const;

export const lineHeight = {
  tight: 1.15,
  snug: 1.3,
  normal: 1.45,
  relaxed: 1.55,
} as const;

export const radius = {
  sm: 12,
  md: 16,
  lg: 20,
  card: 24,
  sheet: 32,
  pill: 999,
} as const;

export const spacing = {
  0: 0,
  1: 4,
  2: 8,
  3: 12,
  4: 16,
  5: 20,
  6: 24,
  8: 32,
  10: 40,
  12: 48,
  16: 64,
} as const;

type NativeShadow = {
  shadowColor: string;
  shadowOffset: { width: number; height: number };
  shadowOpacity: number;
  shadowRadius: number;
  elevation: number;
};

export const shadows = {
  card: {
    css: '0 8px 24px rgba(15, 92, 92, 0.10), 0 2px 6px rgba(15, 92, 92, 0.06)',
    native: { shadowColor: colors.teal, shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.1, shadowRadius: 12, elevation: 3 },
  },
  raised: {
    css: '0 16px 40px rgba(15, 92, 92, 0.16), 0 4px 10px rgba(15, 92, 92, 0.08)',
    native: { shadowColor: colors.teal, shadowOffset: { width: 0, height: 14 }, shadowOpacity: 0.16, shadowRadius: 20, elevation: 8 },
  },
  coral: {
    css: '0 8px 20px rgba(255, 107, 87, 0.38)',
    native: { shadowColor: colors.coral, shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.38, shadowRadius: 10, elevation: 6 },
  },
} as const satisfies Record<string, { css: string; native: NativeShadow }>;

export const layout = {
  /** Minimum touch target (WCAG 2.5.5 / platform guidelines). */
  touchTarget: 44,
  /** App column width used by the reference phone frame. */
  appMaxWidth: 430,
  /** Marketing content width used by the landing reference. */
  contentMaxWidth: 1180,
  tabBarHeight: 72,
  createButtonSize: 60,
} as const;

export const motion = {
  fast: 150,
  base: 250,
  slow: 400,
} as const;

export const reactions = [
  { type: 'PAW', label: 'Paw', emoji: '🐾' },
  { type: 'LAUGH', label: 'Haha', emoji: '😂' },
  { type: 'HEART', label: 'Love', emoji: '❤️' },
] as const;

export type ReactionType = (typeof reactions)[number]['type'];

export const tokens = { colors, fontFamily, fontWeight, fontSize, lineHeight, radius, spacing, shadows, layout, motion } as const;

export default tokens;
