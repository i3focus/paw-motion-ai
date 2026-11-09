export interface Owner {
  id: string;
  email: string;
  displayName: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  owner: Owner;
}

export interface Pet {
  id: string;
  ownerId: string;
  name: string;
  species: string;
  breed: string | null;
  birthDate: string | null;
  size: string;
  activityPreference: string | null;
  status: string;
  createdAt: string;
}

export interface Activity {
  id: string;
  ownerId: string;
  petId: string;
  type: string;
  startedAt: string;
  durationSeconds: number;
  distanceMeters: number | null;
  notes: string | null;
  createdAt: string;
}

export interface DashboardSummary {
  ownerId: string;
  petCount: number;
  activityCountLast7Days: number;
  totalDistanceMetersLast7Days: number;
  totalDurationSecondsLast7Days: number;
  currentStreakDays: number;
  recentActivities: Activity[];
}

export interface Insight {
  id: string;
  ownerId: string;
  petId: string | null;
  text: string;
  provider: string;
  model: string | null;
  fallbackUsed: boolean;
  generatedAt: string;
}
