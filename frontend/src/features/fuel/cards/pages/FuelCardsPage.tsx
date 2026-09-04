import { useState } from 'react';
import { Alert, Button, Card, Descriptions, Input, InputNumber, Modal, Select, Space, Table, Tabs, Tag, Typography, Upload } from 'antd';
import type { UploadFile } from 'antd';
import { useAuth } from '../../../../auth/AuthContext';
import { useFuelCardDetail, useFuelCards } from '../hooks/useFuelCards';
import type { FuelCard, FuelCardBinding, FuelCardHistory, FuelCardImportBatch, FuelCardTransaction } from '../types/fuelCards';

export default function FuelCardsPage() {
  const queries = useFuelCards(); const { hasPermission } = useAuth();
  const [providerId, setProviderId] = useState(''); const [files, setFiles] = useState<UploadFile[]>([]);
  const [selectedCard, setSelectedCard] = useState<FuelCard>(); const [selectedTransaction, setSelectedTransaction] = useState<FuelCardTransaction>();
  const [createOpen, setCreateOpen] = useState(false); const [create, setCreate] = useState({ providerId: '', alias: '', providerCardReference: '', maskedIdentifier: '', lastFour: '', expiryMonth: 12, expiryYear: new Date().getFullYear() + 2 });
  const [bindingType, setBindingType] = useState('VEHICLE'); const [bindingId, setBindingId] = useState('');
  const [restriction, setRestriction] = useState({ currency: 'LKR', maxTransactionAmount: 1, maxDailyAmount: 1, maxMonthlyAmount: 1, maxDailyLitres: 1, allowedFuelTypes: 'DIESEL', allowedStationReferences: '' });
  const [purchaseId, setPurchaseId] = useState('');
  const detail = useFuelCardDetail(selectedCard?.id);
  const upload = () => { const file = files[0]?.originFileObj; if (file && providerId) queries.upload.mutate({ providerId, file }); };
  const reason = 'Operator action';
  const submitCreate = () => queries.create.mutate({ ...create, lastFour: create.lastFour || undefined }, { onSuccess: () => setCreateOpen(false) });
  const applyRestriction = () => selectedCard && queries.restrict.mutate({ card: selectedCard, restriction: {
    ...restriction, allowedFuelTypes: restriction.allowedFuelTypes.split(',').map((value) => value.trim()).filter(Boolean),
    allowedStationReferences: restriction.allowedStationReferences.split(',').map((value) => value.trim()).filter(Boolean), reason,
  } });
  return <div>
    <Alert type="info" showIcon message="Provider synchronization not configured"
      description="Lifecycle actions, restrictions, and blocking are local controls and do not confirm provider action." />
    <Tabs style={{ marginTop: 16 }} items={[
      { key: 'cards', label: 'Fuel cards', children: <Space direction="vertical" style={{ width: '100%' }}>
        {hasPermission('FUEL_CARD_MANAGE') && <Button type="primary" onClick={() => setCreateOpen(true)}>Create fuel card</Button>}
        <Card><Table<FuelCard> rowKey="id" loading={queries.cards.isLoading} onRow={(row) => ({ onClick: () => setSelectedCard(row) })}
        dataSource={queries.cards.data ?? []} columns={[
          { title: 'Alias', dataIndex: 'alias' }, { title: 'Masked card', dataIndex: 'maskedIdentifier' },
          { title: 'Expiry', render: (_, row) => `${String(row.expiryMonth).padStart(2, '0')}/${row.expiryYear}` },
          { title: 'Local status', dataIndex: 'status', render: (status) => <Tag>{status}</Tag> },
          { title: 'Provider', render: () => <Typography.Text type="secondary">Not configured</Typography.Text> },
          { title: 'Action', render: (_, row) => <Button onClick={(event) => { event.stopPropagation(); setSelectedCard(row); }}>Details</Button> },
        ]} /></Card>
        {selectedCard && <Card title={`Card detail — ${selectedCard.alias}`} extra={<Button onClick={() => setSelectedCard(undefined)}>Close</Button>}>
          <Descriptions bordered size="small" items={[
            { key: 'masked', label: 'Masked identifier', children: selectedCard.maskedIdentifier },
            { key: 'expiry', label: 'Expiry', children: `${selectedCard.expiryMonth}/${selectedCard.expiryYear}` },
            { key: 'status', label: 'Local status', children: selectedCard.status },
            { key: 'provider', label: 'Provider synchronization', children: 'Not configured' },
          ]} />
          {hasPermission('FUEL_CARD_MANAGE') && <Space wrap style={{ marginTop: 16 }}>
            {['activate', 'suspend', 'resume', 'cancel'].map((action) => <Button key={action} onClick={() => queries.transition.mutate({ card: selectedCard, action, reason })}>{action}</Button>)}
          </Space>}
          {hasPermission('FUEL_CARD_BLOCK') && <Button danger style={{ margin: 16 }} onClick={() => queries.transition.mutate({ card: selectedCard, action: 'block', reason })}>block</Button>}
          {hasPermission('FUEL_CARD_MANAGE') && <Space direction="vertical" style={{ width: '100%', marginTop: 16 }}>
            <Typography.Title level={5}>Current binding and immutable history</Typography.Title>
            <Space wrap><Select aria-label="Binding type" value={bindingType} onChange={setBindingType} options={[{ value: 'VEHICLE' }, { value: 'DRIVER' }]} />
              <Input aria-label="Binding UUID" value={bindingId} onChange={(event) => setBindingId(event.target.value)} placeholder="Vehicle or driver UUID" />
              <Button disabled={!bindingId} onClick={() => queries.bind.mutate({ card: selectedCard, bindingType, bindingId, reason })}>Assign</Button></Space>
            <Table<FuelCardBinding> size="small" rowKey="id" pagination={false} dataSource={detail.bindings.data ?? []} columns={[
              { title: 'Type', dataIndex: 'bindingType' }, { title: 'Target', dataIndex: 'bindingId' },
              { title: 'From', dataIndex: 'effectiveFrom' }, { title: 'To', dataIndex: 'effectiveTo' }, { title: 'Reason', dataIndex: 'reason' },
            ]} />
            <Typography.Title level={5}>Usage restrictions</Typography.Title>
            <Space wrap><Input aria-label="Currency" value={restriction.currency} onChange={(event) => setRestriction({ ...restriction, currency: event.target.value })} />
              {(['maxTransactionAmount', 'maxDailyAmount', 'maxMonthlyAmount', 'maxDailyLitres'] as const).map((field) => <InputNumber key={field} aria-label={field} min={0.0001} value={restriction[field]} onChange={(value) => setRestriction({ ...restriction, [field]: value ?? 1 })} />)}
              <Input aria-label="Allowed fuel types" value={restriction.allowedFuelTypes} onChange={(event) => setRestriction({ ...restriction, allowedFuelTypes: event.target.value })} />
              <Input aria-label="Allowed stations" value={restriction.allowedStationReferences} onChange={(event) => setRestriction({ ...restriction, allowedStationReferences: event.target.value })} />
              <Button onClick={applyRestriction}>Save restrictions</Button></Space>
            <Typography.Title level={5}>Audit history</Typography.Title>
            <Table<FuelCardHistory> size="small" rowKey="id" pagination={false} dataSource={detail.history.data ?? []} columns={[
              { title: 'Action', dataIndex: 'action' }, { title: 'Result', dataIndex: 'result' },
              { title: 'Reason', dataIndex: 'reasonCode' }, { title: 'Occurred', dataIndex: 'createdAt' },
            ]} />
          </Space>}
        </Card>}
      </Space> },
      { key: 'transactions', label: 'Provider transactions', children: <Card><Table<FuelCardTransaction> rowKey="id"
        loading={queries.transactions.isLoading} dataSource={queries.transactions.data ?? []} columns={[
          { title: 'Provider transaction', dataIndex: 'providerTransactionId' },
          { title: 'Occurred', dataIndex: 'transactionTimestamp' }, { title: 'Fuel', dataIndex: 'fuelType' },
          { title: 'Amount', render: (_, row) => `${row.currency} ${row.totalAmount}` },
          { title: 'State', dataIndex: 'localStatus' },
          { title: 'Review indicators', render: (_, row) => row.indicators.map((code) => <Tag color="orange" key={code}>{code}</Tag>) },
          { title: 'Action', render: (_, row) => <Button onClick={() => setSelectedTransaction(row)}>Details</Button> },
        ]} /></Card> },
      { key: 'imports', label: 'Import history', children: <Space direction="vertical" style={{ width: '100%' }}>
        {hasPermission('FUEL_CARD_IMPORT') && <Card title="Controlled canonical JSON import"><Space wrap>
          <Input aria-label="Provider ID" placeholder="Organization provider UUID" value={providerId} onChange={(e) => setProviderId(e.target.value)} />
          <Upload accept="application/json,.json" beforeUpload={() => false} maxCount={1} fileList={files} onChange={({ fileList }) => setFiles(fileList)}><Button>Select JSON</Button></Upload>
          <Button type="primary" disabled={!providerId || files.length !== 1} loading={queries.upload.isPending} onClick={upload}>Import</Button>
        </Space></Card>}
        <Card><Table<FuelCardImportBatch> rowKey="id" loading={queries.imports.isLoading} dataSource={queries.imports.data ?? []} columns={[
          { title: 'Provider batch', dataIndex: 'providerBatchId' }, { title: 'Transactions', dataIndex: 'transactionCount' },
          { title: 'Imported', dataIndex: 'importedCount' }, { title: 'Review required', dataIndex: 'reviewCount' },
          { title: 'Imported at', dataIndex: 'createdAt' },
        ]} /></Card></Space> },
    ]} />
    <Modal title="Create draft fuel card" open={createOpen} onCancel={() => setCreateOpen(false)} onOk={submitCreate} confirmLoading={queries.create?.isPending}>
      <Space direction="vertical" style={{ width: '100%' }}>
        {([['providerId', 'Provider UUID'], ['alias', 'Alias'], ['providerCardReference', 'Opaque provider reference'], ['maskedIdentifier', 'Masked identifier'], ['lastFour', 'Last four']] as const).map(([field, label]) => <Input key={field} aria-label={label} placeholder={label} value={create[field]} onChange={(event) => setCreate({ ...create, [field]: event.target.value })} />)}
        <Space><InputNumber aria-label="Expiry month" min={1} max={12} value={create.expiryMonth} onChange={(value) => setCreate({ ...create, expiryMonth: value ?? 1 })} /><InputNumber aria-label="Expiry year" min={2000} max={9999} value={create.expiryYear} onChange={(value) => setCreate({ ...create, expiryYear: value ?? 2000 })} /></Space>
      </Space>
    </Modal>
    <Modal title="Immutable provider transaction" open={!!selectedTransaction} footer={null} onCancel={() => setSelectedTransaction(undefined)}>
      {selectedTransaction && <Space direction="vertical" style={{ width: '100%' }}>
        <Typography.Text>{selectedTransaction.providerTransactionId}</Typography.Text>
        <Typography.Text>Reconciliation: {selectedTransaction.localStatus}</Typography.Text>
        {selectedTransaction.originalProviderTransactionId && <Typography.Text>Reversal of: {selectedTransaction.originalProviderTransactionId}</Typography.Text>}
        {hasPermission('FUEL_CARD_RECONCILE') && <Space wrap><Input aria-label="Fuel purchase UUID" value={purchaseId} onChange={(event) => setPurchaseId(event.target.value)} placeholder="Existing fuel purchase UUID" />
          {['match', 'unmatch', 'reject'].map((action) => <Button key={action} disabled={action === 'match' && !purchaseId} onClick={() => queries.reconcile.mutate({ transaction: selectedTransaction, action, purchaseId: purchaseId || undefined, reason })}>{action}</Button>)}</Space>}
      </Space>}
    </Modal>
  </div>;
}
