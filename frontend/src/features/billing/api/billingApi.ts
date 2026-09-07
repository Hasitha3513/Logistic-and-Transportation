import {api} from '../../../api/client';
import type {BillingHistory,BillingRecord} from '../types';
const idempotency=()=>({'Idempotency-Key':crypto.randomUUID()});
export const billingApi={
 list:async()=>(await api.get<BillingRecord[]>('/v1/billing/records')).data,
 get:async(id:string)=>(await api.get<BillingRecord>(`/v1/billing/records/${id}`)).data,
 create:async(value:Record<string,unknown>)=>(await api.post<BillingRecord>('/v1/billing/records',value,{headers:idempotency()})).data,
 replace:async(id:string,value:Record<string,unknown>)=>(await api.put<BillingRecord>(`/v1/billing/records/${id}/lines`,value)).data,
 command:async(id:string,action:'validate'|'approve'|'cancel'|'finalize'|'reversals'|'export',value:Record<string,unknown>)=>(await api.post<BillingRecord>(`/v1/billing/records/${id}/${action}`,value,{headers:idempotency()})).data,
 history:async(id:string)=>(await api.get<BillingHistory[]>(`/v1/billing/records/${id}/history`)).data,
};
