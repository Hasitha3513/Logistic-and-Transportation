import { useMutation,useQuery,useQueryClient } from '@tanstack/react-query';
import { trackingApi } from '../api/trackingApi';
import type { GeofenceFilters,GeofenceTransitionFilters } from '../types';
export const geofenceKeys={all:['tracking','geofences'] as const,detail:(id:string)=>['tracking','geofences','detail',id] as const,memberships:['tracking','geofences','memberships'] as const,transitions:['tracking','geofences','transitions'] as const,unauthorized:['tracking','geofences','unauthorized'] as const};
export const useGeofences=(filters:GeofenceFilters)=>useQuery({queryKey:[...geofenceKeys.all,filters],queryFn:()=>trackingApi.geofences(filters)});
export const useGeofence=(id?:string)=>useQuery({queryKey:geofenceKeys.detail(id??''),queryFn:()=>trackingApi.geofence(id!),enabled:Boolean(id)});
export const useGeofenceMemberships=(filters:{vehicleId?:string;geofenceId?:string;page:number;size:number})=>useQuery({queryKey:[...geofenceKeys.memberships,filters],queryFn:()=>trackingApi.geofenceMemberships(filters)});
export const useGeofenceTransitions=(filters:GeofenceTransitionFilters)=>useQuery({queryKey:[...geofenceKeys.transitions,filters],queryFn:()=>trackingApi.geofenceTransitions(filters)});
export const useUnauthorizedTransitions=(filters:Pick<GeofenceTransitionFilters,'from'|'to'|'cursor'|'limit'>)=>useQuery({queryKey:[...geofenceKeys.unauthorized,filters],queryFn:()=>trackingApi.unauthorizedGeofenceTransitions(filters)});
export function useGeofenceMutations(){const client=useQueryClient();const refresh=(id?:string)=>{void client.invalidateQueries({queryKey:geofenceKeys.all});if(id)void client.invalidateQueries({queryKey:geofenceKeys.detail(id)});};return {create:useMutation({mutationFn:trackingApi.createGeofence,onSuccess:value=>refresh(value.id)}),update:useMutation({mutationFn:trackingApi.updateGeofence,onSuccess:value=>refresh(value.id)}),lifecycle:useMutation({mutationFn:trackingApi.geofenceLifecycle,onSuccess:value=>refresh(value.id)})};}
