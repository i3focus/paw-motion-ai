import { useEffect } from 'react';
import { Stack, useRouter, useSegments } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { Platform, View } from 'react-native';
import * as Notifications from 'expo-notifications';
import { useFonts, Nunito_600SemiBold, Nunito_700Bold, Nunito_800ExtraBold, Nunito_900Black } from '@expo-google-fonts/nunito';
import { AuthProvider, useAuth } from '../src/auth';
import { registerPushToken } from '../src/api/petlife';
import { colors } from '../src/theme';

Notifications.setNotificationHandler({
  handleNotification: async () => ({ shouldShowBanner: true, shouldShowList: true, shouldPlaySound: false, shouldSetBadge: true }),
});

function Guard() {
  const { owner, loading } = useAuth();
  const segments = useSegments();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    const inAuthGroup = segments[0] === '(auth)';
    if (!owner && !inAuthGroup) {
      router.replace('/(auth)/login');
    } else if (owner && inAuthGroup) {
      router.replace('/(tabs)/feed');
    }
  }, [owner, loading, segments, router]);

  useEffect(() => {
    if (!owner) return;
    Notifications.getPermissionsAsync()
      .then((perm) => (perm.granted ? perm : Notifications.requestPermissionsAsync()))
      .then((perm) => {
        if (!perm.granted) return;
        return Notifications.getExpoPushTokenAsync()
          .then((t) => registerPushToken(t.data, Platform.OS === 'ios' ? 'IOS' : 'ANDROID'))
          .catch(() => {});
      })
      .catch(() => {});
  }, [owner]);

  useEffect(() => {
    const sub = Notifications.addNotificationResponseReceivedListener((response) => {
      const data = response.notification.request.content.data as Record<string, unknown> | undefined;
      const petId = data?.petId;
      if (typeof petId === 'string' && /^[0-9a-f-]{36}$/i.test(petId)) {
        router.push(`/pet/${petId}`);
      } else {
        router.push('/notifications');
      }
    });
    return () => sub.remove();
  }, [router]);

  if (loading) {
    return <View style={{ flex: 1, backgroundColor: colors.cream }} />;
  }
  return (
    <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.cream } }}>
      <Stack.Screen name="create-post" options={{ presentation: 'modal' }} />
    </Stack>
  );
}

export default function RootLayout() {
  const [fontsLoaded, fontsError] = useFonts({ Nunito_600SemiBold, Nunito_700Bold, Nunito_800ExtraBold, Nunito_900Black });
  if (!fontsLoaded && !fontsError) {
    return <View style={{ flex: 1, backgroundColor: colors.cream }} />;
  }
  return (
    <AuthProvider>
      <StatusBar style="dark" />
      <Guard />
    </AuthProvider>
  );
}
