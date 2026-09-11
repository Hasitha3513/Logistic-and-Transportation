import { AimOutlined, DisconnectOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Card, Drawer, Empty, Space, Spin, Table, Tabs, Tag, Typography } from 'antd';
import { useState } from 'react';
import { useAuth } from '../../../auth/AuthContext';
import { trackingApi } from '../api/trackingApi';
import { DeviceManagement } from '../components/DeviceManagement';
import type { Position, TrackingState } from '../types';

const age = (timestamp?: string) => timestamp ? `${Math.max(0, Math.floor((Date.now() - Date.parse(timestamp)) / 1000))}s ago` : 'Unknown';

export default function LiveTrackingPage({initialTab='vehicles'}:{initialTab?:'vehicles'|'devices'}) {
  const { hasPermission } = useAuth();
  return <Tabs defaultActiveKey={initialTab} items={[
    { key:'vehicles', label:'Vehicle positions', children:<VehiclePositions canReadHistory={hasPermission('TRACKING_HISTORY_VIEW')} /> },
    { key:'devices', label:'Tracking devices', children:<DeviceManagement canManage={hasPermission('TRACKING_DEVICE_MANAGE')} /> },
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
