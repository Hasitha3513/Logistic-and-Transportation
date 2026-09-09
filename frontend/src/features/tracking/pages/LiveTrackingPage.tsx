import { AimOutlined, DisconnectOutlined, PlusOutlined } from '@ant-design/icons';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, App, Button, Card, Drawer, Empty, Form, Input, Modal, Space, Spin, Table, Tabs, Tag, Typography } from 'antd';
import { Controller, useForm } from 'react-hook-form';
import { useState } from 'react';
import { z } from 'zod';
import { useAuth } from '../../../auth/AuthContext';
import { trackingApi } from '../api/trackingApi';
import type { Position, TrackingDevice, TrackingState } from '../types';

const age = (timestamp?: string) => timestamp ? `${Math.max(0, Math.floor((Date.now() - Date.parse(timestamp)) / 1000))}s ago` : 'Unknown';

const deviceSchema = z.object({ externalDeviceReference:z.string().trim().min(1).max(160), providerAlias:z.string().trim().min(1).max(80), hardwareSerialReference:z.string().trim().max(160).optional() });
type DeviceForm = z.infer<typeof deviceSchema>;

export default function LiveTrackingPage({initialTab='vehicles'}:{initialTab?:'vehicles'|'devices'}) {
  const { hasPermission } = useAuth();
  return <Tabs defaultActiveKey={initialTab} items={[
    { key:'vehicles', label:'Vehicle positions', children:<VehiclePositions canReadHistory={hasPermission('TRACKING_HISTORY_VIEW')} /> },
    { key:'devices', label:'Tracking devices', children:<Devices canManage={hasPermission('TRACKING_DEVICE_MANAGE')} /> },
  ]} />;
}

function VehiclePositions({ canReadHistory }:{canReadHistory:boolean}) {
  const [historyVehicle,setHistoryVehicle]=useState<string>();
  const query = useQuery({ queryKey: ['tracking', 'vehicles'], queryFn: trackingApi.vehicles, refetchInterval: () => document.hidden || !navigator.onLine ? false : 15_000, refetchOnWindowFocus: true, retryDelay: attempt => Math.min(60_000, attempt === 0 ? 30_000 : 60_000) });
  const history = useQuery({ queryKey:['tracking','history',historyVehicle], queryFn:()=>trackingApi.history(historyVehicle!), enabled:Boolean(historyVehicle) });
  if (query.isLoading) return <Spin aria-label="Loading live tracking" />;
  return <Space direction="vertical" size="large" style={{ width: '100%' }}>
    <Typography.Paragraph type="secondary">Provider-neutral current and last-known Vehicle positions. Location state is refreshed every 15 seconds while this tab is visible.</Typography.Paragraph>
    {!navigator.onLine && <Alert type="warning" showIcon message="Browser offline" description="Showing the most recently loaded tracking state." />}
    {query.isError && <Alert type="error" showIcon message="Tracking refresh failed" description="The next refresh uses bounded backoff; displayed points may be stale." />}
    {!query.data?.length ? <Empty description="No tracked vehicles" /> : <Table<TrackingState> rowKey="vehicleId" pagination={false} dataSource={query.data} columns={[
      { title:'Vehicle', dataIndex:'vehicleId', render:value=><Typography.Text copyable>{value}</Typography.Text> },
      { title:'State', render:(_,row)=> <Space><Tag icon={row.connectivity==='OFFLINE'?<DisconnectOutlined/>:<AimOutlined/>}>{row.freshness}</Tag><Tag>{row.connectivity}</Tag><Tag>{row.latestTrusted?.trust ?? 'UNKNOWN'}</Tag></Space> },
      { title:'Position', render:(_,row)=> { const p=row.latestTrusted; return p ? <Card size="small" aria-label={`${row.freshness==='LIVE'?'Current':'Last known'} position`}><strong>{row.freshness==='LIVE'?'CURRENT':'LAST KNOWN'}</strong><br/>{p.latitude}, {p.longitude}</Card> : 'No trusted position'; } },
      { title:'Source time / age', render:(_,row)=> <>{row.latestTrusted?.sourceTimestamp ?? 'Unknown'}<br/><Typography.Text type="secondary">{age(row.latestTrusted?.sourceTimestamp)}</Typography.Text></> },
      { title:'Accuracy', render:(_,row)=> {const accuracy=row.latestTrusted?.horizontalAccuracyMeters??row.latestTrusted?.accuracyMeters;return accuracy==null?'UNKNOWN':`±${accuracy} m`;} },
      ...(canReadHistory ? [{ title:'History', render:(_:unknown,row:TrackingState)=><Button onClick={()=>setHistoryVehicle(row.vehicleId)}>Last 24 hours</Button> }] : []),
    ]} />}
    <Drawer title="Bounded position history (last 24 hours)" width={760} open={Boolean(historyVehicle)} onClose={()=>setHistoryVehicle(undefined)}>
      <Table<Position> rowKey="id" loading={history.isLoading} pagination={false} dataSource={history.data?.items??[]} columns={[{title:'Source time',dataIndex:'sourceTimestamp'},{title:'Coordinates',render:(_,p)=>`${p.latitude}, ${p.longitude}`},{title:'Trust',dataIndex:'trust'},{title:'Ordering',dataIndex:'ordering'}]} />
    </Drawer>
  </Space>;
}

