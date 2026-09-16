import { Alert,Button,Card,Checkbox,Empty,Flex,Select,Space,Statistic,Table,Tag,Typography } from 'antd';
import axios from 'axios';
import { useEffect,useMemo,useState } from 'react';
import { Link,Navigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import { useUnreadNotificationCount } from '../../../notifications/useNotifications';
import { useTrackingDashboard } from './hooks';
import { TrackingDashboardMap } from './TrackingDashboardMap';
import type { Connectivity,DashboardQuery,DashboardVehicle,Freshness,IncidentType,Motion,ProducerStatus } from './types';
import './TrackingDashboardPage.css';

const freshness:Freshness[]=['LIVE','RECENT','STALE','UNKNOWN'];
const connectivity:Connectivity[]=['CONNECTED','DEGRADED','OFFLINE','UNKNOWN'];
const motion:Motion[]=['MOVING','STATIONARY','UNKNOWN'];
const incidentTypes:IncidentType[]=['GEOFENCE','SPEED','ROUTE_DEVIATION'];
const labels:Record<ProducerStatus,string>={ACCEPTED:'Accepted',FIELD_FIDELITY_PENDING:'Field fidelity pending',FIELD_ACCEPTANCE_PENDING:'Field acceptance pending',UNAVAILABLE:'Unavailable'};
const tag=(value:string)=><Tag>{value.replaceAll('_',' ')}</Tag>;

export default function TrackingDashboardPage(){
  const {hasPermission}=useAuth();
  const [visible,setVisible]=useState(!document.hidden),[online,setOnline]=useState(navigator.onLine);
  const [filters,setFilters]=useState<Omit<DashboardQuery,'cursor'>>({freshness:[],connectivity:[],motion:[],incidentTypes:[],includeHeatMap:true,includeIncidents:true,pageSize:100});
  useEffect(()=>{const visibility=()=>setVisible(!document.hidden),connected=()=>setOnline(true),disconnected=()=>setOnline(false);document.addEventListener('visibilitychange',visibility);window.addEventListener('online',connected);window.addEventListener('offline',disconnected);return()=>{document.removeEventListener('visibilitychange',visibility);window.removeEventListener('online',connected);window.removeEventListener('offline',disconnected)}},[]);
  const query=useTrackingDashboard(filters,hasPermission('TRACKING_DASHBOARD_VIEW')&&visible&&online);
  const unread=useUnreadNotificationCount(hasPermission('NOTIFICATION_VIEW'));
  const data=query.data;
  const update=<K extends keyof typeof filters>(key:K,value:(typeof filters)[K])=>setFilters(current=>({...current,[key]:value}));
  const columns=useMemo(()=>[
    {title:'Vehicle',dataIndex:'vehicleId',render:(value:string)=><Typography.Text copyable>{value}</Typography.Text>},
    {title:'Freshness',dataIndex:'freshness',render:tag},{title:'Connectivity',dataIndex:'connectivity',render:tag},{title:'Observed motion',dataIndex:'motion',render:tag},
    {title:'Trusted source time',render:(_:unknown,value:DashboardVehicle)=>value.latestTrusted?new Date(value.latestTrusted.sourceTimestamp).toLocaleString():'Unknown'},
    {title:'Speed',render:(_:unknown,value:DashboardVehicle)=>value.latestTrusted?.speedKph===undefined?'Unknown':`${value.latestTrusted.speedKph} km/h`},
    {title:'Active Trip',render:(_:unknown,value:DashboardVehicle)=>value.activeTrip?`${value.activeTrip.tripId} · ${value.activeTrip.lifecycle}`:'None'},
    {title:'Actions',render:(_:unknown,value:DashboardVehicle)=>value.journeyReplayAvailable?<Link to="/tracking/journey-replay">Open Journey Replay</Link>:'—'},
  ],[]);
  if(!hasPermission('TRACKING_DASHBOARD_VIEW'))return <Navigate to="/" replace/>;
  const status=query.isError?(axios.isAxiosError(query.error)&&query.error.response?.status===429?'Rate limited':'Refresh failed'):data?.sourceStatus??'Loading';
  return <Flex className="tracking-dashboard" vertical gap={16}>
    <Typography.Paragraph type="secondary">Same-Tenant operational fleet evidence. Motion is observed speed, not engine or idle state. Producer acceptance labels remain independent.</Typography.Paragraph>
    {!online&&<Alert type="warning" showIcon message="Dashboard polling paused while offline" description="Displayed evidence is retained in memory and may be stale. Reconnect, then refresh."/>}
    {!visible&&<Alert type="info" showIcon message="Dashboard polling paused while this tab is hidden"/>}
    {data?.sourceStatus==='DEGRADED'&&<Alert type="warning" showIcon message="Live source degraded" description="Fallback evidence is available and is not being represented as live."/>}
    {query.isError&&<Alert type={axios.isAxiosError(query.error)&&query.error.response?.status===429?'warning':'error'} showIcon message={status} description="Previously displayed evidence remains visible. Retry without changing producer state." action={<Button onClick={()=>void query.refetch()}>Retry now</Button>}/>}
    <Flex gap={8} wrap align="center"><Select mode="multiple" aria-label="Filter by freshness" placeholder="All freshness" value={filters.freshness} onChange={value=>update('freshness',value)} options={freshness.map(value=>({value,label:value}))}/><Select mode="multiple" aria-label="Filter by connectivity" placeholder="All connectivity" value={filters.connectivity} onChange={value=>update('connectivity',value)} options={connectivity.map(value=>({value,label:value}))}/><Select mode="multiple" aria-label="Filter by observed motion" placeholder="All motion" value={filters.motion} onChange={value=>update('motion',value)} options={motion.map(value=>({value,label:value}))}/><Select mode="multiple" aria-label="Filter by incident type" placeholder="All incident types" value={filters.incidentTypes} onChange={value=>update('incidentTypes',value)} options={incidentTypes.map(value=>({value,label:value.replaceAll('_',' ')}))}/><Checkbox checked={filters.includeHeatMap} onChange={event=>update('includeHeatMap',event.target.checked)}>Density heat layer</Checkbox><Checkbox checked={filters.includeIncidents} onChange={event=>update('includeIncidents',event.target.checked)}>Incidents</Checkbox><Button type="primary" loading={query.isFetching} onClick={()=>void query.refetch()}>Refresh</Button></Flex>
    <div className="tracking-dashboard__summary"><Card><Statistic title="Matching Vehicles" value={data?.summary.matchingVehicleCount??0}/></Card><Card><Statistic title="Live" value={data?.summary.freshnessCounts.LIVE??0}/></Card><Card><Statistic title="Offline" value={data?.summary.connectivityCounts.OFFLINE??0}/></Card><Card><Statistic title="Unread notifications" value={hasPermission('NOTIFICATION_VIEW')?(unread.data??0):'Restricted'}/></Card></div>
    <Typography.Text className="tracking-dashboard__status" aria-live="polite">{status}. Last successful refresh: {data?.lastSuccessfulRefreshAt?new Date(data.lastSuccessfulRefreshAt).toLocaleString():'Not yet available'}.</Typography.Text>
    <Table<DashboardVehicle> aria-label="Tracking dashboard Vehicle evidence" rowKey="vehicleId" loading={query.isLoading} dataSource={data?.vehicles??[]} columns={columns} scroll={{x:1100}} pagination={false} locale={{emptyText:<Empty description="No Vehicles match these filters"/>}}/>
    <div className="tracking-dashboard__workspace"><TrackingDashboardMap vehicles={data?.vehicles??[]} heatCells={filters.includeHeatMap?(data?.heatMapCells??[]):[]}/><Card title="Producer evidence labels"><Space direction="vertical">{Object.entries(data?.producerStatuses??{}).map(([producer,value])=><div key={producer}><Typography.Text strong>{producer.replaceAll('_',' ')}</Typography.Text> <Tag>{labels[value]}</Tag></div>)}{!Object.keys(data?.producerStatuses??{}).length&&<Typography.Text type="secondary">No producer sections are authorized or available.</Typography.Text>}</Space></Card></div>
    {filters.includeIncidents&&<Card title="Latest incidents (previous 24 hours)"><Table rowKey="evidenceId" dataSource={data?.incidents??[]} pagination={false} scroll={{x:850}} locale={{emptyText:<Empty description="No authorized incident evidence"/>}} columns={[{title:'Type',dataIndex:'type',render:tag},{title:'Vehicle',dataIndex:'vehicleId'},{title:'Severity',dataIndex:'severity',render:(value?:string)=>value??'Unspecified'},{title:'Status',dataIndex:'status'},{title:'Source time',dataIndex:'sourceTimestamp',render:(value:string)=>new Date(value).toLocaleString()},{title:'Producer status',dataIndex:'producerStatus',render:(value:ProducerStatus)=><Tag>{labels[value]}</Tag>}]}/></Card>}
  </Flex>;
}
