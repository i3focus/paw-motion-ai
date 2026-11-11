import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Tabs, useRouter } from 'expo-router';
import { colors, fonts, fontSize, shadow } from '../../src/theme';

export default function TabsLayout() {
  const router = useRouter();
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarStyle: styles.bar,
        tabBarActiveTintColor: colors.teal,
        tabBarInactiveTintColor: colors.muted,
        tabBarLabelStyle: styles.label,
      }}
    >
      <Tabs.Screen name="feed" options={{ title: 'Feed', tabBarIcon: ({ focused }) => <Text style={[styles.icon, focused && styles.iconActive]}>▦</Text> }} />
      <Tabs.Screen name="map" options={{ title: 'Map', tabBarIcon: ({ focused }) => <Text style={[styles.icon, focused && styles.iconActive]}>◎</Text> }} />
      <Tabs.Screen
        name="create"
        options={{
          title: '',
          tabBarButton: () => (
            <View style={styles.fabWrap}>
              <Pressable
                onPress={() => router.push('/create-post')}
                accessibilityRole="button"
                accessibilityLabel="Create post"
                style={({ pressed }) => [styles.fab, pressed && { transform: [{ scale: 0.94 }] }]}
              >
                <Text style={styles.fabText}>+</Text>
              </Pressable>
            </View>
          ),
        }}
        listeners={{ tabPress: (e) => e.preventDefault() }}
      />
      <Tabs.Screen name="ai" options={{ title: 'AI', tabBarIcon: ({ focused }) => <Text style={[styles.icon, focused && styles.iconActive]}>✦</Text> }} />
      <Tabs.Screen name="profile" options={{ title: 'Profile', tabBarIcon: ({ focused }) => <Text style={[styles.icon, focused && styles.iconActive]}>◉</Text> }} />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  bar: { backgroundColor: colors.white, borderTopColor: colors.line, height: 78, paddingTop: 6 },
  label: { fontFamily: fonts.w800, fontSize: fontSize.caption },
  icon: { fontSize: 22, color: colors.muted },
  iconActive: { color: colors.teal },
  fabWrap: { flex: 1, alignItems: 'center' },
  fab: { width: 58, height: 58, borderRadius: 29, backgroundColor: colors.coral, alignItems: 'center', justifyContent: 'center', marginTop: -18, borderWidth: 4, borderColor: colors.cream, ...shadow.coral },
  fabText: { color: colors.coralInk, fontSize: 30, fontFamily: fonts.w900, marginTop: -4 },
});
