import {useEffect,useRef} from 'react';
import {useQuery,useQueryClient} from '@tanstack/react-query';
import {idleMonitoringApi} from './api';
import type {EpisodeFilters,StateFilters} from './types';

export const idleKeys={all:['tracking','idle-monitoring'] as const,states:(session:string,filters:StateFilters)=>[...idleKeys.all,session,'states',filters] as const,episodes:(session:string,filters:EpisodeFilters)=>[...idleKeys.all,session,'episodes',filters] as const,episode:(session:string,id:string)=>[...idleKeys.all,session,'episode',id] as const,evidence:(session:string,id:string,cursor?:string)=>[...idleKeys.all,session,'evidence',id,cursor??'first'] as const};
export function useIdleSession(session?:string){const client=useQueryClient(),previous=useRef(session);useEffect(()=>{if(previous.current&&previous.current!==session)client.removeQueries({queryKey:idleKeys.all});previous.current=session},[client,session])}
export const useIdleStates=(session:string,filters:StateFilters,enabled:boolean)=>useQuery({queryKey:idleKeys.states(session,filters),queryFn:({signal})=>idleMonitoringApi.states(filters,signal),enabled,retry:false});
export const useIdleEpisodes=(session:string,filters:EpisodeFilters|undefined,enabled:boolean)=>useQuery({queryKey:idleKeys.episodes(session,filters??{from:'',to:'',limit:50}),queryFn:({signal})=>idleMonitoringApi.episodes(filters!,signal),enabled:enabled&&Boolean(filters),retry:false});
export const useIdleEpisode=(session:string,id?:string,enabled=true)=>useQuery({queryKey:idleKeys.episode(session,id??''),queryFn:({signal})=>idleMonitoringApi.episode(id!,signal),enabled:enabled&&Boolean(id),retry:false});
export const useIdleEvidence=(session:string,id?:string,cursor?:string,enabled=true)=>useQuery({queryKey:idleKeys.evidence(session,id??'',cursor),queryFn:({signal})=>idleMonitoringApi.evidence(id!,cursor,signal),enabled:enabled&&Boolean(id),retry:false});
