const mockSecure: Record<string, string> = {};
jest.mock('expo-secure-store', () => ({
  WHEN_UNLOCKED_THIS_DEVICE_ONLY: 'WHEN_UNLOCKED_THIS_DEVICE_ONLY',
  isAvailableAsync: jest.fn(async () => true),
  getItemAsync: jest.fn(async (k: string) => mockSecure[k] ?? null),
  setItemAsync: jest.fn(async (k: string, v: string) => { mockSecure[k] = v; }),
  deleteItemAsync: jest.fn(async (k: string) => { delete mockSecure[k]; }),
}));
jest.mock('@react-native-async-storage/async-storage', () =>
  require('@react-native-async-storage/async-storage/jest/async-storage-mock'));

import AsyncStorage from '@react-native-async-storage/async-storage';
import appConfig from '../../app.json';
import { clearSession, getStoredToken, storeSession } from '../api/client';

const KEY = 'pawmotion.mobile.accessToken';
const auth = { accessToken: 'jwt-novo', tokenType: 'Bearer', expiresInSeconds: 3600, owner: { id: '1' } } as never;

describe('token no armazenamento seguro (N10)', () => {
  beforeEach(async () => {
    for (const k of Object.keys(mockSecure)) delete mockSecure[k];
    await AsyncStorage.clear();
  });

  it('grava no secure store e nunca no AsyncStorage', async () => {
    await storeSession(auth);
    expect(mockSecure[KEY]).toBe('jwt-novo');
    expect(await AsyncStorage.getItem(KEY)).toBeNull();
    expect(await getStoredToken()).toBe('jwt-novo');
  });

  it('migra de forma transparente o token legado e apaga o texto puro', async () => {
    await AsyncStorage.setItem(KEY, 'jwt-legado');
    expect(await getStoredToken()).toBe('jwt-legado');
    expect(mockSecure[KEY]).toBe('jwt-legado');
    expect(await AsyncStorage.getItem(KEY)).toBeNull();
  });

  it('logout limpa os dois armazenamentos', async () => {
    await storeSession(auth);
    await AsyncStorage.setItem(KEY, 'sobra');
    await clearSession();
    expect(await getStoredToken()).toBeNull();
  });

  it('desabilita o backup automatico do Android', () => {
    expect(appConfig.expo.android.allowBackup).toBe(false);
  });
});
