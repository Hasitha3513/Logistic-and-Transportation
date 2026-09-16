import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { trackingDashboardApi } from './api';
import type { DashboardQuery } from './types';

export const dashboardKeys={all:['tracking','dashboard'] as const,query:(value:DashboardQuery)=>[...dashboardKeys.all,value] as const};

export function useTrackingDashboard(query:DashboardQuery, enabled:boolean){
  return useQuery({
    queryKey:dashboardKeys.query(query),
    queryFn:({signal})=>trackingDashboardApi.query(query,signal),
    enabled,
    placeholderData:keepPreviousData,
    refetchInterval:()=>document.hidden||!navigator.onLine?false:15_000,
    refetchOnWindowFocus:true,
    retry:2,
    retryDelay:attempt=>attempt===0?30_000:60_000,
  });
}
