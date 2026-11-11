import { useState } from 'react';
import { SafeAreaView, StyleSheet, Text, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { useFonts, Nunito_600SemiBold, Nunito_700Bold, Nunito_800ExtraBold, Nunito_900Black } from '@expo-google-fonts/nunito';
import { AuthProvider, useAuth } from './src/auth';
import { ActivitiesScreen } from './src/screens/ActivitiesScreen';
import { AuthScreen } from './src/screens/AuthScreen';
import { DashboardScreen } from './src/screens/DashboardScreen';
import { InsightsScreen } from './src/screens/InsightsScreen';
import { PetsScreen } from './src/screens/PetsScreen';
import { ProfileScreen } from './src/screens/ProfileScreen';
import { colors, fonts } from './src/theme';

type TabKey = 'dashboard' | 'pets' | 'activities' | 'insights' | 'profile';

const TABS: Array<{ key: TabKey; label: string }> = [
  { key: 'dashboard', label: 'Home' },
  { key: 'pets', label: 'Pets' },
  { key: 'activities', label: 'Activity' },
  { key: 'insights', label: 'Insights' },
  { key: 'profile', label: 'Profile' },
];

function Shell() {
  const { owner, loading } = useAuth();
  const [tab, setTab] = useState<TabKey>('dashboard');

  if (loading) {
    return <View style={styles.center}><Text style={styles.loading}>Loading PETLIFE…</Text></View>;
  }
  if (!owner) {
    return <AuthScreen />;
  }

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.body}>
        {tab === 'dashboard' ? <DashboardScreen /> : null}
        {tab === 'pets' ? <PetsScreen /> : null}
        {tab === 'activities' ? <ActivitiesScreen /> : null}
        {tab === 'insights' ? <InsightsScreen /> : null}
        {tab === 'profile' ? <ProfileScreen /> : null}
      </View>
      <View style={styles.tabs}>
        {TABS.map((item) => (
          <Text key={item.key} accessibilityRole="tab" accessibilityState={{ selected: tab === item.key }} onPress={() => setTab(item.key)} style={[styles.tab, tab === item.key && styles.tabActive]}>
            {item.label}
          </Text>
        ))}
      </View>
    </SafeAreaView>
  );
}

export default function App() {
  const [fontsLoaded] = useFonts({ Nunito_600SemiBold, Nunito_700Bold, Nunito_800ExtraBold, Nunito_900Black });
  if (!fontsLoaded) {
    return <View style={styles.center} />;
  }
  return (
    <AuthProvider>
      <StatusBar style="dark" />
      <Shell />
    </AuthProvider>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.cream },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.cream },
  loading: { color: colors.muted, fontSize: 16, fontFamily: fonts.w700 },
  body: { flex: 1 },
  tabs: { flexDirection: 'row', justifyContent: 'space-around', borderTopColor: colors.line, borderTopWidth: 1, backgroundColor: colors.white, paddingVertical: 10 },
  tab: { color: colors.muted, fontSize: 13, minHeight: 44, textAlignVertical: 'center', fontFamily: fonts.w800, paddingHorizontal: 8, paddingVertical: 6 },
  tabActive: { color: colors.teal },
});
