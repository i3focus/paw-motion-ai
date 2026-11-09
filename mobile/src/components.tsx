import type { ReactNode } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { colors } from './theme';

export function Card({ title, children }: { title?: string; children: ReactNode }) {
  return (
    <View style={styles.card}>
      {title ? <Text style={styles.cardTitle}>{title}</Text> : null}
      {children}
    </View>
  );
}

export function Badge({ label, tone = 'neutral' }: { label: string; tone?: 'neutral' | 'accent' | 'success' | 'warning' }) {
  const background = tone === 'accent' ? colors.accentSoft : tone === 'success' ? '#ECFDF3' : tone === 'warning' ? '#FFFAEB' : colors.cream100;
  const color = tone === 'accent' ? colors.secondary : tone === 'success' ? colors.success : tone === 'warning' ? '#B54708' : colors.cream700;
  return <View style={[styles.badge, { backgroundColor: background }]}><Text style={[styles.badgeText, { color }]}>{label}</Text></View>;
}

export function Button({ label, onPress, disabled, variant = 'primary' }: { label: string; onPress: () => void; disabled?: boolean; variant?: 'primary' | 'secondary' }) {
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      disabled={disabled}
      style={({ pressed }) => [
        styles.button,
        variant === 'primary' ? styles.primaryButton : styles.secondaryButton,
        pressed && styles.pressed,
        disabled && styles.disabled,
      ]}
    >
      {disabled ? <ActivityIndicator color={variant === 'primary' ? colors.white : colors.primary} /> : <Text style={[styles.buttonText, variant === 'primary' ? styles.primaryText : styles.secondaryText]}>{label}</Text>}
    </Pressable>
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
  return <TextInput {...props} placeholderTextColor={colors.cream500} style={[styles.input, props.style]} />;
}

export function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <View style={styles.empty}>
      <Text style={styles.emptyTitle}>{title}</Text>
      <Text style={styles.emptyText}>{description}</Text>
    </View>
  );
}

export function ErrorText({ message }: { message: string | null }) {
  if (!message) {
    return null;
  }
  return <Text style={styles.error}>{message}</Text>;
}

const styles = StyleSheet.create({
  card: { backgroundColor: colors.white, borderColor: colors.cream200, borderWidth: 1, borderRadius: 24, padding: 18 },
  cardTitle: { color: colors.cream900, fontSize: 18, fontWeight: '800', marginBottom: 14 },
  badge: { borderRadius: 999, paddingHorizontal: 10, paddingVertical: 4, alignSelf: 'flex-start' },
  badgeText: { fontSize: 12, fontWeight: '700' },
  button: { borderRadius: 18, paddingVertical: 13, paddingHorizontal: 16, alignItems: 'center', justifyContent: 'center' },
  primaryButton: { backgroundColor: colors.primary },
  secondaryButton: { backgroundColor: colors.white, borderColor: colors.cream300, borderWidth: 1 },
  primaryText: { color: colors.white },
  secondaryText: { color: colors.cream800 },
  buttonText: { fontSize: 15, fontWeight: '800' },
  pressed: { opacity: 0.85 },
  disabled: { opacity: 0.55 },
  field: { marginBottom: 12 },
  label: { color: colors.cream700, fontSize: 13, fontWeight: '700', marginBottom: 6 },
  input: { backgroundColor: colors.white, borderColor: colors.cream300, borderWidth: 1, borderRadius: 16, paddingHorizontal: 14, paddingVertical: 12, color: colors.cream900, fontSize: 15 },
  empty: { borderColor: colors.cream300, borderWidth: 1, borderStyle: 'dashed', borderRadius: 24, backgroundColor: colors.cream50, padding: 22, alignItems: 'center' },
  emptyTitle: { color: colors.cream900, fontSize: 16, fontWeight: '800' },
  emptyText: { color: colors.cream600, fontSize: 14, marginTop: 6, textAlign: 'center' },
  error: { color: colors.danger, backgroundColor: '#FEF3F2', borderRadius: 14, padding: 12, fontSize: 14, marginBottom: 12 },
});
