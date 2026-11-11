import { Redirect } from 'expo-router';
import { useAuth } from '../src/auth';
import { LoadingState } from '../src/components';

export default function Index() {
  const { owner, loading } = useAuth();
  if (loading) return <LoadingState label="Loading PETLIFE…" />;
  return <Redirect href={owner ? '/(tabs)/feed' : '/(auth)/login'} />;
}
