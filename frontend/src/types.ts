export interface Owner {
  id: string;
  email: string;
  displayName: string;
  role: 'OWNER' | 'PARTNER' | string;
  bio: string | null;
  phone: string | null;
  avatarUrl: string | null;
  businessName: string | null;
  website: string | null;
  partnerDescription: string | null;
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

export interface Campaign {
  id: string;
  partnerId: string;
  title: string;
  description: string;
  targetAudience: string;
  callToAction: string;
  status: 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'ARCHIVED' | 'EXPIRED' | string;
  aiTopics: string[];
  analyzedAt: string | null;
  creditsCharged: number;
  creditsRefunded: number;
  expiresAt: string;
  remainingBalance: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface WalletAccount {
  id: string;
  walletId: string;
  code: 'MAIN' | 'BONUS' | 'PARTNER' | string;
  status: 'OPEN' | 'FROZEN' | string;
  balance: number;
}

export interface Wallet {
  id: string;
  partnerId: string;
  status: 'ACTIVE' | 'BLOCKED' | string;
  blockedReason: string | null;
  accounts: WalletAccount[];
}

export interface LedgerEntry {
  id: string;
  accountId: string;
  direction: 'CREDIT' | 'DEBIT' | string;
  amount: number;
  balanceAfter: number;
  referenceType: string;
  referenceId: string;
  description: string | null;
  occurredAt: string;
}

export interface BillingPricing {
  currency: string;
  creditsPerCurrencyUnit: number;
  minPurchaseAmount: number;
  maxPurchaseAmount: number;
  campaignPriceAmount: number;
  campaignCreationCostCredits: number;
  campaignDurationDays: number;
  paymentProvider: string;
}

export interface PaymentTransaction {
  id: string;
  partnerId: string;
  amount: number;
  currency: string;
  expectedCredits: number;
  method: string | null;
  provider: string;
  status: 'PENDING' | 'CONFIRMED' | 'FAILED' | 'EXPIRED' | 'CANCELED' | 'DISPUTED' | 'REFUNDED' | string;
  providerCheckoutUrl: string | null;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface OwnerRecommendation {
  campaignId: string;
  partnerName: string;
  campaignTitle: string;
  description: string;
  callToAction: string;
  recommendedTopics: string[];
  analyzedAt: string | null;
}
