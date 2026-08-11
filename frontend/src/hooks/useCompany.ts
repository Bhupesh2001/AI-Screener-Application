import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { companyApi } from '@/api/company';
import type { CompanyFilters } from '@/types';

export function useCompanySearch(query: string) {
  return useQuery({
    queryKey: ['companies', 'search', query],
    queryFn: () => companyApi.search(query),
    enabled: query.length > 0,
  });
}

export function useCompanyFilter(filters: CompanyFilters, enabled = true) {
  return useQuery({
    queryKey: ['companies', 'filter', filters],
    queryFn: () => companyApi.filter(filters),
    enabled,
  });
}

export function useCompanyDetail(id: number | undefined) {
  return useQuery({
    queryKey: ['companies', id],
    queryFn: () => companyApi.getById(id!),
    enabled: id !== undefined,
  });
}

export function useCompanyDetailBySymbol(symbol: string | undefined) {
  return useQuery({
    queryKey: ['companies', 'symbol', symbol],
    queryFn: () => companyApi.getBySymbol(symbol!),
    enabled: !!symbol,
  });
}

export function useScoreChange(id: number | undefined) {
  return useQuery({
    queryKey: ['companies', id, 'score-change'],
    queryFn: () => companyApi.getScoreChange(id!),
    enabled: id !== undefined,
  });
}

export function useWhyInteresting(id: number | undefined) {
  return useQuery({
    queryKey: ['companies', id, 'why-interesting'],
    queryFn: () => companyApi.getWhyInteresting(id!),
    enabled: id !== undefined,
  });
}

export function useGenerateResearch(companyId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => companyApi.generateResearch(companyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['companies', companyId] });
    },
  });
}