function Devices({canManage}:{canManage:boolean}) {
  const {message}=App.useApp();const cache=useQueryClient();const [open,setOpen]=useState(false);const [association,setAssociation]=useState<TrackingDevice>();const [vehicleId,setVehicleId]=useState('');
  const query=useQuery({queryKey:['tracking','devices'],queryFn:trackingApi.devices});
  const form=useForm<DeviceForm>({resolver:zodResolver(deviceSchema),defaultValues:{externalDeviceReference:'',providerAlias:'',hardwareSerialReference:''}});
  const refresh=()=>cache.invalidateQueries({queryKey:['tracking','devices']});
  const create=useMutation({mutationFn:trackingApi.createDevice,onSuccess:()=>{message.success('Tracking device created');setOpen(false);form.reset();refresh();}});
  const lifecycle=useMutation({mutationFn:({device,action}:{device:TrackingDevice;action:'activate'|'disable'})=>trackingApi.lifecycle(device,action),onSuccess:()=>{message.success('Device lifecycle updated');refresh();}});
  const associate=useMutation({mutationFn:()=>trackingApi.associate(association!.id,vehicleId),onSuccess:()=>{message.success('Vehicle associated');setAssociation(undefined);setVehicleId('');}});
  return <Space direction="vertical" size="large" style={{width:'100%'}}>
    <Space style={{justifyContent:'space-between',width:'100%'}}><Typography.Paragraph type="secondary">Provider references are masked unless you have device-management permission. Credentials are never displayed.</Typography.Paragraph>{canManage&&<Button type="primary" icon={<PlusOutlined/>} onClick={()=>setOpen(true)}>Register device</Button>}</Space>
    <Table<TrackingDevice> rowKey="id" loading={query.isLoading} dataSource={query.data??[]} pagination={false} columns={[{title:'External reference',dataIndex:'externalReference'},{title:'Provider',dataIndex:'providerAlias'},{title:'Lifecycle',dataIndex:'lifecycle',render:value=><Tag>{value}</Tag>},{title:'Last receipt',dataIndex:'lastSeenAt',render:value=>value??'Never'},...(canManage?[{title:'Actions',render:(_:unknown,device:TrackingDevice)=><Space><Button onClick={()=>lifecycle.mutate({device,action:device.lifecycle==='ACTIVE'?'disable':'activate'})}>{device.lifecycle==='ACTIVE'?'Disable':'Activate'}</Button><Button onClick={()=>setAssociation(device)}>Associate Vehicle</Button></Space>}]:[])]} />
    <Modal title="Register tracking device" open={open} onCancel={()=>setOpen(false)} onOk={form.handleSubmit(value=>create.mutate(value))} confirmLoading={create.isPending}><Form layout="vertical"><Form.Item label="External device reference" validateStatus={form.formState.errors.externalDeviceReference?'error':''} help={form.formState.errors.externalDeviceReference?.message}><Controller control={form.control} name="externalDeviceReference" render={({field})=><Input {...field}/>} /></Form.Item><Form.Item label="Provider alias"><Controller control={form.control} name="providerAlias" render={({field})=><Input {...field}/>} /></Form.Item><Form.Item label="Hardware serial reference"><Controller control={form.control} name="hardwareSerialReference" render={({field})=><Input {...field}/>} /></Form.Item></Form></Modal>
    <Modal title="Associate Vehicle" open={Boolean(association)} onCancel={()=>setAssociation(undefined)} onOk={()=>associate.mutate()} okButtonProps={{disabled:!vehicleId}} confirmLoading={associate.isPending}><Form.Item label="Vehicle ID"><Input aria-label="Vehicle ID" value={vehicleId} onChange={event=>setVehicleId(event.target.value)} /></Form.Item></Modal>
  </Space>;
}
