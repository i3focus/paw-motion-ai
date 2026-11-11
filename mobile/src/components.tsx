import type { ReactNode } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { reactions, type ReactionType } from '@petlife/design-tokens';
import { colors, fonts, fontSize, radius, shadow } from './theme';

export function Card({ title, children, variant = 'white' }: { title?: string; children: ReactNode; variant?: 'white' | 'sand' | 'sponsored' }) {
  return (
    <View style={[styles.card, variant === 'sand' && styles.cardSand, variant === 'sponsored' && styles.cardSponsored]}>
      {title ? <Text accessibilityRole="header" style={styles.cardTitle}>{title}</Text> : null}
      {children}
    </View>
  );
}

type BadgeTone = 'neutral' | 'accent' | 'success' | 'warning' | 'danger' | 'sponsored';

const badgeTones: Record<BadgeTone, { background: string; color: string; border?: string }> = {
  neutral: { background: colors.sand, color: colors.ink },
  accent: { background: colors.tealLight, color: colors.teal },
  success: { background: colors.successSoft, color: colors.success },
  warning: { background: colors.amberSoft, color: colors.amberInk },
  danger: { background: colors.dangerSoft, color: colors.danger },
  sponsored: { background: colors.amberSoft, color: colors.amberInk, border: colors.amber },
};

export function Badge({ label, tone = 'neutral' }: { label: string; tone?: BadgeTone }) {
  const t = badgeTones[tone];
  return (
    <View style={[styles.badge, { backgroundColor: t.background }, t.border ? { borderColor: t.border, borderWidth: 1 } : null]}>
      <Text style={[styles.badgeText, { color: t.color }]}>{label}</Text>
    </View>
  );
}

export function SponsoredLabel() {
  return <Badge label="Sponsored" tone="sponsored" />;
}

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost';

export function Button({ label, onPress, disabled, loading, variant = 'primary', accessibilityLabel }: { label: string; onPress: () => void; disabled?: boolean; loading?: boolean; variant?: ButtonVariant; accessibilityLabel?: string }) {
  const busy = loading ?? disabled;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel ?? label}
      accessibilityState={{ disabled: Boolean(disabled), busy: Boolean(busy) }}
      onPress={onPress}
      disabled={disabled}
      style={({ pressed }) => [styles.button, buttonStyles[variant], pressed && styles.pressed, disabled && styles.disabled]}
    >
      {busy ? <ActivityIndicator color={buttonText[variant].color} /> : <Text style={[styles.buttonText, buttonText[variant]]}>{label}</Text>}
    </Pressable>
  );
}

export function Chip({ label, selected = false, onPress }: { label: string; selected?: boolean; onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" accessibilityState={{ selected }} onPress={onPress} style={[styles.chip, selected ? styles.chipSelected : styles.chipIdle]}>
      <Text style={[styles.chipText, { color: selected ? colors.white : colors.teal }]}>{label}</Text>
    </Pressable>
  );
}

