import { apiClient } from './client';
import type { CompanyDetail, CompanyFilters, CompanySummary, ScoreChange, WhyInteresting } from '@/types';

export const companyApi = {
  search: async (query: string): Promise<CompanySummary[]> => {
    const res = await apiClient.get<CompanySummary[]>('/companies/search', { params: { q: query } });
    return res.data;
  },

  filter: async (filters: CompanyFilters): Promise<CompanySummary[]> => {
    const res = await apiClient.get<CompanySummary[]>('/companies/filter', { params: filters });
    return res.data;
  },

  getById: async (id: number): Promise<CompanyDetail> => {
    const res = await apiClient.get<CompanyDetail>(`/companies/${id}`);
    return res.data;
  },

  getBySymbol: async (symbol: string): Promise<CompanyDetail> => {
    const res = await apiClient.get<CompanyDetail>(`/companies/symbol/${symbol}`);
    return res.data;
  },

  getScoreChange: async (id: number): Promise<ScoreChange> => {
    const res = await apiClient.get<ScoreChange>(`/companies/${id}/score-change`);
    return res.data;
  },

  getWhyInteresting: async (id: number): Promise<WhyInteresting> => {
    const res = await apiClient.get<WhyInteresting>(`/companies/${id}/why-interesting`);
    return res.data;
  },

  generateResearch: async (id: number) => {
    const res = await apiClient.post(`/companies/${id}/research/generate`);
    return res.data;
  },
};
