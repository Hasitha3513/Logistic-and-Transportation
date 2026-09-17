import { useMemo, useState } from 'react';
import axios from 'axios';
import { Alert, Button, Descriptions, Drawer, Empty, Flex, Input, Select, Space, Table, Tag, Typography, notification } from 'antd';
import { useAuth } from '../../../auth/AuthContext';
import { AcknowledgementModal } from './AcknowledgementModal';
import { useAcknowledgeGpsException, useGpsException, useGpsExceptionEvidence, useGpsExceptionSession, useGpsExceptions } from './hooks';
import type { AcknowledgementCommand, EpisodeFilters, GpsExceptionEpisode, GpsExceptionEvidence, GpsExceptionSeverity, GpsExceptionStatus, GpsExceptionType } from './types';
import { validateRange } from './validation';

const statuses: GpsExceptionStatus[] = ['OPEN', 'ACKNOWLEDGED', 'RECOVERING', 'RESOLVED'];
const severities: GpsExceptionSeverity[] = ['WARNING', 'HIGH'];
const types: GpsExceptionType[] = ['INVALID_TELEMETRY', 'CLOCK_ANOMALY', 'LOW_ACCURACY', 'IMPOSSIBLE_MOVEMENT', 'SIGNAL_LOSS', 'DEVICE_TAMPER', 'BATTERY_LOW', 'BATTERY_RAPID_DRAIN', 'BINDING_VIOLATION', 'PROCESSING_FAILURE'];
const typeLabels: Record<GpsExceptionType, string> = {
  INVALID_TELEMETRY: 'Invalid telemetry', CLOCK_ANOMALY: 'Clock anomaly', LOW_ACCURACY: 'Low accuracy', IMPOSSIBLE_MOVEMENT: 'Impossible movement', SIGNAL_LOSS: 'Signal loss', DEVICE_TAMPER: 'Device tamper', BATTERY_LOW: 'Battery low', BATTERY_RAPID_DRAIN: 'Battery rapid drain', BINDING_VIOLATION: 'Binding violation', PROCESSING_FAILURE: 'Telemetry processing failure',
};
const readable = (value: string) => value.toLowerCase().replaceAll('_', ' ').replace(/^./, first => first.toUpperCase());
const displayTime = (value?: string | null) => value ? new Date(value).toLocaleString(undefined, { timeZoneName: 'short' }) : 'Not recorded';
const severityTag = (value: GpsExceptionSeverity) => <Tag color={value === 'HIGH' ? 'red' : 'orange'}>{value}</Tag>;
const statusTag = (value: string) => <Tag color={value === 'RESOLVED' ? 'green' : value === 'ACKNOWLEDGED' ? 'blue' : value === 'RECOVERING' ? 'cyan' : 'orange'}>{readable(value)}</Tag>;
const isUncertain = (error: unknown) => axios.isAxiosError(error) && !error.response;
const isConflict = (error: unknown) => axios.isAxiosError(error) && error.response?.status === 409;

interface DraftFilters { from: string; to: string; status?: GpsExceptionStatus; type?: GpsExceptionType; severity?: GpsExceptionSeverity; vehicleId: string; deviceId: string }
const initialDraft: DraftFilters = { from: '', to: '', vehicleId: '', deviceId: '' };

export default function GpsExceptionsPage() {
  const { user } = useAuth();
  useGpsExceptionSession(user?.id);
  return <GpsExceptionsWorkspace key={user?.id ?? 'anonymous'} />;
}