export function ReactionBar({ counts, selected, onReact }: { counts: Partial<Record<ReactionType, number>>; selected?: ReactionType | null; onReact: (type: ReactionType) => void }) {
  return (
    <View style={styles.reactions} accessibilityRole="toolbar" accessibilityLabel="Reactions">
      {reactions.map((reaction) => {
        const active = selected === reaction.type;
        return (
          <Pressable
            key={reaction.type}
            accessibilityRole="button"
            accessibilityLabel={`${reaction.label} (${counts[reaction.type] ?? 0})`}
            accessibilityState={{ selected: active }}
            onPress={() => onReact(reaction.type)}
            style={[styles.reaction, active && styles.reactionActive]}
          >
            <Text style={styles.reactionText}>{reaction.emoji} {counts[reaction.type] ?? 0}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      {children}
    </View>
  );
}

export function Input(props: React.ComponentProps<typeof TextInput>) {
  return <TextInput {...props} placeholderTextColor={colors.muted} style={[styles.input, props.style]} />;
}

export function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <View style={styles.empty}>
      <Text style={styles.emptyIcon} accessibilityElementsHidden>🐾</Text>
      <Text style={styles.emptyTitle}>{title}</Text>
      <Text style={styles.emptyText}>{description}</Text>
    </View>
  );
}

export function LoadingState({ label = 'Loading…' }: { label?: string }) {
  return (
    <View style={styles.loading} accessibilityRole="progressbar" accessibilityLabel={label}>
      <ActivityIndicator color={colors.teal} />
      <Text style={styles.loadingText}>{label}</Text>
    </View>
  );
}

export function ErrorText({ message }: { message: string | null }) {
  if (!message) {
    return null;
  }
  return <Text accessibilityRole="alert" style={styles.error}>{message}</Text>;
}

const buttonStyles = StyleSheet.create({
  primary: { backgroundColor: colors.coral, ...shadow.coral },
  secondary: { backgroundColor: colors.teal },
  outline: { backgroundColor: 'transparent', borderColor: colors.teal, borderWidth: 2 },
  ghost: { backgroundColor: 'transparent' },
});

const buttonText = StyleSheet.create({
  primary: { color: colors.coralInk },
  secondary: { color: colors.white },
  outline: { color: colors.teal },
  ghost: { color: colors.teal },
});

const styles = StyleSheet.create({
  card: { backgroundColor: colors.white, borderRadius: radius.card, padding: 18, ...shadow.card },
  cardSand: { backgroundColor: colors.sand, shadowOpacity: 0, elevation: 0 },
  cardSponsored: { borderColor: colors.amber, borderWidth: 2 },
  cardTitle: { color: colors.tealDark, fontSize: fontSize.title, fontFamily: fonts.w800, marginBottom: 14 },
  badge: { borderRadius: radius.pill, paddingHorizontal: 10, paddingVertical: 4, alignSelf: 'flex-start' },
  badgeText: { fontSize: fontSize.caption, fontFamily: fonts.w800 },
  button: { minHeight: 48, borderRadius: radius.pill, paddingHorizontal: 20, alignItems: 'center', justifyContent: 'center' },
  buttonText: { fontSize: fontSize.body, fontFamily: fonts.w900 },
  pressed: { opacity: 0.85, transform: [{ scale: 0.98 }] },
  disabled: { opacity: 0.5 },
  chip: { minHeight: 44, borderRadius: radius.pill, paddingHorizontal: 16, alignItems: 'center', justifyContent: 'center' },
  chipIdle: { backgroundColor: colors.white, ...shadow.card },
  chipSelected: { backgroundColor: colors.teal },
  chipText: { fontSize: fontSize.label, fontFamily: fonts.w800 },
  reactions: { flexDirection: 'row', gap: 8, flexWrap: 'wrap' },
  reaction: { minHeight: 44, borderRadius: radius.pill, paddingHorizontal: 12, justifyContent: 'center', backgroundColor: colors.sand },
  reactionActive: { backgroundColor: colors.tealLight, borderColor: colors.teal, borderWidth: 2 },
  reactionText: { color: colors.ink, fontSize: fontSize.label, fontFamily: fonts.w800 },
  field: { marginBottom: 12 },
  label: { color: colors.ink, fontSize: fontSize.label, fontFamily: fonts.w800, marginBottom: 6 },
  input: { minHeight: 48, backgroundColor: colors.white, borderColor: colors.line, borderWidth: 2, borderRadius: radius.md, paddingHorizontal: 14, paddingVertical: 12, color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w600 },
  empty: { borderRadius: radius.card, backgroundColor: colors.sand, padding: 22, alignItems: 'center' },
  emptyIcon: { fontSize: 32 },
  emptyTitle: { color: colors.tealDark, fontSize: fontSize.bodyLg, fontFamily: fonts.w800, marginTop: 8 },
  emptyText: { color: colors.muted, fontSize: fontSize.body, fontFamily: fonts.w600, marginTop: 6, textAlign: 'center' },
  loading: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 10, padding: 24 },
  loadingText: { color: colors.muted, fontSize: fontSize.body, fontFamily: fonts.w700 },
  error: { color: colors.danger, backgroundColor: colors.dangerSoft, borderRadius: radius.sm, padding: 12, fontSize: fontSize.body, fontFamily: fonts.w700, marginBottom: 12 },
});
