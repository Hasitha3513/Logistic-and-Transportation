import { api } from '../../../../api/client';
import type { FuelException, FuelExceptionDetail } from '../types/fuelExceptions';
export const fuelExceptionsApi={
 list:async()=>(await api.get<FuelException[]>('/v1/fuel/exceptions')).data,
 detail:async(id:string)=>(await api.get<FuelExceptionDetail>(`/v1/fuel/exceptions/${id}`)).data,
 create:async(value:Record<string,unknown>)=>(await api.post<FuelException>('/v1/fuel/exceptions',value)).data,
 command:async(id:string,action:string,value:Record<string,unknown>)=>(await api.post<FuelException>(`/v1/fuel/exceptions/${id}/${action}`,value)).data,
 note:async(id:string,text:string)=>(await api.post(`/v1/fuel/exceptions/${id}/notes`,{text})).data,
 evidence:async(id:string,value:Record<string,unknown>)=>(await api.post(`/v1/fuel/exceptions/${id}/evidence`,value)).data,
 correction:async(id:string,value:Record<string,unknown>)=>(await api.post(`/v1/fuel/exceptions/${id}/corrections`,value)).data,
 reviewCorrection:async(id:string,correctionId:string,action:'approve'|'reject',version:number,reason:string)=>(await api.post(`/v1/fuel/exceptions/${id}/corrections/${correctionId}/${action}`,{version,reason})).data,
};
