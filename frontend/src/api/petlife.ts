import type { components } from '@petlife/api-client';
import { request } from './client';

export type FeedCard = components['schemas']['FeedCardDto'];
export type FeedPage = components['schemas']['PageResponseFeedCardDto'];
export type StoryGroup = components['schemas']['StoryGroupDto'];
export type Story = components['schemas']['StoryDto'];
export type ReactionSummary = components['schemas']['ReactionSummaryDto'];
export type Comment = components['schemas']['CommentDto'];
export type CommentPage = components['schemas']['PageResponseCommentDto'];
export type Media = components['schemas']['MediaDto'];
export type Place = components['schemas']['PlaceDto'];
export type PlacePage = components['schemas']['PageResponsePlaceDto'];
export type LostPetAlert = components['schemas']['LostPetAlertDto'];
export type Playdate = components['schemas']['PlaydateDto'];
export type NearbyPet = components['schemas']['NearbyPetDto'];
export type Conversation = components['schemas']['ConversationDto'];
export type AiMessage = components['schemas']['AiMessageDto'];
export type AiAction = components['schemas']['AiActionDto'];
export type AiUsage = components['schemas']['AiUsageDto'];
export type SendMessageResponse = components['schemas']['SendMessageResponse'];
export type PetEngagement = components['schemas']['PetEngagementDto'];
export type BadgeDto = components['schemas']['BadgeDto'];
export type Challenge = components['schemas']['ChallengeDto'];
export type Leaderboard = components['schemas']['LeaderboardDto'];
export type PetProfile = components['schemas']['PetProfileDto'];
export type PetPost = components['schemas']['PetPostDto'];
export type Adventure = components['schemas']['AdventureDto'];
export type PetFriend = components['schemas']['PetFriendDto'];
export type Community = components['schemas']['CommunityDto'];
export type CommunityEvent = components['schemas']['CommunityEventDto'];
export type Discover = components['schemas']['DiscoverDto'];
export type Notification = components['schemas']['NotificationDto'];
export type NotificationPage = components['schemas']['PageResponseNotificationDto'];
export type NotificationPreferences = components['schemas']['NotificationPreferencesDto'];
export type FollowState = components['schemas']['FollowStateDto'];
export type HealthPassport = components['schemas']['HealthPassportDto'];
export type HealthRecord = components['schemas']['HealthRecordDto'];
export type Booking = components['schemas']['BookingDto'];