function GpsExceptionsWorkspace() {
  const { user, hasPermission } = useAuth();
  const canView = hasPermission('GPS_EXCEPTION_VIEW');
  const canReview = hasPermission('GPS_EXCEPTION_REVIEW');
  const sessionId = user?.id ?? 'anonymous';
  const [draft, setDraft] = useState(initialDraft);
  const [filters, setFilters] = useState<EpisodeFilters>();
  const [cursorHistory, setCursorHistory] = useState<(string | undefined)[]>([undefined]);
  const cursor = cursorHistory.at(-1);
  const [selectedId, setSelectedId] = useState<string>();
  const [evidenceHistory, setEvidenceHistory] = useState<(string | undefined)[]>([undefined]);
  const evidenceCursor = evidenceHistory.at(-1);
  const [ackOpen, setAckOpen] = useState(false);
  const [uncertain, setUncertain] = useState<AcknowledgementCommand>();
  const [ackError, setAckError] = useState<unknown>();
  const rangeError = validateRange(draft.from, draft.to);
  const activeFilters = filters ? { ...filters, cursor, limit: 100 } : undefined;
  const list = useGpsExceptions(sessionId, activeFilters, canView);
  const detail = useGpsException(sessionId, selectedId, canView);
  const evidence = useGpsExceptionEvidence(sessionId, selectedId, evidenceCursor, canView);
  const acknowledge = useAcknowledgeGpsException(selectedId);

  const columns = useMemo(() => [
    { title: 'Status', dataIndex: 'status', render: statusTag },
    { title: 'Type', dataIndex: 'type', render: (value: GpsExceptionType) => typeLabels[value] },
    { title: 'Severity', dataIndex: 'severity', render: severityTag },
    { title: 'Vehicle ID', dataIndex: 'vehicleId' },
    { title: 'Device ID', dataIndex: 'deviceId' },
    { title: 'Opened', dataIndex: 'openedAt', render: displayTime },
    { title: 'Last observed', dataIndex: 'lastObservedAt', render: displayTime },
    { title: 'Evidence', dataIndex: 'evidenceCount' },
    { title: 'Actions', render: (_: unknown, episode: GpsExceptionEpisode) => <Button aria-label={`View GPS exception ${episode.id}`} onClick={() => { setSelectedId(episode.id); setEvidenceHistory([undefined]); setUncertain(undefined); setAckError(undefined); }}>View</Button> },
  ], []);

  if (!canView) return <Alert type="warning" showIcon message="GPS exception access denied" description="GPS_EXCEPTION_VIEW is required to view episodes and evidence. Review permission alone does not reveal Tracking evidence." />;

  const applyFilters = () => {
    if (rangeError) return;
    setCursorHistory([undefined]);
    setSelectedId(undefined);
    setFilters({ from: new Date(draft.from).toISOString(), to: new Date(draft.to).toISOString(), status: draft.status, type: draft.type, severity: draft.severity, vehicleId: draft.vehicleId.trim() || undefined, deviceId: draft.deviceId.trim() || undefined });
  };
  const submitAcknowledgement = async (command: AcknowledgementCommand) => {
    setAckError(undefined);
    try {
      const result = await acknowledge.mutateAsync(command);
      setUncertain(undefined); setAckOpen(false);
      notification.success({ message: 'GPS exception acknowledged', description: `${readable(result.status)} at ${displayTime(result.acknowledgedAt)}` });
      await detail.refetch();
    } catch (error) {
      setAckError(error);
      if (isUncertain(error)) setUncertain(command);
      else setUncertain(undefined);
      if (isConflict(error)) await detail.refetch();
    }
  };
  const episode = detail.data;
  const closeDetail = () => { setSelectedId(undefined); setAckOpen(false); setUncertain(undefined); setAckError(undefined); };

  return <Flex vertical gap={16}>
    <Alert type="info" showIcon message="GPS reliability evidence" description="This operational view shows minimized assessments, not raw telemetry, coordinates, provider payloads or physical-device acceptance. Recovery and resolution are detector-controlled." />
    <Typography.Text type="secondary">API times are UTC. Displayed times include your current display timezone ({Intl.DateTimeFormat().resolvedOptions().timeZone}).</Typography.Text>
    <Space wrap align="start">
      <Input aria-label="GPS exception range from" type="datetime-local" value={draft.from} onChange={event => setDraft(value => ({ ...value, from: event.target.value }))} />
      <Input aria-label="GPS exception range to" type="datetime-local" value={draft.to} onChange={event => setDraft(value => ({ ...value, to: event.target.value }))} />
      <Select aria-label="Filter GPS exceptions by status" allowClear placeholder="All statuses" value={draft.status} onChange={status => setDraft(value => ({ ...value, status }))} options={statuses.map(value => ({ value, label: readable(value) }))} style={{ width: 160 }} />
      <Select aria-label="Filter GPS exceptions by type" allowClear placeholder="All types" value={draft.type} onChange={type => setDraft(value => ({ ...value, type }))} options={types.map(value => ({ value, label: typeLabels[value] }))} style={{ width: 220 }} />
      <Select aria-label="Filter GPS exceptions by severity" allowClear placeholder="All severities" value={draft.severity} onChange={severity => setDraft(value => ({ ...value, severity }))} options={severities.map(value => ({ value }))} style={{ width: 160 }} />
      <Input aria-label="Filter GPS exceptions by vehicle ID" placeholder="Vehicle UUID" value={draft.vehicleId} onChange={event => setDraft(value => ({ ...value, vehicleId: event.target.value }))} />
      <Input aria-label="Filter GPS exceptions by device ID" placeholder="Device UUID" value={draft.deviceId} onChange={event => setDraft(value => ({ ...value, deviceId: event.target.value }))} />
      <Button type="primary" disabled={Boolean(rangeError)} onClick={applyFilters}>Apply filters</Button>
    </Space>
    {rangeError && <Alert type="warning" showIcon message="A valid UTC range is required" description={rangeError} />}
    {!filters && <Empty description="Choose a range of up to seven days, then apply filters" />}
    {filters && list.isError && <Alert type="error" showIcon message="GPS exception episodes unavailable" description="The request was denied, not found, or could not be completed. No foreign-Tenant details are disclosed." action={<Button onClick={() => void list.refetch()}>Retry</Button>} />}
    {filters && <Table<GpsExceptionEpisode> aria-label="GPS exception episodes" rowKey="id" loading={list.isLoading || list.isFetching} dataSource={list.data?.items ?? []} columns={columns} pagination={false} scroll={{ x: 1250 }} locale={{ emptyText: <Empty description="No GPS exception episodes match this range" /> }} />}
    {filters && <Space>
      <Button disabled={cursorHistory.length === 1 || list.isFetching} onClick={() => setCursorHistory(history => history.slice(0, -1))}>Previous page</Button>
      <Typography.Text>Page {cursorHistory.length}</Typography.Text>
      <Button disabled={!list.data?.nextCursor || list.isFetching} onClick={() => setCursorHistory(history => [...history, list.data?.nextCursor ?? undefined])}>Next page</Button>
    </Space>}

    <Drawer title="GPS exception detail" open={Boolean(selectedId)} onClose={closeDetail} width={760} destroyOnClose>
      {detail.isLoading && <Typography.Text role="status">Loading GPS exception…</Typography.Text>}
      {detail.isError && <Alert type="error" message="GPS exception unavailable" description="It may not exist or may belong to another Tenant." action={<Button onClick={() => void detail.refetch()}>Retry</Button>} />}
      {episode && <Flex vertical gap={16}>
        <Descriptions bordered column={1} items={[
          { key: 'status', label: 'Status', children: statusTag(episode.status) }, { key: 'type', label: 'Type', children: typeLabels[episode.type] }, { key: 'severity', label: 'Severity', children: severityTag(episode.severity) },
          { key: 'vehicle', label: 'Vehicle ID', children: episode.vehicleId }, { key: 'device', label: 'Device ID', children: episode.deviceId }, { key: 'opened', label: 'Opened', children: displayTime(episode.openedAt) },
          { key: 'observed', label: 'Last observed', children: displayTime(episode.lastObservedAt) }, { key: 'resolved', label: 'Resolved', children: displayTime(episode.resolvedAt) },
          { key: 'evidence', label: 'Evidence count', children: episode.evidenceCount }, { key: 'recovery', label: 'Consecutive recovery points', children: episode.consecutiveRecoveryPoints }, { key: 'version', label: 'Version', children: episode.version },
        ]} />
        {canReview && episode.status === 'OPEN' && <Button type="primary" onClick={() => { setAckError(undefined); setUncertain(undefined); setAckOpen(true); }}>Acknowledge review</Button>}
        {canReview && episode.status !== 'OPEN' && <Alert type="info" showIcon message={`Acknowledgement unavailable while ${readable(episode.status)}`} />}
        <Typography.Title level={4}>Immutable evidence</Typography.Title>
        {evidence.isError && <Alert type="error" message="Evidence unavailable" action={<Button onClick={() => void evidence.refetch()}>Retry</Button>} />}
        <Table<GpsExceptionEvidence> aria-label="GPS exception evidence" rowKey="id" loading={evidence.isLoading || evidence.isFetching} dataSource={evidence.data?.items ?? []} pagination={false} locale={{ emptyText: <Empty description="No minimized evidence recorded" /> }} scroll={{ x: 1100 }} columns={[
          { title: 'Source time', dataIndex: 'sourceTimestamp', render: displayTime }, { title: 'Assessed', dataIndex: 'assessedAt', render: displayTime }, { title: 'Trust', dataIndex: 'trust', render: readable },
          { title: 'Ordering', dataIndex: 'ordering', render: readable }, { title: 'Reliability', dataIndex: 'reliabilityState', render: readable }, { title: 'Quality', dataIndex: 'qualityCodes', render: (values: string[]) => values.length ? values.map(readable).join(', ') : 'Unknown / not recorded' }, { title: 'Transition', dataIndex: 'transition', render: readable },
        ]} />
        <Space><Button disabled={evidenceHistory.length === 1 || evidence.isFetching} onClick={() => setEvidenceHistory(history => history.slice(0, -1))}>Previous evidence page</Button><Typography.Text>Evidence page {evidenceHistory.length}</Typography.Text><Button disabled={!evidence.data?.nextCursor || evidence.isFetching} onClick={() => setEvidenceHistory(history => [...history, evidence.data?.nextCursor ?? undefined])}>Next evidence page</Button></Space>
      </Flex>}
    </Drawer>
    <AcknowledgementModal episode={episode} open={ackOpen} pending={acknowledge.isPending} uncertain={uncertain} error={ackError} onCancel={() => { setAckOpen(false); setUncertain(undefined); setAckError(undefined); }} onSubmit={submitAcknowledgement} />
  </Flex>;
}
