import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { ApiError } from '../api/client';
import { useAuth } from '../auth';
import { Button, ErrorText, Field, Input } from '../components';
import { colors } from '../theme';
import { isValidEmail, normalizeEmail } from '../validation';

export function AuthScreen() {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  async function submit() {
    const errors: Record<string, string> = {};
    const normalizedEmail = normalizeEmail(email);
    if (!isValidEmail(normalizedEmail)) {
      errors.email = 'Enter a valid email address';
    }
    if (password.length < 8) {
      errors.password = 'Password must be at least 8 characters';
    }
    if (mode === 'register' && !displayName.trim()) {
      errors.displayName = 'Display name is required';
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    setError(null);
    setLoading(true);
    try {
      if (mode === 'login') {
        await login(normalizedEmail, password);
      } else {
        await register(normalizedEmail, password, displayName.trim());
      }
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to authenticate');
    } finally {
      setLoading(false);
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.logo}>PawMotion <Text style={styles.logoAccent}>AI</Text></Text>
        <Text style={styles.tagline}>Move together. Live better.</Text>
      </View>
      <View style={styles.card}>
        <View style={styles.tabs}>
          {(['login', 'register'] as const).map((value) => (
            <Button key={value} label={value === 'login' ? 'Login' : 'Register'} variant={mode === value ? 'primary' : 'secondary'} onPress={() => setMode(value)} disabled={loading} />
          ))}
        </View>
        {mode === 'register' ? <Field label="Display name"><Input value={displayName} onChangeText={setDisplayName} autoCapitalize="words" /></Field> : null}
        {mode === 'register' ? <ErrorText message={fieldErrors.displayName ?? null} /> : null}
        <Field label="Email"><Input value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" /></Field>
        <ErrorText message={fieldErrors.email ?? null} />
        <Field label="Password"><Input value={password} onChangeText={setPassword} secureTextEntry /></Field>
        <ErrorText message={fieldErrors.password ?? null} />
        <ErrorText message={error} />
        <Button label={loading ? 'Please wait…' : mode === 'login' ? 'Login' : 'Create account'} onPress={submit} disabled={loading} />
        <Text style={styles.footer}>Pet owners only. Partners use the web Partner Portal.</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.cream50, justifyContent: 'center', padding: 20 },
  hero: { alignItems: 'center', marginBottom: 24 },
  logo: { color: colors.secondary, fontSize: 34, fontWeight: '900' },
  logoAccent: { color: colors.accent },
  tagline: { color: colors.cream600, marginTop: 6 },
  card: { backgroundColor: colors.white, borderRadius: 28, borderColor: colors.cream200, borderWidth: 1, padding: 20 },
  tabs: { flexDirection: 'row', gap: 10, marginBottom: 16 },
  footer: { color: colors.cream500, fontSize: 12, marginTop: 14, textAlign: 'center' },
});
