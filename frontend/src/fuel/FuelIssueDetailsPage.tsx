import { useState } from 'react';
import { CheckOutlined, CloseOutlined, EditOutlined, SendOutlined } from '@ant-design/icons';
import { Alert, App as AntApp, Button, Card, Descriptions, Flex, Input, Modal, Space, Timeline, Typography } from 'antd';
import { Link, Navigate, useParams } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { useAuth } from '../auth/AuthContext';
import { FuelIssueStatusTag } from '../components/status/StatusTags';
import { useAuthorizeFuelIssue, useCancelFuelIssue, useFuelIssue, useFuelIssueHistory, useIssueFuel, useSubmitFuelIssue } from './hooks/useFuelIssues';

const formatDate = (value?: string | null) => value ? new Date(value).toLocaleString() : '—';

export default function FuelIssueDetailsPage() {
  const { fuelIssueId = '' } = useParams();
  const { hasPermission } = useAuth();
  const { message } = AntApp.useApp();
  const issue = useFuelIssue(fuelIssueId);
  const history = useFuelIssueHistory(fuelIssueId);
  const submit = useSubmitFuelIssue(fuelIssueId);
  const authorize = useAuthorizeFuelIssue(fuelIssueId);
  const issueAction = useIssueFuel(fuelIssueId);
  const cancel = useCancelFuelIssue(fuelIssueId);
  const [confirmation, setConfirmation] = useState<'submit' | 'authorize' | 'issue' | undefined>();
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const current = issue.data;

  if (!hasPermission('FUEL_ISSUE_VIEW')) return <Navigate to="/workspace" replace />;
  const perform = async () => {
    if (!confirmation) return;
    const mutation = confirmation === 'submit' ? submit : confirmation === 'authorize' ? authorize : issueAction;
    try { await mutation.mutateAsync(confirmation === 'authorize' ? {} : undefined); void message.success(`Fuel issue ${confirmation === 'issue' ? 'issued' : `${confirmation}d`}`); setConfirmation(undefined); }
    catch { /* the operational error is rendered below */ }
  };
  const activeError = submit.error ?? authorize.error ?? issueAction.error ?? cancel.error;
  const errorMessage = isAxiosError<{ message?: string }>(activeError) ? activeError.response?.data?.message : undefined;

  const actions = current && <Space wrap>
    {current.status === 'DRAFT' && hasPermission('FUEL_ISSUE_UPDATE') && <Link to={`/fuel/issues/${current.id}/edit`}><Button icon={<EditOutlined />}>Edit</Button></Link>}
    {current.status === 'DRAFT' && hasPermission('FUEL_ISSUE_SUBMIT') && <Button type="primary" icon={<SendOutlined />} onClick={() => setConfirmation('submit')}>Submit</Button>}
    {current.status === 'PENDING_AUTHORIZATION' && hasPermission('FUEL_ISSUE_AUTHORIZE') && <Button type="primary" icon={<CheckOutlined />} onClick={() => setConfirmation('authorize')}>Authorize</Button>}
    {current.status === 'AUTHORIZED' && hasPermission('FUEL_ISSUE_ISSUE') && <Button type="primary" icon={<CheckOutlined />} onClick={() => setConfirmation('issue')}>Record issue</Button>}
    {!['ISSUED', 'CANCELLED'].includes(current.status) && hasPermission('FUEL_ISSUE_CANCEL') && <Button danger icon={<CloseOutlined />} onClick={() => setCancelOpen(true)}>Cancel</Button>}
  </Space>;

  return <Flex vertical gap={18}>
    <Flex justify="space-between" align="center" wrap gap={12}><div><Typography.Title level={3}>{current?.voucherNumber ?? 'Fuel issue'}</Typography.Title><Typography.Text type="secondary">Fuel authorization and issue audit record</Typography.Text></div>{actions}</Flex>
    {issue.isError && <Alert type="error" showIcon message="Fuel issue could not be loaded" />}
    {errorMessage && <Alert type="error" showIcon message="Operation rejected" description={errorMessage} />}
    {current && <>
      {current.status === 'ISSUED' && <Alert type="success" showIcon message="Fuel issued" description={current.odometer != null || current.engineHours != null ? "This operational record is read-only. Authoritative vehicle readings were recorded in the Fleet ledger." : "This operational record is read-only and retained for audit."} />}
      <Card title="Overview" extra={<FuelIssueStatusTag status={current.status} />}><Descriptions bordered column={{ xs: 1, md: 2 }} items={[
        { key: 'voucher', label: 'Voucher', children: current.voucherNumber }, { key: 'date', label: 'Issue date', children: formatDate(current.issueDateTime) },
        { key: 'vehicle', label: 'Vehicle ID', children: current.vehicle.id }, { key: 'trip', label: 'Trip ID', children: current.trip?.id ?? '—' },
        { key: 'driver', label: 'Driver ID', children: current.driver?.id ?? '—' }, { key: 'station', label: 'Station', children: `${current.station.code} — ${current.station.name}` },
        { key: 'fuel', label: 'Fuel type', children: current.fuelType }, { key: 'quantity', label: 'Quantity', children: current.quantity },
        { key: 'price', label: 'Unit price', children: current.unitPrice ?? '—' }, { key: 'total', label: 'Total', children: current.totalAmount ?? '—' },
        { key: 'odometer', label: 'Odometer', children: current.odometer ?? '—' }, { key: 'hours', label: 'Engine hours', children: current.engineHours ?? '—' },
        { key: 'authorized', label: 'Authorized at', children: formatDate(current.authorizationDateTime) }, { key: 'notes', label: 'Notes', children: current.notes ?? '—' },
      ]} /></Card>
      <Card title="Lifecycle history" loading={history.isLoading}><Timeline items={(history.data ?? []).map((entry) => ({
        color: entry.toStatus === 'CANCELLED' ? 'red' : entry.toStatus === 'ISSUED' ? 'green' : 'blue',
        children: <><Typography.Text strong>{entry.action}</Typography.Text><br /><Typography.Text type="secondary">{formatDate(entry.occurredAt)} · {entry.actor} · {entry.fromStatus ?? 'NEW'} → {entry.toStatus}{entry.comment ? ` · ${entry.comment}` : ''}</Typography.Text></>,
      }))} /></Card>
    </>}
    <Modal open={Boolean(confirmation)} title={`${confirmation?.replaceAll('_', ' ')} fuel issue?`} onCancel={() => setConfirmation(undefined)} onOk={() => void perform()} confirmLoading={submit.isPending || authorize.isPending || issueAction.isPending}>
      <Typography.Text>The backend will revalidate vehicle, trip, station, readings, and fuel limits before this transition.</Typography.Text>
    </Modal>
    <Modal open={cancelOpen} title="Cancel fuel issue" okText="Cancel fuel issue" okButtonProps={{ danger: true, disabled: !cancelReason.trim() }} onCancel={() => { setCancelOpen(false); setCancelReason(''); }} onOk={async () => {
      try { await cancel.mutateAsync({ reason: cancelReason.trim() }); void message.success('Fuel issue cancelled'); setCancelOpen(false); setCancelReason(''); } catch { /* rendered above */ }
    }} confirmLoading={cancel.isPending}><Typography.Paragraph>A reason is required and will be retained in history.</Typography.Paragraph><Input.TextArea aria-label="Cancellation reason" value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} rows={3} /></Modal>
  </Flex>;
}
