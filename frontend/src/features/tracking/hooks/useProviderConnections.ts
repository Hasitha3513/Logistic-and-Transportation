import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { trackingApi } from '../api/trackingApi';

export const providerConnectionKeys={all:['tracking','provider-connections'] as const,types:['tracking','provider-types'] as const,detail:(id:string)=>['tracking','provider-connections',id] as const};
export const useProviderTypes=()=>useQuery({queryKey:providerConnectionKeys.types,queryFn:trackingApi.providerTypes,staleTime:300_000});
export const useProviderConnections=(page:number,size:number)=>useQuery({queryKey:[...providerConnectionKeys.all,page,size],queryFn:()=>trackingApi.providerConnections(page,size)});
export const useProviderConnection=(id?:string)=>useQuery({queryKey:providerConnectionKeys.detail(id??''),queryFn:()=>trackingApi.providerConnection(id!),enabled:Boolean(id)});
export function useProviderConnectionMutations(){
  const client=useQueryClient();
  const refresh=(id?:string)=>{void client.invalidateQueries({queryKey:providerConnectionKeys.all});if(id)void client.invalidateQueries({queryKey:providerConnectionKeys.detail(id)});};
  return {
    create:useMutation({mutationFn:trackingApi.createProviderConnection,onSuccess:value=>refresh(value.id)}),
    update:useMutation({mutationFn:trackingApi.updateProviderConnection,onSuccess:value=>refresh(value.id)}),
    test:useMutation({mutationFn:trackingApi.testProviderConnection,onSuccess:value=>refresh(value.connection.id)}),
    lifecycle:useMutation({mutationFn:trackingApi.providerConnectionLifecycle,onSuccess:value=>refresh(value.id)}),
    discover:useMutation({mutationFn:({id,cursor}:{id:string;cursor?:string})=>trackingApi.discoverProviderDevices(id,cursor)}),
  };
}
