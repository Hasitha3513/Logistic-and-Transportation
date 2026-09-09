import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Drawer, Input, InputNumber, Select } from 'antd';
import axios from 'axios';
import { Controller, useForm } from 'react-hook-form';
import type { ProviderConnectionInput, ProviderConnectionUpdateInput, TrackingProviderConnection, TrackingProviderType } from '../types';
import { providerConnectionSchema, type ProviderConnectionFormValues } from '../validation/providerConnectionSchema';

interface Props { open:boolean; connection?:TrackingProviderConnection; providerTypes:TrackingProviderType[]; loading:boolean; onClose:()=>void; onCreate:(value:ProviderConnectionInput)=>Promise<void>; onUpdate:(id:string,value:ProviderConnectionUpdateInput)=>Promise<void> }
type ApiError={code?:string;message?:string;fieldErrors?:Array<{field:string;message:string}>};
const initial=(connection?:TrackingProviderConnection):ProviderConnectionFormValues=>({providerType:connection?.providerType??'',displayName:connection?.displayName??'',providerAlias:connection?.providerAlias??'',providerKeyId:connection?'preserved-by-backend':'',endpointUri:connection?.endpointUri??'',credentialReference:'',pollIntervalSeconds:connection?.pollIntervalSeconds??5,pageSize:connection?.pageSize??500,safeConfigurationText:JSON.stringify(connection?.safeConfiguration??{},null,2)});

export function ProviderConnectionEditor({open,connection,providerTypes,loading,onClose,onCreate,onUpdate}:Props){
 const form=useForm<ProviderConnectionFormValues>({resolver:zodResolver(providerConnectionSchema),values:initial(connection)});
 const submit=form.handleSubmit(async value=>{
  if(!connection&&!value.credentialReference){form.setError('credentialReference',{message:'Credential reference is required'});return;}
  const safeConfiguration=JSON.parse(value.safeConfigurationText||'{}') as Record<string,string>;
  try {
   if(connection) await onUpdate(connection.id,{displayName:value.displayName,endpointUri:value.endpointUri||undefined,safeConfiguration,credentialReference:value.credentialReference||undefined,pollIntervalSeconds:value.pollIntervalSeconds,pageSize:value.pageSize,version:connection.version});
   else await onCreate({providerType:value.providerType,displayName:value.displayName,providerAlias:value.providerAlias,providerKeyId:value.providerKeyId,endpointUri:value.endpointUri||undefined,safeConfiguration,credentialReference:value.credentialReference,pollIntervalSeconds:value.pollIntervalSeconds,pageSize:value.pageSize});
  } catch(error){const body=axios.isAxiosError<ApiError>(error)?error.response?.data:undefined;body?.fieldErrors?.forEach(item=>{if(item.field in form.getValues())form.setError(item.field as keyof ProviderConnectionFormValues,{message:item.message});});if(body?.code==='TRACKING_STALE_VERSION')form.setError('root',{message:'This connection changed elsewhere. Close and reopen it to load the latest version.'});else form.setError('root',{message:body?.message??'The provider connection could not be saved.'});}
 });
 const field=(name:keyof ProviderConnectionFormValues,label:string,node:React.ReactNode,help?:string)=><div className="resource-editor-field"><label htmlFor={`provider-${name}`}>{label}</label>{node}{help&&<small>{help}</small>}{form.formState.errors[name]&&<span role="alert" className="resource-editor-error">{form.formState.errors[name]?.message}</span>}</div>;
 return <Drawer title={connection?'Edit provider connection':'Add Provider Connection'} open={open} onClose={onClose} width={640} destroyOnClose extra={<button className="ant-btn ant-btn-primary" disabled={loading} onClick={()=>void submit()}>{loading?'Saving…':'Save'}</button>}>
  <form className="resource-editor-form" onSubmit={event=>void submit(event)}>
   {form.formState.errors.root&&<Alert type="error" showIcon message={form.formState.errors.root.message}/>} 
   {field('providerType','Provider type',<Controller name="providerType" control={form.control} render={({field:input})=><Select id="provider-providerType" {...input} disabled={Boolean(connection)} options={providerTypes.filter(item=>item.supported).map(item=>({value:item.providerType,label:item.providerType}))}/>}/>)}
   {field('displayName','Display name',<Controller name="displayName" control={form.control} render={({field:input})=><Input id="provider-displayName" {...input}/>}/>)}
   {!connection&&field('providerAlias','Provider alias',<Controller name="providerAlias" control={form.control} render={({field:input})=><Input id="provider-providerAlias" {...input}/>}/>)}
   {!connection&&field('providerKeyId','Provider key ID',<Controller name="providerKeyId" control={form.control} render={({field:input})=><Input id="provider-providerKeyId" {...input}/>}/>)}
   {field('endpointUri','Endpoint URI',<Controller name="endpointUri" control={form.control} render={({field:input})=><Input id="provider-endpointUri" {...input} placeholder="https://..."/>}/>, 'Backend provider validation remains authoritative.')}
   {field('credentialReference',connection?'Replacement credential reference':'Credential reference',<Controller name="credentialReference" control={form.control} render={({field:input})=><Input.Password id="provider-credentialReference" {...input} autoComplete="new-password"/>}/>,connection?'Leave blank to preserve the configured credential. Existing references are never displayed.':'Enter an opaque resolver reference, not a provider token.')}
   {field('pollIntervalSeconds','Poll interval (seconds)',<Controller name="pollIntervalSeconds" control={form.control} render={({field:input})=><InputNumber id="provider-pollIntervalSeconds" min={5} max={86400} value={input.value} onChange={input.onChange} style={{width:'100%'}}/>}/>)}
   {field('pageSize','Page size',<Controller name="pageSize" control={form.control} render={({field:input})=><InputNumber id="provider-pageSize" min={1} max={500} value={input.value} onChange={input.onChange} style={{width:'100%'}}/>}/>)}
   {field('safeConfigurationText','Safe configuration (JSON)',<Controller name="safeConfigurationText" control={form.control} render={({field:input})=><Input.TextArea id="provider-safeConfigurationText" {...input} rows={6} maxLength={8192} showCount/>}/>, 'String values only; secret-like keys are rejected. Maximum 8 KiB.')}
  </form>
 </Drawer>;
}
