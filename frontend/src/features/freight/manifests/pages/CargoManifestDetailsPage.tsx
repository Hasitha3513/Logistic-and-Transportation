import { EditOutlined, PlusOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, App as AntApp, Badge, Button, Card, Checkbox, Descriptions, Divider, Flex, Form, Input, InputNumber, List, Modal, Radio, Select, Space, Tag, Typography } from 'antd';
import { isAxiosError } from 'axios';
import { useEffect, useState } from 'react';
import { Controller, useForm, useWatch, type DefaultValues } from 'react-hook-form';
import { Navigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../../auth/AuthContext';
import { useFreightOrder } from '../../orders/hooks/useFreightOrders';
import { useCargoManifest, useManifestActions, useManifestReadiness } from '../hooks/useCargoManifests';
import type { ManifestFailure, ManifestItem, ManifestItemPayload } from '../types/cargoManifest';
import { manifestItemSchema, type ManifestItemForm } from '../validation/cargoManifestSchema';

interface ErrorBody { message?: string; code?: string; fieldErrors?: Array<{ field: string; message: string }> }

const classificationGuidance = 'Complete Fragile and Temperature-sensitive classification for all cargo items before finalizing the manifest.';
const empty: DefaultValues<ManifestItemForm> = {
  freightOrderLineId: '', description: '', quantity: 1, packingInformation: '', commodityClassification: '',
  customsApplicable: false, customsInformation: '', hazardous: false, hazardousClassification: '', hazardousDetails: '',
  unitWeight: undefined, weightUnit: 'KG', length: undefined, width: undefined, height: undefined, dimensionUnit: 'M',
};
const readinessMessage = (failure: ManifestFailure) => failure.code === 'SPECIAL_CARGO_CLASSIFICATION_MISSING' ? classificationGuidance : failure.message;

export default function CargoManifestDetailsPage() {
  const { cargoManifestId = '' } = useParams();
  const { hasPermission } = useAuth();
  const { message, modal } = AntApp.useApp();
  const query = useCargoManifest(cargoManifestId);
  const readiness = useManifestReadiness(cargoManifestId);
  const order = useFreightOrder(query.data?.freightOrderId);
  const actions = useManifestActions(cargoManifestId);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<ManifestItem>();
  const form = useForm<ManifestItemForm>({ resolver: zodResolver(manifestItemSchema), defaultValues: empty });
  const customsApplicable = useWatch({ control: form.control, name: 'customsApplicable' });
  const hazardous = useWatch({ control: form.control, name: 'hazardous' });

  useEffect(() => {
    if (!open) return;
    form.reset(editing ? {
      ...editing,
      customsInformation: editing.customsInformation ?? '',
      hazardousClassification: editing.hazardousClassification ?? '',
      hazardousDetails: editing.hazardousDetails ?? '',
      fragile: editing.fragile ?? undefined,
      temperatureSensitive: editing.temperatureSensitive ?? undefined,
      unitWeight: editing.unitWeight ?? undefined,
      weightUnit: (editing.weightUnit as 'KG' | 'G' | 'TONNE') ?? 'KG',
      length: editing.length ?? undefined,
      width: editing.width ?? undefined,
      height: editing.height ?? undefined,
      dimensionUnit: (editing.dimensionUnit as 'M' | 'CM' | 'MM') ?? 'M',
    } : empty);
  }, [editing, form, open]);

  if (!hasPermission('CARGO_MANIFEST_VIEW')) return <Navigate to="/workspace" replace />;
  const manifest = query.data;

  const mutate = async (values: ManifestItemForm) => {
    if (!manifest) return;
    const payload: ManifestItemPayload = {
      ...values, version: manifest.version,
      customsInformation: values.customsInformation || null,
      hazardousClassification: values.hazardousClassification || null,
      hazardousDetails: values.hazardousDetails || null,
      unitWeight: values.unitWeight ?? null,
      weightUnit: values.unitWeight ? (values.weightUnit || 'KG') : null,
      length: values.length ?? null,
      width: values.width ?? null,
      height: values.height ?? null,
      dimensionUnit: (values.length || values.width || values.height) ? (values.dimensionUnit || 'M') : null,
    };
    try {
      if (editing) await actions.updateItem.mutateAsync({ itemId: editing.id, payload });
      else await actions.addItem.mutateAsync(payload);
      setOpen(false);
      void message.success(editing ? 'Cargo item updated' : 'Cargo item added');
    } catch (error) {
      if (isAxiosError<ErrorBody>(error)) form.setError('root', { message: error.response?.data?.message ?? 'Cargo item could not be saved' });
    }
  };

  const finalize = () => {
    if (!manifest) return;
    const hasClassificationFailure = readiness.data?.failures.some(failure => failure.code === 'SPECIAL_CARGO_CLASSIFICATION_MISSING');
    modal.confirm({
      title: 'Finalize cargo manifest?', icon: <SafetyCertificateOutlined />,
      content: readiness.data?.ready ? 'All known cargo is manifested and validation passed.' : hasClassificationFailure ? classificationGuidance : 'Validation blockers must be resolved before finalization.',
      okText: 'Finalize manifest', okButtonProps: { disabled: !readiness.data?.ready },
      onOk: async () => {
        try {
          await actions.finalize.mutateAsync(manifest.version);
          void message.success('Cargo manifest finalized');
        } catch (error) {
          if (isAxiosError<ErrorBody>(error)) {
            const body = error.response?.data;
            void message.error(body?.code === 'SPECIAL_CARGO_CLASSIFICATION_MISSING' ? classificationGuidance : body?.message ?? 'Cargo manifest could not be finalized');
          }
        }
      },
    });
  };

  const field = (name: keyof ManifestItemForm, label: string, control: React.ReactElement) => <Form.Item label={label} required={name === 'fragile' || name === 'temperatureSensitive'} validateStatus={form.formState.errors[name] ? 'error' : undefined} help={form.formState.errors[name]?.message as string | undefined}>{control}</Form.Item>;

  return <Flex vertical gap={18}>
    {query.isError && <Alert type="error" showIcon message="Cargo manifest could not be loaded" />}
    {manifest && <>
      <Flex justify="space-between" align="start" wrap gap={12}>
        <div><Typography.Title level={3}>{manifest.manifestNumber}</Typography.Title><Space><Tag color={manifest.finalized ? 'green' : 'gold'}>{manifest.finalized ? 'FINALIZED' : 'UNFINALIZED'}</Tag><Typography.Text type="secondary">Freight Order {manifest.freightOrderNumber}</Typography.Text></Space></div>
        <Space>{!manifest.finalized && hasPermission('CARGO_MANIFEST_MANAGE') && <Button icon={<PlusOutlined />} onClick={() => { setEditing(undefined); setOpen(true); }}>Add cargo item</Button>}{!manifest.finalized && hasPermission('CARGO_MANIFEST_FINALIZE') && <Button type="primary" icon={<SafetyCertificateOutlined />} onClick={finalize}>Finalize manifest</Button>}</Space>
      </Flex>
      <Card title="Readiness" extra={<Badge status={readiness.data?.ready ? 'success' : 'warning'} text={readiness.data?.ready ? 'Ready to finalize' : 'Not ready'} />} loading={readiness.isLoading}>
        {readiness.data?.ready ? <Alert type="success" showIcon message="Manifest is complete" /> : <List size="small" dataSource={readiness.data?.failures ?? []} locale={{ emptyText: 'Readiness is being evaluated' }} renderItem={failure => <List.Item><Alert type="warning" showIcon message={readinessMessage(failure)} description={failure.code} /></List.Item>} />}
      </Card>
      <Card title="Manifest overview"><Descriptions bordered column={{ xs: 1, md: 2 }} items={[{ key: 'manifest', label: 'Manifest reference', children: manifest.manifestNumber }, { key: 'order', label: 'Freight Order', children: manifest.freightOrderNumber }, { key: 'condition', label: 'Condition', children: manifest.finalized ? 'FINALIZED' : 'UNFINALIZED' }, { key: 'version', label: 'Version', children: manifest.version }]} /></Card>
      <Card title="Cargo items"><List dataSource={manifest.items} locale={{ emptyText: 'No cargo items have been manifested' }} renderItem={item => {
        const classificationRequired = item.fragile == null || item.temperatureSensitive == null;
        return <List.Item actions={!manifest.finalized && hasPermission('CARGO_MANIFEST_MANAGE') ? [<Button key="edit" type="link" icon={<EditOutlined />} onClick={() => { setEditing(item); setOpen(true); }}>Edit</Button>] : []}>
          <List.Item.Meta title={<Space wrap><Typography.Text strong>{item.description}</Typography.Text><Tag>{item.commodityClassification}</Tag>{item.hazardous && <Tag color="red">HAZARDOUS</Tag>}{item.customsApplicable && <Tag color="blue">CUSTOMS</Tag>}{item.fragile === true && <Tag color="orange">FRAGILE</Tag>}{item.temperatureSensitive === true && <Tag color="cyan">TEMPERATURE SENSITIVE</Tag>}{classificationRequired && <Tag color="warning">CLASSIFICATION REQUIRED</Tag>}</Space>} description={<div>Quantity: {item.quantity} · Packing: {item.packingInformation}<br />Weight: {item.unitWeight ? `${item.unitWeight} ${item.weightUnit ?? 'KG'} per unit` : <Tag color="warning">WEIGHT REQUIRED</Tag>} · Dimensions: {item.length && item.width && item.height ? `${item.length} × ${item.width} × ${item.height} ${item.dimensionUnit ?? 'M'}` : <Tag color="warning">DIMENSIONS REQUIRED</Tag>}<br />Freight Order line: {order.data?.lines.find(line => line.id === item.freightOrderLineId)?.description ?? item.freightOrderLineId}{item.customsApplicable && <><br />Customs: {item.customsInformation}</>}{item.hazardous && <><br />Hazardous: {item.hazardousClassification} — {item.hazardousDetails}</>}</div>} />
        </List.Item>;
      }} /></Card>
      <Card title="Audit"><Descriptions column={{ xs: 1, md: 2 }} items={[{ key: 'created', label: 'Created', children: `${new Date(manifest.createdAt).toLocaleString()} by ${manifest.createdBy}` }, { key: 'updated', label: 'Updated', children: `${new Date(manifest.updatedAt).toLocaleString()} by ${manifest.updatedBy}` }, { key: 'finalized', label: 'Finalized', children: manifest.finalizedAt ? `${new Date(manifest.finalizedAt).toLocaleString()} by ${manifest.finalizedBy}` : '—' }]} /></Card>
      <Modal title={editing ? 'Edit cargo item' : 'Add cargo item'} open={open} onCancel={() => setOpen(false)} onOk={() => void form.handleSubmit(mutate)()} okText="Save cargo item" confirmLoading={actions.addItem.isPending || actions.updateItem.isPending} width={760}>
        {form.formState.errors.root?.message && <Alert type="error" showIcon message={form.formState.errors.root.message} />}
        <Form layout="vertical">
          <Flex gap={12} wrap>{field('freightOrderLineId', 'Freight Order line', <Controller name="freightOrderLineId" control={form.control} render={({ field: input }) => <Select {...input} aria-label="Freight Order line" options={(order.data?.lines ?? []).map(line => ({ value: line.id, label: `${line.description} — ${line.quantity}` }))} style={{ width: 330 }} />} />)}{field('quantity', 'Quantity', <Controller name="quantity" control={form.control} render={({ field: input }) => <InputNumber {...input} aria-label="Quantity" min={0.0001} style={{ width: 160 }} />} />)}</Flex>
          {field('description', 'Traceable description', <Controller name="description" control={form.control} render={({ field: input }) => <Input {...input} aria-label="Traceable description" />} />)}
          {field('packingInformation', 'Packing information', <Controller name="packingInformation" control={form.control} render={({ field: input }) => <Input {...input} aria-label="Packing information" placeholder="Supplier/operator supplied description" />} />)}
          {field('commodityClassification', 'Commodity classification', <Controller name="commodityClassification" control={form.control} render={({ field: input }) => <Input {...input} aria-label="Commodity classification" placeholder="Provider-neutral supplied code" />} />)}
          <Divider orientation="left">Cargo measurements & physical dimensions (US-27)</Divider>
          <Flex gap={12} wrap align="start">
            {field('unitWeight', 'Unit weight', <Controller name="unitWeight" control={form.control} render={({ field: input }) => <InputNumber {...input} aria-label="Unit weight" min={0.0001} placeholder="e.g. 500.0" style={{ width: 180 }} />} />)}
            {field('weightUnit', 'Weight unit', <Controller name="weightUnit" control={form.control} render={({ field: input }) => <Select {...input} aria-label="Weight unit" options={[{ value: 'KG', label: 'kg (Kilograms)' }, { value: 'G', label: 'g (Grams)' }, { value: 'TONNE', label: 't (Tonnes)' }]} style={{ width: 140 }} />} />)}
          </Flex>
          <Flex gap={12} wrap align="start">
            {field('length', 'Length', <Controller name="length" control={form.control} render={({ field: input }) => <InputNumber {...input} aria-label="Length" min={0.0001} placeholder="e.g. 1.2" style={{ width: 120 }} />} />)}
            {field('width', 'Width', <Controller name="width" control={form.control} render={({ field: input }) => <InputNumber {...input} aria-label="Width" min={0.0001} placeholder="e.g. 0.8" style={{ width: 120 }} />} />)}
            {field('height', 'Height', <Controller name="height" control={form.control} render={({ field: input }) => <InputNumber {...input} aria-label="Height" min={0.0001} placeholder="e.g. 1.0" style={{ width: 120 }} />} />)}
            {field('dimensionUnit', 'Dimension unit', <Controller name="dimensionUnit" control={form.control} render={({ field: input }) => <Select {...input} aria-label="Dimension unit" options={[{ value: 'M', label: 'm (Meters)' }, { value: 'CM', label: 'cm (Centimeters)' }, { value: 'MM', label: 'mm (Millimeters)' }]} style={{ width: 140 }} />} />)}
          </Flex>
          <Divider orientation="left">Special cargo classification</Divider>
          <Flex gap={32} wrap>{field('fragile', 'Fragile', <Controller name="fragile" control={form.control} render={({ field: input }) => <div role="radiogroup" aria-label="Fragile"><Radio.Group aria-label="Fragile" value={input.value} onChange={input.onChange} options={[{ label: 'Yes', value: true }, { label: 'No', value: false }]} /></div>} />)}{field('temperatureSensitive', 'Temperature sensitive', <Controller name="temperatureSensitive" control={form.control} render={({ field: input }) => <div role="radiogroup" aria-label="Temperature sensitive"><Radio.Group aria-label="Temperature sensitive" value={input.value} onChange={input.onChange} options={[{ label: 'Yes', value: true }, { label: 'No', value: false }]} /></div>} />)}</Flex>
          <Divider orientation="left">Customs</Divider>
          {field('customsApplicable', 'Customs applicable', <Controller name="customsApplicable" control={form.control} render={({ field: input }) => <Checkbox checked={input.value} onChange={event => input.onChange(event.target.checked)}>Customs information applies</Checkbox>} />)}
          {customsApplicable && field('customsInformation', 'Customs information', <Controller name="customsInformation" control={form.control} render={({ field: input }) => <Input.TextArea {...input} aria-label="Customs information" />} />)}
          <Divider orientation="left">Hazardous goods</Divider>
          {field('hazardous', 'Hazardous cargo', <Controller name="hazardous" control={form.control} render={({ field: input }) => <Checkbox checked={input.value} onChange={event => input.onChange(event.target.checked)}>Hazardous-goods information applies</Checkbox>} />)}
          {hazardous && <>{field('hazardousClassification', 'Hazardous classification', <Controller name="hazardousClassification" control={form.control} render={({ field: input }) => <Input {...input} aria-label="Hazardous classification" />} />)}{field('hazardousDetails', 'Hazardous details', <Controller name="hazardousDetails" control={form.control} render={({ field: input }) => <Input.TextArea {...input} aria-label="Hazardous details" />} />)}</>}
        </Form>
      </Modal>
    </>}
  </Flex>;
}
