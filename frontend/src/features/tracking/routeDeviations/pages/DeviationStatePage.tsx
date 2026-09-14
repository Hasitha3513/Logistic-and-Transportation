import {Alert,Button,Descriptions,Drawer,Empty,Flex,Select,Space,Table,Tag,Typography} from 'antd';
import {useState} from 'react';
import {Navigate,useNavigate} from 'react-router-dom';
import {useAuth} from '../../../../auth/AuthContext';
import {DeviationTabs} from '../components/DeviationTabs';
import {useDeviationState,useDeviationStates} from '../hooks/useRouteDeviations';
import type {DeviationState,DeviationVehicleState} from '../types';

const text=(value?:string|null)=>value||'Unknown / Unavailable';
const stateTag=(value:DeviationState)=><Tag color={value==='DEVIATING'?'red':value==='ON_ROUTE'?'green':'default'}>{value.replaceAll('_',' ')}</Tag>;

export default function DeviationStatePage(){
  const {hasPermission}=useAuth(),navigate=useNavigate();
  const [page,setPage]=useState(0),[state,setState]=useState<DeviationState>(),[selected,setSelected]=useState<string>();
  const query=useDeviationStates({state,page,size:20}),detail=useDeviationState(selected);
  if(!hasPermission('ROUTE_DEVIATION_VIEW'))return <Navigate to="/" replace/>;
  return <Flex vertical gap={16}>
    <DeviationTabs active="states"/>
    <Space wrap>
      <Select aria-label="Filter deviation states" allowClear placeholder="All states" value={state}
        onChange={value=>{setState(value);setPage(0)}}
        options={['UNKNOWN','ON_ROUTE','DEVIATING'].map(value=>({value,label:value.replaceAll('_',' ')}))}/>
      <Button onClick={()=>void query.refetch()}>Refresh</Button>
    </Space>
    <Typography.Text type="secondary">Unknown and unavailable facts remain explicit. Coordinates and inferred distances are not displayed.</Typography.Text>
    {query.isError&&<Alert type="error" message="Current deviation states could not be loaded" action={<Button onClick={()=>void query.refetch()}>Retry</Button>}/>} 
    <Table<DeviationVehicleState> aria-label="Current route deviation states" rowKey="vehicleId" loading={query.isLoading}
      dataSource={query.data?.items??[]} locale={{emptyText:<Empty description="No current route-deviation states"/>}}
      scroll={{x:1100}} pagination={{current:page+1,pageSize:20,total:query.data?.total??0,showSizeChanger:false,onChange:value=>setPage(value-1)}}
      columns={[
        {title:'Vehicle ID',dataIndex:'vehicleId'},
        {title:'State',dataIndex:'state',render:stateTag},
        {title:'Availability',dataIndex:'availability',render:(value:string)=>value.replaceAll('_',' ')},
        {title:'Trip ID',dataIndex:'tripId',render:text},
        {title:'Route',render:(_,row)=>row.routeId?`${row.routeId} / ${text(row.routeVersion)}`:'Unknown / Unavailable'},
        {title:'Active episode',dataIndex:'activeEpisodeId',render:(value?:string)=>value??'None'},
        {title:'Latest source time',dataIndex:'lastSourceTimestamp',render:(value?:string)=>value?new Date(value).toLocaleString():'Not evaluated'},
        {title:'Actions',render:(_,row)=><Button aria-label={`View state for ${row.vehicleId}`} onClick={()=>setSelected(row.vehicleId)}>View</Button>},
      ]}/>
    <Drawer title="Current route-deviation state" open={Boolean(selected)} onClose={()=>setSelected(undefined)}>
      {detail.isError&&<Alert type="error" message="State detail could not be loaded" action={<Button onClick={()=>void detail.refetch()}>Retry</Button>}/>} 
      {detail.data&&<Descriptions bordered column={1} items={[
        {key:'vehicle',label:'Vehicle ID',children:detail.data.vehicleId},
        {key:'state',label:'State',children:stateTag(detail.data.state)},
        {key:'availability',label:'Availability',children:detail.data.availability.replaceAll('_',' ')},
        {key:'trip',label:'Trip ID',children:text(detail.data.tripId)},
        {key:'route',label:'Route',children:detail.data.routeId?`${detail.data.routeId} / ${text(detail.data.routeVersion)}`:'Unknown / Unavailable'},
        {key:'rule',label:'Rule ID',children:text(detail.data.ruleId)},
        {key:'episode',label:'Active episode',children:detail.data.activeEpisodeId??'None'},
        {key:'time',label:'Latest eligible source time',children:detail.data.lastSourceTimestamp?new Date(detail.data.lastSourceTimestamp).toLocaleString():'Not evaluated'},
      ]}/>} 
      {detail.data?.activeEpisodeId&&hasPermission('ROUTE_DEVIATION_EVENT_VIEW')&&<Button onClick={()=>navigate(`/tracking/route-deviations/episodes/${detail.data?.activeEpisodeId}`)}>Open episode</Button>}
    </Drawer>
  </Flex>;
}
