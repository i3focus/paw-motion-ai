import { colors, fontFamily, fontSize, radius, shadows, spacing, layout } from '@petlife/design-tokens';

export { colors, fontSize, radius, spacing, layout };

export const fonts = {
  w600: fontFamily.native.semibold,
  w700: fontFamily.native.bold,
  w800: fontFamily.native.extrabold,
  w900: fontFamily.native.black,
} as const;

export const shadow = {
  card: shadows.card.native,
  raised: shadows.raised.native,
  coral: shadows.coral.native,
} as const;
