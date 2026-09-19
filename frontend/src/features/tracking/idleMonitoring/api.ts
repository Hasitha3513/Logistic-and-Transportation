import {api} from '../../../api/client';
import type {CursorPage,EpisodeFilters,IdleEpisode,IdleEvidence,IdleStateView,StateFilters} from './types';

const root='/v1/tracking/idle-monitoring';
export const idleMonitoringApi={
  states:async(filters:StateFilters,signal?:AbortSignal)=>(await api.get<CursorPage<IdleStateView>>(`${root}/states`,{params:filters,signal})).data,
  episodes:async(filters:EpisodeFilters,signal?:AbortSignal)=>(await api.get<CursorPage<IdleEpisode>>(`${root}/episodes`,{params:filters,signal})).data,
  episode:async(id:string,signal?:AbortSignal)=>(await api.get<IdleEpisode>(`${root}/episodes/${id}`,{signal})).data,
  evidence:async(id:string,cursor?:string,signal?:AbortSignal)=>(await api.get<CursorPage<IdleEvidence>>(`${root}/episodes/${id}/evidence`,{params:{cursor,limit:100},signal})).data,
};
