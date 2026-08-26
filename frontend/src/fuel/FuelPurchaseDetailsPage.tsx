import { useState } from 'react';
import { Alert, App as AntApp, Button, Card, DatePicker, Descriptions, Flex, Form, Input, InputNumber, Modal, Select, Space, Statistic, Timeline, Typography } from 'antd';
import dayjs from 'dayjs';
import { Link, Navigate, useParams } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { useAuth } from '../auth/AuthContext';
import { FuelPurchaseStatusTag } from '../components/status/StatusTags';
import { useFuelStations } from './hooks/useFuelIssues';
import { useApproveFuelPurchase, useCancelFuelPurchase, useFuelPurchase, useFuelPurchaseHistory, useReceiveFuelPurchase, useReconcileFuelPurchase, useSubmitFuelPurchase } from './hooks/useFuelPurchases';

const fmt = (value?: string | null) => value ? new Date(value).toLocaleString() : '—';

export default function FuelPurchaseDetailsPage() {
  const { fuelPurchaseId = '' } = useParams();
  const { hasPermission } = useAuth();
  const { message } = AntApp.useApp();
  const purchase = useFuelPurchase(fuelPurchaseId);
  const history = useFuelPurchaseHistory(fuelPurchaseId);
  const stations = useFuelStations();
  const submit = useSubmitFuelPurchase(fuelPurchaseId);
  const approve = useApproveFuelPurchase(fuelPurchaseId);
  const receive = useReceiveFuelPurchase(fuelPurchaseId);
  const reconcile = useReconcileFuelPurchase(fuelPurchaseId);
  const cancel = useCancelFuelPurchase(fuelPurchaseId);
  const [confirm, setConfirm] = useState<'submit' | 'approve'>();
  const [receiveOpen, setReceiveOpen] = useState(false);
  const [receivedQuantity, setReceivedQuantity] = useState<number>();
  const [receivedAt, setReceivedAt] = useState(dayjs());
  const [deliveryNote, setDeliveryNote] = useState('');
  const [destinationFuelStationId, setDestinationFuelStationId] = useState<string>();
  const [remarks, setRemarks] = useState('');
  const [reconcileOpen, setReconcileOpen] = useState(false);
  const [reconciliationNotes, setReconciliationNotes] = useState('');
  const [reference, setReference] = useState('');
  const [cancelOpen, setCancelOpen] = useState(false);
  const [reason, setReason] = useState('');

  if (!hasPermission('FUEL_PURCHASE_VIEW')) return <Navigate to="/workspace" replace />;
  const current = purchase.data;
  const activeError = submit.error ?? approve.error ?? receive.error ?? reconcile.error ?? cancel.error;
  const error = isAxiosError<{ message?: string }>(activeError) ? activeError.response?.data?.message : undefined;

  const runConfirm = async () => {
    if (!confirm) return;
    try {
      await (confirm === 'submit' ? submit.mutateAsync(undefined) : approve.mutateAsync({}));
      void message.success(`Fuel purchase ${confirm === 'submit' ? 'submitted' : 'approved'}`);
      setConfirm(undefined);
    } catch { /* rendered below */ }
  };

  const actions = current && <Space wrap>
    {current.status === 'DRAFT' && hasPermission('FUEL_PURCHASE_UPDATE') && <Link to={`/fuel/purchases/${current.id}/edit`}><Button>Edit</Button></Link>}
    {current.status === 'DRAFT' && hasPermission('FUEL_PURCHASE_SUBMIT') && <Button type="primary" onClick={() => setConfirm('submit')}>Submit</Button>}
    {current.status === 'SUBMITTED' && hasPermission('FUEL_PURCHASE_APPROVE') && <Button type="primary" onClick={() => setConfirm('approve')}>Approve</Button>}
    {current.status === 'APPROVED' && hasPermission('FUEL_PURCHASE_RECEIVE') && <Button type="primary" onClick={() => { setReceivedQuantity(Number(current.quantity)); setDestinationFuelStationId(current.fuelStationId ?? undefined); setReceiveOpen(true); }}>Receive</Button>}
    {current.status === 'RECEIVED' && hasPermission('FUEL_PURCHASE_RECONCILE') && <Button type="primary" onClick={() => setReconcileOpen(true)}>Reconcile</Button>}
    {['DRAFT', 'SUBMITTED', 'APPROVED'].includes(current.status) && hasPermission('FUEL_PURCHASE_CANCEL') && <Button danger onClick={() => setCancelOpen(true)}>Cancel</Button>}
  </Space>;

  return <Flex vertical gap={18}>
    <Flex justify="space-between" wrap gap={12}><div><Typography.Title level={3}>{current?.purchaseNumber ?? 'Fuel purchase'}</Typography.Title><Typography.Text type="secondary">Invoice, physical receipt and reconciliation audit trail.</Typography.Text></div>{actions}</Flex>
    {purchase.isError && <Alert type="error" showIcon message="Fuel purchase could not be loaded" />}
    {error && <Alert type="error" showIcon message="Operation rejected" description={error} />}
    {current && <>
      <Card title="Purchase" extra={<FuelPurchaseStatusTag status={current.status} />}><Descriptions bordered column={{ xs: 1, md: 2 }} items={[
        { key: 'vendor', label: 'Vendor', children: `${current.vendor.code} — ${current.vendor.name}` }, { key: 'invoice', label: 'Invoice', children: current.invoiceNumber ?? '—' },
        { key: 'date', label: 'Purchase date', children: current.purchaseDate }, { key: 'invoiceDate', label: 'Invoice date', children: current.invoiceDate ?? '—' },
        { key: 'fuel', label: 'Fuel type', children: current.fuelType }, { key: 'qty', label: 'Invoice quantity', children: current.quantity },
        { key: 'unit', label: 'Unit price', children: current.unitPrice }, { key: 'currency', label: 'Currency', children: current.currencyCode },
        { key: 'notes', label: 'Notes', children: current.notes ?? '—' },
      ]} /></Card>
      <Card title="Financial totals"><Flex gap={28} wrap><Statistic title="Subtotal" value={current.subtotal} precision={2} /><Statistic title={`Tax (${current.taxRate}%)`} value={current.taxAmount} precision={2} /><Statistic title="Other charges" value={current.otherCharges} precision={2} /><Statistic title="Total" value={current.totalAmount} precision={2} /></Flex></Card>
      <Card title="Receipt and variance"><Descriptions column={{ xs: 1, md: 2 }} items={[
        { key: 'received', label: 'Received quantity', children: current.receivedQuantity ?? '—' }, { key: 'qv', label: 'Quantity variance', children: current.quantityVariance ?? '—' },
        { key: 'expected', label: 'Catalogue price', children: current.expectedUnitPrice ?? '—' }, { key: 'pv', label: 'Price variance', children: current.priceVariance ?? '—' },
        { key: 'receivedAt', label: 'Received at', children: fmt(current.receivedAt) }, { key: 'delivery', label: 'Delivery note', children: current.deliveryNoteNumber ?? '—' },
        { key: 'rec', label: 'Reconciliation', children: current.reconciliationStatus }, { key: 'ref', label: 'Reference', children: current.reconciliationReference ?? '—' },
      ]} /></Card>
      <Card title="History" loading={history.isLoading}><Timeline items={(history.data ?? []).map(entry => ({
        color: entry.toStatus === 'CANCELLED' ? 'red' : entry.toStatus === 'RECONCILED' ? 'green' : 'blue',
        children: <><Typography.Text strong>{entry.action}</Typography.Text><br /><Typography.Text type="secondary">{fmt(entry.occurredAt)} · {entry.actor} · {entry.fromStatus ?? 'NEW'} → {entry.toStatus}{entry.comment ? ` · ${entry.comment}` : ''}</Typography.Text></>,
      }))} /></Card>
    </>}
    <Modal open={Boolean(confirm)} title={`${confirm} fuel purchase?`} onCancel={() => setConfirm(undefined)} onOk={() => void runConfirm()} confirmLoading={submit.isPending || approve.isPending}><Typography.Text>The backend will revalidate lifecycle, vendor, invoice and calculated totals.</Typography.Text></Modal>
    <Modal open={receiveOpen} title="Receive fuel purchase" onCancel={() => setReceiveOpen(false)} okButtonProps={{ disabled: !receivedQuantity || receivedQuantity <= 0 }} confirmLoading={receive.isPending} onOk={async () => {
      try { await receive.mutateAsync({ receivedQuantity, receivedAt: receivedAt.toISOString(), destinationFuelStationId: destinationFuelStationId || null, deliveryNoteNumber: deliveryNote || null, remarks: remarks || null }); void message.success('Fuel receipt recorded'); setReceiveOpen(false); } catch { /* rendered above */ }
    }}><Form layout="vertical"><Alert type="info" showIcon message={`Invoice quantity: ${current?.quantity ?? '—'} · Variance: ${receivedQuantity == null || !current ? '—' : receivedQuantity - Number(current.quantity)}`} /><Form.Item label="Received quantity" required><InputNumber value={receivedQuantity} min={0} precision={4} onChange={value => setReceivedQuantity(value ?? undefined)} style={{ width: '100%' }} /></Form.Item><Form.Item label="Destination station"><Select allowClear showSearch optionFilterProp="label" value={destinationFuelStationId} onChange={setDestinationFuelStationId} options={(stations.data ?? []).map(station => ({ value: station.id, label: `${station.code} — ${station.name}` }))} /></Form.Item><Form.Item label="Received time"><DatePicker showTime value={receivedAt} onChange={value => value && setReceivedAt(value)} style={{ width: '100%' }} /></Form.Item><Form.Item label="Delivery note"><Input value={deliveryNote} onChange={event => setDeliveryNote(event.target.value)} /></Form.Item><Form.Item label="Remarks"><Input.TextArea value={remarks} onChange={event => setRemarks(event.target.value)} /></Form.Item></Form></Modal>
    <Modal open={reconcileOpen} title="Reconcile fuel purchase" onCancel={() => setReconcileOpen(false)} confirmLoading={reconcile.isPending} onOk={async () => {
      try { await reconcile.mutateAsync({ reconciliationNotes: reconciliationNotes || null, referenceNumber: reference || null }); void message.success('Fuel purchase reconciled'); setReconcileOpen(false); } catch { /* rendered above */ }
    }}><Descriptions size="small" column={1} items={[{ key: 'iq', label: 'Invoice quantity', children: current?.quantity }, { key: 'rq', label: 'Received quantity', children: current?.receivedQuantity }, { key: 'qv', label: 'Quantity variance', children: current?.quantityVariance }, { key: 'pv', label: 'Price variance', children: current?.priceVariance ?? '—' }, { key: 'total', label: 'Total', children: current?.totalAmount }]} /><Form layout="vertical"><Form.Item label="Reference"><Input value={reference} onChange={event => setReference(event.target.value)} /></Form.Item><Form.Item label="Notes"><Input.TextArea value={reconciliationNotes} onChange={event => setReconciliationNotes(event.target.value)} /></Form.Item></Form></Modal>
    <Modal open={cancelOpen} title="Cancel fuel purchase" okButtonProps={{ danger: true, disabled: !reason.trim() }} onCancel={() => setCancelOpen(false)} confirmLoading={cancel.isPending} onOk={async () => {
      try { await cancel.mutateAsync({ reason: reason.trim() }); void message.success('Fuel purchase cancelled'); setCancelOpen(false); } catch { /* rendered above */ }
    }}><Input.TextArea aria-label="Cancellation reason" value={reason} onChange={event => setReason(event.target.value)} rows={3} /></Modal>
  </Flex>;
}
