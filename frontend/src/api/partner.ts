import type { components } from '@petlife/api-client';
import { getStoredPartnerToken, request } from './client';

export type CampaignV2 = components['schemas']['CampaignV2Dto'];
export type CampaignMetrics = components['schemas']['CampaignMetricsDto'];
export type UpdateCampaignRequest = components['schemas']['UpdateCampaignRequest'];
export type PartnerProfileV2 = components['schemas']['PartnerProfileDto'];
export type UpdatePartnerProfileRequest = components['schemas']['UpdatePartnerProfileRequest'];
export type PartnerService = components['schemas']['PartnerServiceDto'];
export type PartnerServiceRequest = components['schemas']['PartnerServiceRequest'];
export type Booking = components['schemas']['BookingDto'];
export type AdminCampaign = components['schemas']['AdminCampaignDto'];

const token = () => getStoredPartnerToken();
const auth = <T>(path: string, init?: RequestInit) => request<T>(path, init, token());

// ---- campaigns (v2) ----
export const listCampaignsV2 = () => auth<CampaignV2[]>('/api/partner/v2/campaigns');
export const getCampaign = (id: string) => auth<CampaignV2>(`/api/partner/campaigns/${id}`);
export const updateCampaign = (id: string, input: UpdateCampaignRequest) =>
  auth<CampaignV2>(`/api/partner/campaigns/${id}`, { method: 'PATCH', body: JSON.stringify(input) });
export const submitCampaign = (id: string) => auth<CampaignV2>(`/api/partner/campaigns/${id}/submit`, { method: 'POST' });
export const pauseCampaign = (id: string) => auth<CampaignV2>(`/api/partner/campaigns/${id}/pause`, { method: 'POST' });
export const resumeCampaign = (id: string) => auth<CampaignV2>(`/api/partner/campaigns/${id}/resume`, { method: 'POST' });
export const endCampaign = (id: string) => auth<CampaignV2>(`/api/partner/campaigns/${id}/end`, { method: 'POST' });
export const getCampaignMetrics = (id: string, from?: string, to?: string) => {
  const qs = new URLSearchParams();
  if (from) qs.set('from', from);
  if (to) qs.set('to', to);
  const suffix = qs.size ? `?${qs.toString()}` : '';
  return auth<CampaignMetrics>(`/api/partner/campaigns/${id}/metrics${suffix}`);
};

// ---- business profile (v2) ----
export const getPartnerProfileV2 = () => auth<PartnerProfileV2>('/api/partner/profile');
export const updatePartnerProfileV2 = (input: UpdatePartnerProfileRequest) =>
  auth<PartnerProfileV2>('/api/partner/profile', { method: 'PATCH', body: JSON.stringify(input) });

// ---- bookable services ----
export const listPartnerServices = () => auth<PartnerService[]>('/api/partner/services');
export const createPartnerService = (input: PartnerServiceRequest) =>
  auth<PartnerService>('/api/partner/services', { method: 'POST', body: JSON.stringify(input) });
export const updatePartnerService = (id: string, input: PartnerServiceRequest) =>
  auth<PartnerService>(`/api/partner/services/${id}`, { method: 'PUT', body: JSON.stringify(input) });

// ---- incoming bookings ----
export const listPartnerBookings = (page = 0, size = 30) =>
  auth<components['schemas']['PageResponseBookingDto']>(`/api/partner/bookings?page=${page}&size=${size}`);
export const confirmBooking = (id: string) => auth<Booking>(`/api/partner/bookings/${id}/confirm`, { method: 'POST' });
export const completeBooking = (id: string) => auth<Booking>(`/api/partner/bookings/${id}/complete`, { method: 'POST' });
export const declinePartnerBooking = (id: string) => auth<Booking>(`/api/bookings/${id}/cancel`, { method: 'POST' });

// ---- media upload (partner token, field: file) ----
export async function uploadPartnerMedia(file: File): Promise<components['schemas']['MediaDto']> {
  const form = new FormData();
  form.append('file', file);
  const base = import.meta.env.VITE_API_BASE_URL ?? '';
  const t = token();
  const res = await fetch(`${base}/api/media`, { method: 'POST', headers: t ? { Authorization: `Bearer ${t}` } : {}, body: form });
  if (!res.ok) throw new Error(`Upload failed (${res.status})`);
  return res.json();
}
