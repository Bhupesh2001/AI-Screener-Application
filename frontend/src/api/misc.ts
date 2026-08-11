import { apiClient } from './client';
import type { CompanySummary, EventItem, SectorSummary, Settings } from '@/types';

export const watchlistApi = {
  get: async (): Promise<CompanySummary[]> => {
    const res = await apiClient.get<CompanySummary[]>('/watchlist');
    return res.data;
  },
  add: async (companyId: number, note?: string) => {
    await apiClient.post(`/watchlist/${companyId}`, note ? { note } : {});
  },
  remove: async (companyId: number) => {
    await apiClient.delete(`/watchlist/${companyId}`);
  },
};

export const eventsApi = {
  getRecent: async (type?: string, limit = 50): Promise<EventItem[]> => {
    const res = await apiClient.get<EventItem[]>('/events', { params: { type, limit } });
    return res.data;
  },
  getForCompany: async (companyId: number): Promise<EventItem[]> => {
    const res = await apiClient.get<EventItem[]>(`/events/company/${companyId}`);
    return res.data;
  },
};

export const sectorsApi = {
  getAll: async (): Promise<SectorSummary[]> => {
    const res = await apiClient.get<SectorSummary[]>('/sectors');
    return res.data;
  },
  getOne: async (sector: string): Promise<SectorSummary> => {
    const res = await apiClient.get<SectorSummary>(`/sectors/${sector}`);
    return res.data;
  },
};

export const settingsApi = {
  get: async (): Promise<Settings> => {
    const res = await apiClient.get<Settings>('/settings');
    return res.data;
  },
  update: async (settings: Partial<Settings>): Promise<Settings> => {
    const res = await apiClient.put<Settings>('/settings', settings);
    return res.data;
  },
};

export const discoveryApi = {
  refresh: async (): Promise<{ totalProcessed: number; included: number; excluded: number }> => {
    const res = await apiClient.post('/discovery/refresh');
    return res.data;
  },
};
