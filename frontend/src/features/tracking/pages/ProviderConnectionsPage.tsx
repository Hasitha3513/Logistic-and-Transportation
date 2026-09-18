import { CheckCircleOutlined, EyeOutlined, PlusOutlined, ReloadOutlined, ToolOutlined } from '@ant-design/icons';
import { Alert, App as AntApp, Button, Card, Descriptions, Drawer, Empty, Flex, Modal, Space, Steps, Table, Tag, Typography } from 'antd';
import axios from 'axios';
import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import { ProviderConnectionEditor } from '../components/ProviderConnectionEditor';
import { useProviderConnection, useProviderConnectionMutations, useProviderConnections, useProviderTypes } from '../hooks/useProviderConnections';
import type { ProviderConnectionTestStatus, TrackingProviderConnection } from '../types';

type ApiError={code?:string;message?:string};
const lifecycleColor:Record<string,string>={DRAFT:'default',ACTIVE:'success',DISABLED:'warning',RETIRED:'error'};
const testLabel:Record<ProviderConnectionTestStatus,string>={NOT_TESTED:'Not tested',PASS:'Connection successful',AUTH_FAILED:'Authentication failed',UNREACHABLE:'Provider unreachable',INVALID_CONFIGURATION:'Configuration invalid'};
const when=(value?:string)=>value?new Date(value).toLocaleString():'Never';
const recovery:Record<string,{message:string;description:string}>={
 PROVIDER_RESPONSE_OVERFLOW:{message:'Provider response exceeded the governed limit',description:'The polling result was rejected without advancing its watermark. Reduce the provider-side response window or wait for the bounded retry.'},
 PROVIDER_AUTHENTICATION:{message:'Provider credentials were rejected',description:'Verify the opaque credential reference and rotate the backend-managed provider credential before retrying.'},
 PROVIDER_ENDPOINT_BLOCKED:{message:'Provider endpoint was rejected',description:'Use an approved HTTPS destination. Private Traccar destinations require deployment-administrator allowlisting.'},
 PROVIDER_REJECTED:{message:'Provider rejected the request',description:'Review the safe provider configuration and device identity before the next bounded retry.'},
 PROVIDER_UNAVAILABLE:{message:'Provider is temporarily unavailable',description:'Polling will retry with bounded backoff. Previously accepted telemetry and watermarks are preserved.'},
 PROVIDER_TRANSIENT:{message:'Provider is temporarily unavailable',description:'Polling will retry with bounded backoff. Previously accepted telemetry and watermarks are preserved.'},
 PROVIDER_RATE_LIMITED:{message:'Provider rate limit reached',description:'Polling will resume after bounded backoff without advancing the successful watermark.'},
 PROVIDER_RESPONSE_OVERSIZE:{message:'Provider response exceeded the governed limit',description:'The polling result was rejected without advancing its watermark. Reduce the provider-side response window or wait for the bounded retry.'},
 PROVIDER_PAGE_OVERSIZE:{message:'Provider response exceeded the governed limit',description:'The polling result was rejected without advancing its watermark. Reduce the provider-side response window or wait for the bounded retry.'},
 TRACKING_KAFKA_UNAVAILABLE:{message:'Telemetry publication is unavailable',description:'The provider result was not acknowledged as progress. Kafka recovery and canonical deduplication make the next bounded retry safe.'},
 TRACKING_KAFKA_ACK_TIMEOUT:{message:'Telemetry publication acknowledgement timed out',description:'Polling progress was not acknowledged. The next bounded retry uses the same canonical identity and remains duplicate-safe.'},
};

