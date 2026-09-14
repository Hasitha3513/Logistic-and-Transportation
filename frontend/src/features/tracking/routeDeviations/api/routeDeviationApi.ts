import {api} from '../../../../api/client';
import type {DeviationEpisode,DeviationEpisodePage,DeviationReview,DeviationReviewPage,DeviationRule,DeviationRuleInput,DeviationRulePage,DeviationStatePage,DeviationVehicleState,EpisodeFilters,ReviewInput,RuleFilters,StateFilters} from '../types';
const root='/v1/tracking/route-deviations';
const key=()=>crypto.randomUUID();
export const routeDeviationApi={
 rules:async(filters:RuleFilters)=>(await api.get<DeviationRulePage>(`${root}/rules`,{params:filters})).data,
 rule:async(id:string)=>(await api.get<DeviationRule>(`${root}/rules/${id}`)).data,
 createRule:async({input,idempotencyKey}:{input:DeviationRuleInput;idempotencyKey:string})=>(await api.post<DeviationRule>(`${root}/rules`,input,{headers:{'Idempotency-Key':idempotencyKey}})).data,
 updateRule:async({rule,input}:{rule:DeviationRule;input:Pick<DeviationRuleInput,'toleranceMeters'>})=>(await api.put<DeviationRule>(`${root}/rules/${rule.id}`,{expectedVersion:rule.version,...input},{headers:{'Idempotency-Key':key()}})).data,
 command:async({rule,action,reason}:{rule:DeviationRule;action:'activate'|'disable'|'retire';reason?:string})=>(await api.post<DeviationRule>(`${root}/rules/${rule.id}/${action}`,reason===undefined?{expectedVersion:rule.version}:{expectedVersion:rule.version,reason},{headers:{'Idempotency-Key':key()}})).data,
 states:async(filters:StateFilters)=>(await api.get<DeviationStatePage>(`${root}/states`,{params:filters})).data,
 state:async(vehicleId:string)=>(await api.get<DeviationVehicleState>(`${root}/states/${vehicleId}`)).data,
 episodes:async(filters:EpisodeFilters)=>(await api.get<DeviationEpisodePage>(`${root}/episodes`,{params:filters})).data,
 episode:async(id:string)=>(await api.get<DeviationEpisode>(`${root}/episodes/${id}`)).data,
 reviews:async(id:string)=>(await api.get<DeviationReviewPage>(`${root}/episodes/${id}/reviews`,{params:{limit:100}})).data,
 review:async({episode,input,expectedVersion}:{episode:DeviationEpisode;input:ReviewInput;expectedVersion:number})=>(await api.post<DeviationReview>(`${root}/episodes/${episode.id}/${input.status==='APPROVED'?'approve':'reject'}`,{expectedVersion,reason:input.reason,note:input.note},{headers:{'Idempotency-Key':key()}})).data,
 correctReview:async({episode,input,expectedVersion}:{episode:DeviationEpisode;input:ReviewInput;expectedVersion:number})=>(await api.post<DeviationReview>(`${root}/episodes/${episode.id}/correct-review`,{expectedVersion,status:input.status,reason:input.reason,note:input.note},{headers:{'Idempotency-Key':key()}})).data,
};
