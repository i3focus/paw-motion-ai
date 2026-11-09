import { useCallback, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { ApiError, generateInsight, listPets } from '../api/client';
import { Badge, Button, Card, EmptyState, ErrorText, Field, Input } from '../components';
import type { Insight, Pet } from '../types';
import { colors } from '../theme';

export function InsightsScreen() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [petId, setPetId] = useState('');
  const [periodDays, setPeriodDays] = useState('7');
  const [insight, setInsight] = useState<Insight | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    listPets()
      .then((petList) => {
        setPets(petList);
        if (petList[0]) {
          setPetId(petList[0].id);
        }
      })
      .catch(() => undefined);
  }, []);

  const generate = useCallback(async () => {
    const period = Number(periodDays);
    if (!Number.isInteger(period) || period < 1 || period > 90) {
      setError('Period must be a whole number between 1 and 90 days');
      return;
    }
    setError(null);
    setLoading(true);
    try {
      setInsight(await generateInsight({ petId: petId || undefined, periodDays: period }));
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to generate insight');
    } finally {
      setLoading(false);
    }
  }, [petId, periodDays]);

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Text style={styles.title}>AI Insights</Text>
      <Text style={styles.subtitle}>Non-medical activity summaries based on recorded data.</Text>
      <Card title="Generate insight">
        <Field label="Pet">
          <View style={styles.petPicker}>
            <Button label="All pets" variant={!petId ? 'primary' : 'secondary'} onPress={() => setPetId('')} />
            {pets.map((pet) => (
              <Button key={pet.id} label={pet.name} variant={petId === pet.id ? 'primary' : 'secondary'} onPress={() => setPetId(pet.id)} />
            ))}
          </View>
        </Field>
        <Field label="Period (days)"><Input value={periodDays} onChangeText={setPeriodDays} keyboardType="numeric" /></Field>
        <ErrorText message={error} />
        <Button label={loading ? 'Generating…' : 'Generate'} onPress={generate} disabled={loading} />
      </Card>
      {insight ? (
        <Card title="Latest insight">
          <Text style={styles.insightText}>{insight.text}</Text>
          <View style={styles.badges}>
            <Badge label={`Provider: ${insight.provider}`} />
            {insight.model ? <Badge label={`Model: ${insight.model}`} /> : null}
            {insight.fallbackUsed ? <Badge label="Fallback" tone="warning" /> : null}
          </View>
        </Card>
      ) : (
        <EmptyState title="No insight generated" description="Generate a summary after recording activities." />
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream50 },
  content: { padding: 20, gap: 16 },
  title: { color: colors.cream900, fontSize: 30, fontWeight: '900' },
  subtitle: { color: colors.cream600, marginTop: -10, marginBottom: 4 },
  petPicker: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  insightText: { color: colors.cream800, fontSize: 16, lineHeight: 24 },
  badges: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 14 },
});