const q = (params: Record<string, string | number | boolean | undefined>) =>
  Object.entries(params).filter(([, v]) => v !== undefined && v !== '').map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`).join('&');
const p = (path: string, params: Record<string, string | number | boolean | undefined> = {}) =>
  `${path}${Object.keys(params).length ? `?${q(params)}` : ''}`;

// Feed and stories
export const getFeed = (page = 0, size = 20, lat?: number, lng?: number) => request<FeedPage>(p('/api/feed', { page, size, lat, lng }));
export const react = (itemId: string, type: 'PAW' | 'LAUGH' | 'HEART') => request<ReactionSummary>(`/api/feed/${itemId}/reaction`, { method: 'PUT', body: JSON.stringify({ type }) });
export const unreact = (itemId: string) => request<ReactionSummary>(`/api/feed/${itemId}/reaction`, { method: 'DELETE' });
export const getComments = (itemId: string, page = 0) => request<CommentPage>(p(`/api/feed/${itemId}/comments`, { page, size: 20 }));
export const addComment = (itemId: string, body: string) => request<Comment>(`/api/feed/${itemId}/comments`, { method: 'POST', body: JSON.stringify({ body }) });
export const getStories = () => request<StoryGroup[]>('/api/stories');
export const createStory = (input: components['schemas']['CreateStoryRequest']) => request<Story>('/api/stories', { method: 'POST', body: JSON.stringify(input) });
export const viewStory = (storyId: string) => request<void>(`/api/stories/${storyId}/view`, { method: 'POST' });
export const recordAdEvent = (campaignId: string, type: 'IMPRESSION' | 'CLICK', placement = 'FEED_CARD') =>
  request<void>(`/api/ads/${campaignId}/events`, { method: 'POST', body: JSON.stringify({ type, placement }) });

// Posts and media
export const createPost = (input: components['schemas']['CreatePostRequest']) => request<components['schemas']['PostCreatedDto']>('/api/posts', { method: 'POST', body: JSON.stringify(input) });
export async function uploadMedia(file: File): Promise<Media> {
  const form = new FormData();
  form.append('file', file);
  const token = localStorage.getItem('pawmotion.owner.accessToken');
  const res = await fetch('/api/media', { method: 'POST', headers: token ? { Authorization: `Bearer ${token}` } : {}, body: form });
  if (!res.ok) throw new Error(`Upload failed (${res.status})`);
  return res.json();
}

// Map
export const getPlaces = (lat: number, lng: number, radius = 5000, category?: string, query?: string) =>
  request<PlacePage>(p('/api/places', { lat, lng, radius, category, q: query }));
export const getPlace = (placeId: string) => request<Place>(`/api/places/${placeId}`);
export const checkIn = (placeId: string, petId: string) => request<void>(`/api/places/${placeId}/check-ins`, { method: 'POST', body: JSON.stringify({ petId }) });
export const getLostPets = (lat: number, lng: number, radius = 10000) => request<LostPetAlert[]>(p('/api/lost-pets', { lat, lng, radius }));
export const createLostPetAlert = (input: components['schemas']['CreateLostPetAlertRequest']) => request<LostPetAlert>('/api/lost-pets', { method: 'POST', body: JSON.stringify(input) });
export const reportSighting = (alertId: string, input: components['schemas']['SightingRequest']) => request<void>(`/api/lost-pets/${alertId}/sightings`, { method: 'POST', body: JSON.stringify(input) });
export const resolveAlert = (alertId: string, found: boolean) => request<LostPetAlert>(`/api/lost-pets/${alertId}/resolve`, { method: 'POST', body: JSON.stringify({ found }) });
export const getPlaydates = () => request<Playdate[]>('/api/playdates');
export const createPlaydate = (input: components['schemas']['CreatePlaydateRequest']) => request<Playdate>('/api/playdates', { method: 'POST', body: JSON.stringify(input) });
export const respondPlaydate = (playdateId: string, petId: string, response: 'ACCEPTED' | 'DECLINED') =>
  request<Playdate>(`/api/playdates/${playdateId}/respond`, { method: 'POST', body: JSON.stringify({ petId, response }) });
export const getNearbyPets = (lat: number, lng: number, radius = 5000) => request<NearbyPet[]>(p('/api/pets/nearby', { lat, lng, radius }));

// AI assistant
export const getConversations = () => request<components['schemas']['PageResponseConversationDto']>('/api/assistant/conversations');
export const createConversation = (petId?: string) => request<Conversation>('/api/assistant/conversations', { method: 'POST', body: JSON.stringify({ petId }) });
export const getMessages = (conversationId: string) => request<AiMessage[]>(`/api/assistant/conversations/${conversationId}/messages`);
export const sendMessage = (conversationId: string, content: string) =>
  request<SendMessageResponse>(`/api/assistant/conversations/${conversationId}/messages`, { method: 'POST', body: JSON.stringify({ content }) });
export const confirmAction = (actionId: string) => request<AiAction>(`/api/assistant/actions/${actionId}/confirm`, { method: 'POST' });
export const cancelAction = (actionId: string) => request<AiAction>(`/api/assistant/actions/${actionId}/cancel`, { method: 'POST' });
export const getAiUsage = () => request<AiUsage>('/api/assistant/usage');
export async function writeCaption(petId: string, hint?: string): Promise<{ caption: string }> {
  const convo = await createConversation(petId);
  const res = await sendMessage(convo.id!, `Write a short, playful Instagram-style caption for my pet's photo.${hint ? ` Context: ${hint}` : ''}`);
  return { caption: res.assistantMessage?.content ?? '' };
}

