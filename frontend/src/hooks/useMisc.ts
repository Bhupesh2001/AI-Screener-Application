import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { discoveryApi, eventsApi, sectorsApi, settingsApi } from '@/api/misc';
import type { Settings } from '@/types';

export function useRecentEvents(type?: string, limit = 50) {
  return useQuery({
    queryKey: ['events', type, limit],
    queryFn: () => eventsApi.getRecent(type, limit),
  });
}

export function useCompanyEvents(companyId: number | undefined) {
  return useQuery({
    queryKey: ['events', 'company', companyId],
    queryFn: () => eventsApi.getForCompany(companyId!),
    enabled: companyId !== undefined,
  });
}

export function useSectors() {
  return useQuery({
    queryKey: ['sectors'],
    queryFn: sectorsApi.getAll,
  });
}

export function useSector(sector: string | undefined) {
  return useQuery({
    queryKey: ['sectors', sector],
    queryFn: () => sectorsApi.getOne(sector!),
    enabled: !!sector,
  });
}

export function useSettings() {
  return useQuery({
    queryKey: ['settings'],
    queryFn: settingsApi.get,
  });
}

export function useUpdateSettings() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (settings: Partial<Settings>) => settingsApi.update(settings),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['settings'] });
    },
  });
}

export function useRefreshDiscovery() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: discoveryApi.refresh,
    onSuccess: () => {
      queryClient.invalidateQueries();
    },
  });
}
