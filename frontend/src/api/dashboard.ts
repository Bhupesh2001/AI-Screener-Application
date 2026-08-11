import { apiClient } from './client';
import type { Dashboard } from '@/types';

export const dashboardApi = {
  get: async (): Promise<Dashboard> => {
    const res = await apiClient.get<Dashboard>('/dashboard');
    return res.data;
  },
};
