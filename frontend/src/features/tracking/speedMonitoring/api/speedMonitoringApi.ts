import {api} from '../../../../api/client';
import type {EpisodeFilters,RuleFilters,SpeedEpisode,SpeedEpisodePage,SpeedRule,SpeedRuleInput,SpeedRulePage,SpeedState,SpeedStatePage,StateFilters} from '../types';
const root='/v1/tracking/speed-monitoring';
export const speedMonitoringApi={
 rules:async(filters:RuleFilters)=>(await api.get<SpeedRulePage>(`${root}/rules`,{params:filters})).data,
 rule:async(id:string)=>(await api.get<SpeedRule>(`${root}/rules/${id}`)).data,
 createRule:async({input,key}:{input:SpeedRuleInput;key:string})=>(await api.post<SpeedRule>(`${root}/rules`,input,{headers:{'Idempotency-Key':key}})).data,
 updateRule:async({rule,input}:{rule:SpeedRule;input:Omit<SpeedRuleInput,'scope'>})=>(await api.put<SpeedRule>(`${root}/rules/${rule.id}`,{...input,expectedVersion:rule.version})).data,
 command:async({rule,action,reason,key}:{rule:SpeedRule;action:'activate'|'disable'|'retire';reason?:string;key:string})=>(await api.post<SpeedRule>(`${root}/rules/${rule.id}/${action}`,reason===undefined?{expectedVersion:rule.version}:{expectedVersion:rule.version,reason},{headers:{'Idempotency-Key':key}})).data,
 states:async(filters:StateFilters)=>(await api.get<SpeedStatePage>(`${root}/states`,{params:filters})).data,
 state:async(vehicleId:string)=>(await api.get<SpeedState>(`${root}/states/${vehicleId}`)).data,
 episodes:async(filters:EpisodeFilters)=>(await api.get<SpeedEpisodePage>(`${root}/episodes`,{params:filters})).data,
 episode:async(id:string)=>(await api.get<SpeedEpisode>(`${root}/episodes/${id}`)).data,
};