export default function ProviderConnectionsPage(){
 const {hasPermission}=useAuth();const {message,notification}=AntApp.useApp();const [page,setPage]=useState(0);const [editing,setEditing]=useState<TrackingProviderConnection|'create'>();const [selectedId,setSelectedId]=useState<string>();
 const list=useProviderConnections(page,20);const types=useProviderTypes();const detail=useProviderConnection(selectedId);const actions=useProviderConnectionMutations();const pending=actions.create.isPending||actions.update.isPending;
 if(!hasPermission('TRACKING_DEVICE_MANAGE'))return <Navigate to="/tracking/vehicles" replace/>;
 const safeError=(error:unknown,fallback:string)=>{const body=axios.isAxiosError<ApiError>(error)?error.response?.data:undefined;notification.error({message:fallback,description:body?.code==='TRACKING_STALE_VERSION'?'This connection changed elsewhere. Reload the latest data and try again.':body?.message??'The backend safely rejected this operation.'});};
 const lifecycle=(connection:TrackingProviderConnection,action:'activate'|'disable'|'retire')=>{
  const retire=action==='retire';Modal.confirm({title:retire?'Retire provider connection?':action==='disable'?'Disable provider connection?':'Activate provider connection?',content:retire?'This action is permanent and stops future polling for this connection. Historical Tracking data is preserved.':action==='disable'?'Disabling stops polling until reactivated. Historical data remains available.':'The connection becomes eligible for polling on the next coordinator cycle.',okText:retire?'Retire':action==='disable'?'Disable':'Activate',okButtonProps:{danger:retire},onOk:async()=>{try{await actions.lifecycle.mutateAsync({connection,action});void message.success(`Provider connection ${action}d`);}catch(error){safeError(error,'Lifecycle action failed');}}});
 };
 const runTest=async(connection:TrackingProviderConnection)=>{try{const result=await actions.test.mutateAsync(connection.id);const label=testLabel[result.status];if(result.status==='PASS')void message.success(label);else notification.warning({message:label,description:'Review the safe configuration and credential setup. No telemetry was ingested.'});}catch(error){safeError(error,'Connection test failed');}};
 const selected=detail.data;const capabilities=(types.data??[]).find(item=>item.providerType===selected?.providerType)?.capabilities??[];
 const recoveryAction=selected?.lastErrorCategory?recovery[selected.lastErrorCategory]:undefined;
 const onboarding=[{title:'Connection saved',description:'Safe settings and opaque credential reference'},{title:'Connection verified',description:'Access check only; no telemetry implied'},{title:'Device bound',description:'Register or select identity and bind it'},{title:'Vehicle associated',description:'Same-Tenant effective association'},{title:'Polling enabled',description:'Activate connection and device'},{title:'Telemetry received',description:'Confirm a real last-receipt timestamp'}];
 const columns=[
  {title:'Display name',dataIndex:'displayName'}, {title:'Provider type',dataIndex:'providerType'}, {title:'Provider alias',dataIndex:'providerAlias'},
  {title:'Lifecycle',dataIndex:'lifecycle',render:(value:string)=><Tag color={lifecycleColor[value]}>{value}</Tag>},
  {title:'Test status',dataIndex:'testStatus',render:(value:ProviderConnectionTestStatus)=><Tag color={value==='PASS'?'success':value==='NOT_TESTED'?'default':'warning'}>{testLabel[value]}</Tag>},
  {title:'Credential',dataIndex:'credentialConfigured',render:(value:boolean)=>value?<Tag color="success">Configured</Tag>:<Tag>Not configured</Tag>},
  {title:'Poll interval',dataIndex:'pollIntervalSeconds',render:(value:number)=>`${value}s`}, {title:'Last tested',dataIndex:'lastTestedAt',render:when},
  {title:'Actions',fixed:'right' as const,render:(_:unknown,item:TrackingProviderConnection)=><Space wrap>
   <Button size="small" icon={<EyeOutlined/>} onClick={()=>setSelectedId(item.id)}>Details</Button>
   {item.lifecycle!=='RETIRED'&&<Button size="small" onClick={()=>setEditing(item)}>Edit</Button>}
   {item.lifecycle!=='RETIRED'&&<Button size="small" icon={<ToolOutlined/>} loading={actions.test.isPending&&actions.test.variables===item.id} onClick={()=>void runTest(item)}>Test Connection</Button>}
   {(item.lifecycle==='DRAFT'||item.lifecycle==='DISABLED')&&<Button size="small" onClick={()=>lifecycle(item,'activate')}>Activate</Button>}
   {item.lifecycle==='ACTIVE'&&<Button size="small" onClick={()=>lifecycle(item,'disable')}>Disable</Button>}
   {item.lifecycle!=='RETIRED'&&<Button size="small" danger onClick={()=>lifecycle(item,'retire')}>Retire</Button>}
  </Space>},
 ];
 return <Flex vertical gap={16}>
  <Card title="Guided provider onboarding"><Steps responsive current={-1} items={onboarding}/><Typography.Paragraph type="secondary" style={{marginTop:16}}>Complete each state explicitly. A saved or successfully tested connection is not evidence that polling is enabled or that telemetry has arrived. Device-side protocol and destination setup is still required.</Typography.Paragraph><Space wrap><Tag color="blue">Flespi Cloud</Tag><Tag color="purple">Traccar HTTPS</Tag><Tag>Generic signed HMAC</Tag><Typography.Text type="secondary">Generic ingress remains available through its governed signed endpoint; it is not a polling connection and is not replaced by this workflow.</Typography.Text></Space></Card>
  <Flex justify="space-between" align="center" gap={16} wrap><Typography.Text type="secondary">Manage installed provider connections. Test Connection validates access only; it does not activate polling, create devices, or ingest telemetry.</Typography.Text><Space><Button icon={<ReloadOutlined/>} loading={list.isFetching} onClick={()=>void list.refetch()}>Refresh</Button><Button type="primary" icon={<PlusOutlined/>} onClick={()=>setEditing('create')}>Add Provider Connection</Button></Space></Flex>
  {list.isError&&<Alert type="error" showIcon message="Provider connections could not be loaded" description="Check your access and backend connection, then retry." action={<Button onClick={()=>void list.refetch()}>Retry</Button>}/>} 
  <Table<TrackingProviderConnection> rowKey="id" loading={list.isLoading} dataSource={list.data?.items??[]} columns={columns} scroll={{x:1180}} locale={{emptyText:<Empty description="No provider connections configured"><Button type="primary" onClick={()=>setEditing('create')}>Add Provider Connection</Button></Empty>}} pagination={{current:page+1,pageSize:20,total:list.data?.total??0,showSizeChanger:false,onChange:value=>setPage(value-1)}}/>
  {editing&&<ProviderConnectionEditor open connection={editing==='create'?undefined:editing} providerTypes={types.data??[]} loading={pending} onClose={()=>setEditing(undefined)} onCreate={async input=>{await actions.create.mutateAsync(input);void message.success('Draft provider connection created');setEditing(undefined);}} onUpdate={async(id,input)=>{await actions.update.mutateAsync({id,input});void message.success('Provider connection updated');setEditing(undefined);}}/>}
  <Drawer title="Provider connection details" width={720} open={Boolean(selectedId)} onClose={()=>setSelectedId(undefined)}>
   {detail.isError?<Alert type="error" showIcon message="Provider connection unavailable" description="It may not exist in your Tenant, or your access changed."/>:!selected?<Typography.Text>Loading safe details…</Typography.Text>:<Space direction="vertical" size="large" style={{width:'100%'}}>
    <Steps direction="vertical" size="small" current={selected.lastProviderMessageAt?5:selected.lifecycle==='ACTIVE'?4:2} items={onboarding.map((item,index)=>({...item,status:index===1&&selected.testStatus!=='PASS'?'error':undefined}))}/>
    <Descriptions bordered size="small" column={1} items={[{key:'name',label:'Display name',children:selected.displayName},{key:'type',label:'Provider type',children:selected.providerType},{key:'alias',label:'Provider alias',children:selected.providerAlias},{key:'lifecycle',label:'Polling eligibility',children:selected.lifecycle==='ACTIVE'?<Tag color="success">Enabled</Tag>:<Tag color={lifecycleColor[selected.lifecycle]}>Not enabled · {selected.lifecycle}</Tag>},{key:'test',label:'Connection verification',children:testLabel[selected.testStatus]},{key:'credential',label:'Credential reference',children:selected.credentialConfigured?'Opaque reference configured':'Not configured'},{key:'endpoint',label:'Endpoint',children:selected.endpointUri??'Provider default'},{key:'poll',label:'Poll interval',children:`${selected.pollIntervalSeconds} seconds`},{key:'page',label:'Page size',children:selected.pageSize},{key:'tested',label:'Last verified',children:when(selected.lastTestedAt)},{key:'pollSuccess',label:'Last successful poll',children:when(selected.lastSuccessfulPollAt)},{key:'message',label:'Telemetry received',children:selected.lastProviderMessageAt?when(selected.lastProviderMessageAt):<Tag color="warning">No telemetry received</Tag>},{key:'error',label:'Health / error category',children:selected.lastErrorCategory??'None'}]}/>
    {selected.providerType==='TRACCAR'&&<Alert type="info" showIcon message="Private Traccar endpoints are deployment-managed" description="If this endpoint is self-hosted or private, ask a deployment administrator to approve its exact destination and trusted CA. Tenant users cannot edit the allowlist here."/>}
    {recoveryAction&&<Alert type="warning" showIcon message={recoveryAction.message} description={recoveryAction.description}/>}
    {!selected.lastProviderMessageAt&&<Alert type="warning" showIcon message="Telemetry has not been received" description="Verify the device-side provider destination and identity, bind the device to a Vehicle, activate both records, then wait for the next bounded poll."/>}
    <div><Typography.Text strong>Capabilities</Typography.Text><div><Space wrap>{capabilities.length?capabilities.map(value=><Tag key={value}>{value}</Tag>):<Typography.Text type="secondary">None reported</Typography.Text>}</Space></div></div>
    <div><Typography.Text strong>Safe configuration</Typography.Text><pre>{JSON.stringify(selected.safeConfiguration,null,2)}</pre></div>
    {capabilities.includes('DISCOVERY')&&selected.lifecycle!=='RETIRED'&&<Button icon={<CheckCircleOutlined/>} loading={actions.discover.isPending} onClick={()=>void actions.discover.mutateAsync({id:selected.id}).then(result=>Modal.info({title:'Discovered devices',content:result.devices.length?result.devices.map(device=><div key={device.maskedExternalDeviceReference}>{device.displayName??'Device'} — {device.maskedExternalDeviceReference}</div>):'No devices discovered. Discovery does not onboard devices.'})).catch(error=>safeError(error,'Device discovery failed'))}>Discover Devices</Button>}
    <Link to="/tracking/devices"><Button type="primary">Continue to Device and Vehicle binding</Button></Link>
   </Space>}
  </Drawer>
 </Flex>;
}