// Engagement and pet profile
export const getPetEngagement = (petId: string) => request<PetEngagement>(`/api/pets/${petId}/engagement`);
export const getPetBadges = (petId: string) => request<BadgeDto[]>(`/api/pets/${petId}/badges`);
export const getChallenges = () => request<Challenge[]>('/api/engagement/challenges');
export const joinChallenge = (challengeId: string, petId: string) => request<Challenge>(`/api/engagement/challenges/${challengeId}/join`, { method: 'POST', body: JSON.stringify({ petId }) });
export const getLeaderboard = (scope: 'neighborhood' | 'global' = 'neighborhood') => request<Leaderboard>(p('/api/engagement/leaderboard', { scope }));
export const getPetProfile = (petId: string) => request<PetProfile>(`/api/pets/${petId}/profile`);
export const getPetPosts = (petId: string, page = 0) => request<components['schemas']['PageResponsePetPostDto']>(p(`/api/pets/${petId}/posts`, { page, size: 12 }));
export const getPetAdventures = (petId: string, page = 0) => request<components['schemas']['PageResponseAdventureDto']>(p(`/api/pets/${petId}/adventures`, { page, size: 20 }));
export const getPetFriends = (petId: string, page = 0) => request<components['schemas']['PageResponsePetFriendDto']>(p(`/api/pets/${petId}/friends`, { page, size: 20 }));
export const followPet = (petId: string, follow: boolean) => request<FollowState>(`/api/pets/${petId}/follow`, { method: follow ? 'PUT' : 'DELETE' });
export const getHealthPassport = (petId: string) => request<HealthPassport>(`/api/pets/${petId}/health`);

// Communities and discover
export const getCommunities = (kind?: string, query?: string, page = 0) =>
  request<components['schemas']['PageResponseCommunityDto']>(p('/api/communities', { kind, q: query, page }));
export const createCommunity = (input: components['schemas']['CreateCommunityRequest']) => request<Community>('/api/communities', { method: 'POST', body: JSON.stringify(input) });
export const joinCommunity = (communityId: string, join: boolean) => request<Community>(`/api/communities/${communityId}/join`, { method: join ? 'POST' : 'DELETE' });
export const getCommunityEvents = (communityId: string, upcoming = true, page = 0) =>
  request<components['schemas']['PageResponseCommunityEventDto']>(p(`/api/communities/${communityId}/events`, { upcoming, page }));
export const rsvpEvent = (eventId: string, status: 'GOING' | 'INTERESTED') => request<CommunityEvent>(`/api/events/${eventId}/rsvp`, { method: 'POST', body: JSON.stringify({ status }) });
export const getDiscover = (lat?: number, lng?: number) => request<Discover>(p('/api/discover', { lat, lng }));

// Notifications
export const getNotifications = (page = 0) => request<NotificationPage>(p('/api/notifications', { page, size: 30 }));
export const getUnreadCount = () => request<{ unread: number }>('/api/notifications/unread-count');
export const markNotificationRead = (id: string) => request<void>(`/api/notifications/${id}/read`, { method: 'POST' });
export const markAllRead = () => request<void>('/api/notifications/read-all', { method: 'POST' });
export const getNotificationPreferences = () => request<NotificationPreferences>('/api/me/notification-preferences');
export const updateNotificationPreferences = (prefs: NotificationPreferences) =>
  request<NotificationPreferences>('/api/me/notification-preferences', { method: 'PATCH', body: JSON.stringify(prefs) });
export const registerPushToken = (token: string, platform: 'IOS' | 'ANDROID' | 'WEB') =>
  request<void>('/api/devices/push-tokens', { method: 'POST', body: JSON.stringify({ token, platform }) });

// Bookings (owner side)
export const listMyBookings = (page = 0) => request<components['schemas']['PageResponseBookingDto']>(p('/api/bookings', { page, size: 30 }));
export const createBooking = (input: components['schemas']['CreateBookingRequest']) =>
  request<Booking>('/api/bookings', { method: 'POST', body: JSON.stringify(input) });
export const cancelBooking = (bookingId: string) => request<Booking>(`/api/bookings/${bookingId}/cancel`, { method: 'POST' });

// Admin review (owner token, requires ADMIN role)
export type AdminCampaign = components['schemas']['AdminCampaignDto'];
export const getAdminCampaignQueue = (status = 'IN_REVIEW', page = 0) =>
  request<components['schemas']['PageResponseAdminCampaignDto']>(p('/api/admin/campaigns', { status, page }));
export const approveCampaign = (campaignId: string) => request<AdminCampaign>(`/api/admin/campaigns/${campaignId}/approve`, { method: 'POST' });
export const rejectCampaign = (campaignId: string, reason?: string) =>
  request<AdminCampaign>(`/api/admin/campaigns/${campaignId}/reject`, { method: 'POST', body: JSON.stringify({ reason }) });
export const verifyPartner = (partnerId: string, verified: boolean) =>
  request<components['schemas']['PartnerProfileDto']>(p(`/api/admin/partners/${partnerId}/verify`, { verified }), { method: 'POST' });
