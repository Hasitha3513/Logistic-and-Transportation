import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Card, Checkbox, Empty, Flex, Form, Input, List, Progress, Radio, Select, Slider, Space, Spin, Tag, Typography } from 'antd';
import axios from 'axios';
import { useEffect, useMemo, useState } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import { ReplayMap } from './ReplayMap';
import { useJourneyReplay } from './hooks';
import type { ReplayForm, ReplayOverlayType, ReplayQuery } from './types';
import { replayFormSchema } from './validation';
import './JourneyReplayPage.css';

const speeds=[0.5,1,2,4,8] as const;
const localValue=(date:Date)=>new Date(date.getTime()-date.getTimezoneOffset()*60_000).toISOString().slice(0,16);
const explain=(error:unknown)=>axios.isAxiosError<{code?:string}>(error)?error.response?.data?.code:undefined;

export default function JourneyReplayPage(){
  const {hasPermission}=useAuth();
  const [query,setQuery]=useState<ReplayQuery>();
  const [activeIndex,setActiveIndex]=useState(0),[playing,setPlaying]=useState(false),[speed,setSpeed]=useState<number>(1),[showCoordinates,setShowCoordinates]=useState(false);
  const canViewIncidents=hasPermission('JOURNEY_REPLAY_INCIDENT_VIEW');
  const [incidentTypes,setIncidentTypes]=useState<ReplayOverlayType[]>(['GEOFENCE']);
  const [defaultRange]=useState(()=>{const to=new Date();return{from:localValue(new Date(to.getTime()-6*3_600_000)),to:localValue(to)}});
  const form=useForm<ReplayForm>({resolver:zodResolver(replayFormSchema),defaultValues:{selectorType:'VEHICLE',selectorId:'',...defaultRange}});
  const selectorType=useWatch({control:form.control,name:'selectorType'});
  const replay=useJourneyReplay(query,incidentTypes,canViewIncidents),points=useMemo(()=>replay.points.data?.items??[],[replay.points.data]),stops=replay.stops.data?.items??[];
  useEffect(()=>{if(!playing||points.length<2)return;const handle=window.setInterval(()=>setActiveIndex(index=>{if(index>=points.length-1){setPlaying(false);return index}return index+1}),Math.max(125,1000/speed));return()=>window.clearInterval(handle)},[playing,points.length,speed]);
  const current=points[activeIndex];
  const submit=(value:ReplayForm)=>{const selector=value.selectorType==='VEHICLE'?{vehicleId:value.selectorId}:{tripId:value.selectorId};setQuery({...selector,from:value.from?new Date(value.from).toISOString():undefined,to:value.to?new Date(value.to).toISOString():undefined,limit:1000});setActiveIndex(0);setPlaying(false)};
  const errorCode=explain(replay.points.error);
  const warnings=useMemo(()=>new Set(points.flatMap(point=>point.qualityFlags)),[points]);
  if(!hasPermission('JOURNEY_REPLAY_VIEW'))return <Navigate to="/" replace/>;
  return <Flex className="journey-replay" vertical gap={16}>
    <Typography.Paragraph type="secondary">Replay authorized retained Vehicle history in source-time order. The page never auto-plays and keeps query selectors and cursors out of the URL.</Typography.Paragraph>
    <Form onFinish={form.handleSubmit(submit)} layout="vertical"><div className="journey-replay__controls">
      <Controller control={form.control} name="selectorType" render={({field})=><Form.Item label="Replay by"><Radio.Group {...field} optionType="button" options={[{label:'Vehicle',value:'VEHICLE'},{label:'Trip',value:'TRIP'}]}/></Form.Item>}/>
      <Controller control={form.control} name="selectorId" render={({field,fieldState})=><Form.Item label={selectorType==='VEHICLE'?'Vehicle ID':'Trip ID'} validateStatus={fieldState.error?'error':undefined} help={fieldState.error?.message}><Input {...field} aria-label={selectorType==='VEHICLE'?'Vehicle ID':'Trip ID'} autoComplete="off"/></Form.Item>}/>
      <Controller control={form.control} name="from" render={({field,fieldState})=><Form.Item label="Range start" validateStatus={fieldState.error?'error':undefined} help={fieldState.error?.message}><Input {...field} type="datetime-local" aria-label="Range start"/></Form.Item>}/>
      <Controller control={form.control} name="to" render={({field,fieldState})=><Form.Item label="Range end" validateStatus={fieldState.error?'error':undefined} help={fieldState.error?.message}><Input {...field} type="datetime-local" aria-label="Range end"/></Form.Item>}/>
      <Form.Item label=" "><Button type="primary" htmlType="submit" loading={replay.points.isFetching||replay.stops.isFetching}>Load replay</Button></Form.Item>
    </div></Form>
    {(replay.points.isLoading||replay.stops.isLoading)&&<Spin aria-label="Loading journey replay"/>}
    {errorCode==='REPLAY_CURSOR_EXPIRED'&&<Alert type="warning" message="Replay page expired" description="Reload the replay to create a new stable snapshot." action={<Button onClick={()=>setQuery(query?{...query,cursor:undefined}:undefined)}>Reload</Button>}/>}
    {replay.points.isError&&errorCode!=='REPLAY_CURSOR_EXPIRED'&&<Alert type={axios.isAxiosError(replay.points.error)&&replay.points.error.response?.status===403?'warning':'error'} message={axios.isAxiosError(replay.points.error)&&replay.points.error.response?.status===403?'Journey replay is forbidden':'Journey replay could not be loaded'} description="The existing timeline remains unchanged. Check the selector, range, or permission and retry." action={<Button onClick={()=>void replay.points.refetch()}>Retry</Button>}/>}
    {replay.points.data&&<>
      {replay.points.data.coverage==='NO_DATA'&&<Empty description="No retained journey evidence exists for this range"/>}
      {replay.points.data.coverage==='PARTIAL_RETENTION'&&<Alert type="warning" message="Partial retained history" description="The requested range crosses retention boundaries. Missing intervals remain explicit and the path is not joined across them."/>}
      {(replay.points.data.browserCeilingWarning||replay.points.data.truncated)&&<Alert type="warning" message="Replay is truncated" description="Choose a narrower range before relying on the end of this timeline."/>}
      {warnings.size>0&&<Alert type="warning" message="Journey quality warnings" description={[...warnings].join(', ').replaceAll('_',' ')}/>}
      {points.length>0&&<>
        <Card size="small"><Flex vertical gap={8}><Space wrap><Button aria-label={playing?'Pause replay':'Play replay'} onClick={()=>setPlaying(value=>!value)}>{playing?'Pause':'Play'}</Button><Select aria-label="Playback speed" value={speed} onChange={setSpeed} options={speeds.map(value=>({value,label:`${value}x`}))}/><Button onClick={()=>setActiveIndex(0)}>Restart</Button><Button aria-pressed={showCoordinates} onClick={()=>setShowCoordinates(value=>!value)}>{showCoordinates?'Hide':'Show'} coordinates</Button></Space><div aria-live="polite">{playing?'Replay playing':'Replay paused'} at {speed}x — point {activeIndex+1} of {points.length}</div><Slider ariaLabelForHandle="Replay timeline seek" min={0} max={Math.max(0,points.length-1)} value={activeIndex} onChange={setActiveIndex}/><Progress percent={Math.round(100*(activeIndex+1)/points.length)} showInfo={false}/></Flex></Card>
        <div className="journey-replay__workspace"><ReplayMap points={points} stops={stops} activeIndex={activeIndex}/><Card className="journey-replay__timeline" title="Synchronized timeline"><Typography.Text strong>{new Date(current.sourceTimestamp).toLocaleString()}</Typography.Text><br/><Typography.Text>Source timestamp: {current.sourceTimestamp}</Typography.Text><br/><Typography.Text type="secondary">Received: {current.receivedAt}</Typography.Text><br/>{showCoordinates&&<Typography.Paragraph>Coordinates: {current.coordinate.latitude}, {current.coordinate.longitude}</Typography.Paragraph>}<Space wrap>{current.qualityFlags.map(flag=><Tag key={flag}>{flag.replaceAll('_',' ')}</Tag>)}<Tag>{current.trust}</Tag><Tag>{current.ordering}</Tag></Space>{current.attribution&&<Typography.Paragraph>Trip {current.attribution.tripId??'unknown'} · Route {current.attribution.routeId??'unknown'} / {current.attribution.routeVersion??'unknown'} · {current.attribution.status}</Typography.Paragraph>}</Card></div>
        <Card title="Detected stops"><List dataSource={stops} locale={{emptyText:'No confirmed stops in the visible evidence'}} renderItem={stop=><List.Item><List.Item.Meta title={`${new Date(stop.start).toLocaleString()} – ${new Date(stop.end).toLocaleString()}`} description={`${Math.round(stop.durationSeconds/60)} min · ${stop.pointCount} points · radius ${Math.round(stop.radiusMeters)} m · ${stop.quality}${stop.startTruncated||stop.endTruncated?' · range boundary':''}`}/></List.Item>}/></Card>
        {replay.points.data.missingIntervals.length>0&&<Card title="Journey gaps"><List dataSource={replay.points.data.missingIntervals} renderItem={gap=><List.Item>{gap.from} – {gap.to}: {gap.reasons.join(', ').replaceAll('_',' ')}</List.Item>}/></Card>}
        {canViewIncidents&&<Card title="Incident overlays"><Flex vertical gap={12}>
          <Checkbox.Group value={incidentTypes} onChange={values=>setIncidentTypes(values as ReplayOverlayType[])} options={[{label:'Geofence transitions',value:'GEOFENCE'},{label:'Speed episodes — technical evidence',value:'SPEED'},{label:'Route deviations — technical evidence',value:'ROUTE_DEVIATION'}]}/>
          {(incidentTypes.includes('SPEED')||incidentTypes.includes('ROUTE_DEVIATION'))&&<Alert type="warning" message="Technical-only incident evidence" description="Speed field fidelity and route-deviation field acceptance remain pending. Replay does not upgrade producer acceptance."/>}
          {replay.incidents.isError&&<Alert
            type="error"
            message="Incident evidence could not be loaded"
            description="The movement timeline remains available. Retry the incident query independently."
            action={<Button onClick={()=>void replay.incidents.refetch()}>Retry incidents</Button>}
          />}
          <List loading={replay.incidents.isFetching} dataSource={replay.incidents.data?.items??[]} locale={{emptyText:'No producer incident evidence exists for the selected range'}} renderItem={incident=><List.Item><List.Item.Meta title={incident.status} description={<Space wrap><span>{new Date(incident.sourceTimestamp).toLocaleString()}</span><Tag>{incident.severity??'UNSPECIFIED'}</Tag><Tag>{incident.producer}</Tag><Tag>{incident.evidenceStatus.replaceAll('_',' ')}</Tag></Space>}/></List.Item>}/>
        </Flex></Card>}
        {replay.points.data.nextCursor&&<Button onClick={()=>setQuery(currentQuery=>currentQuery?{...currentQuery,cursor:replay.points.data?.nextCursor}:currentQuery)}>Load next point page</Button>}
      </>}
    </>}
  </Flex>
}
