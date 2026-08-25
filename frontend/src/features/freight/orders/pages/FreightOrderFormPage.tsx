import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, App as AntApp, Button, Card, DatePicker, Flex, Form, Input, InputNumber, Select, Space, Typography } from 'antd';
import { isAxiosError } from 'axios';
import dayjs from 'dayjs';
import { useEffect } from 'react';
import { Controller, useFieldArray, useForm } from 'react-hook-form';
import { Navigate, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../../auth/AuthContext';
import { useFreightCustomers, useFreightLocations, useFreightOrder, useSaveFreightOrder } from '../hooks/useFreightOrders';
import type { FreightOrderPayload } from '../types/freightOrder';
import { freightOrderSchema, type FreightOrderFormValues } from '../validation/freightOrderSchema';

interface ErrorBody { message?: string; fieldErrors?: Array<{ field: string; message: string }> | Record<string, string> }
const initial: FreightOrderFormValues = { customerId: '', originLocationId: '', destinationLocationId: '', requestedPickupAt: '', requestedDeliveryAt: '', serviceLevel: '', priority: '', specialHandlingInstructions: '', lines: [{ description: '', quantity: 1 }] };

export default function FreightOrderFormPage() {
  const { freightOrderId } = useParams(); const editing = Boolean(freightOrderId); const { hasPermission } = useAuth();
  const navigate = useNavigate(); const { message } = AntApp.useApp(); const order = useFreightOrder(freightOrderId);
  const customers = useFreightCustomers(); const locations = useFreightLocations(); const save = useSaveFreightOrder(freightOrderId);
  const form = useForm<FreightOrderFormValues>({ resolver: zodResolver(freightOrderSchema), defaultValues: initial });
  const lines = useFieldArray({ control: form.control, name: 'lines' });
  useEffect(() => { if (order.data) form.reset({ customerId: order.data.customerId, originLocationId: order.data.originLocationId,
    destinationLocationId: order.data.destinationLocationId, requestedPickupAt: order.data.requestedPickupAt,
    requestedDeliveryAt: order.data.requestedDeliveryAt, serviceLevel: order.data.serviceLevel, priority: order.data.priority,
    specialHandlingInstructions: order.data.specialHandlingInstructions ?? '', lines: order.data.lines.map((line) => ({ id: line.id, description: line.description, quantity: Number(line.quantity) })) }); }, [form, order.data]);
  if (!hasPermission('FREIGHT_ORDER_MANAGE')) return <Navigate to={editing ? `/freight/orders/${freightOrderId}` : '/freight/orders'} replace />;
  const referenceOptions = (values?: { id: string; code: string; name: string }[]) => (values ?? []).map((item) => ({ value: item.id, label: `${item.code} — ${item.name}` }));
  const submit = form.handleSubmit(async (values) => {
    const payload: FreightOrderPayload = { ...values, specialHandlingInstructions: values.specialHandlingInstructions || null,
      requestedPickupAt: new Date(values.requestedPickupAt).toISOString(), requestedDeliveryAt: new Date(values.requestedDeliveryAt).toISOString(),
      version: order.data?.version };
    try { const saved = await save.mutateAsync(payload); void message.success(editing ? 'Freight order updated' : 'Freight order created'); navigate(`/freight/orders/${saved.id}`); }
    catch (error) {
      if (isAxiosError<ErrorBody>(error)) {
        const fields = error.response?.data?.fieldErrors;
        const entries = Array.isArray(fields) ? fields.map((item) => [item.field, item.message] as const) : Object.entries(fields ?? {});
        entries.forEach(([field, fieldMessage]) => form.setError(field as keyof FreightOrderFormValues, { message: fieldMessage }));
        form.setError('root', { message: error.response?.data?.message ?? 'The freight order could not be saved' });
      }
    }
  });
  const field = (name: Exclude<keyof FreightOrderFormValues, 'lines'>, label: string, control: React.ReactElement) => <Form.Item label={label} required={name !== 'specialHandlingInstructions'} validateStatus={form.formState.errors[name] ? 'error' : undefined} help={form.formState.errors[name]?.message}>{control}</Form.Item>;
  return <Flex vertical gap={18}>
    <div><Typography.Title level={3}>{editing ? 'Edit freight order' : 'Create freight order'}</Typography.Title><Typography.Text type="secondary">Capture the customer request and minimal shipment lines. No lifecycle state is assigned.</Typography.Text></div>
    {form.formState.errors.root?.message && <Alert type="error" showIcon message="Freight order could not be saved" description={form.formState.errors.root.message} />}
    <Card loading={editing && order.isLoading}><Form layout="vertical" onFinish={() => void submit()}>
      <Flex wrap gap={16}>
        {field('customerId', 'Customer', <Controller name="customerId" control={form.control} render={({ field: input }) => <Select {...input} aria-label="Customer" showSearch optionFilterProp="label" loading={customers.isLoading} options={referenceOptions(customers.data)} style={{ width: 300 }} />} />)}
        {field('originLocationId', 'Origin', <Controller name="originLocationId" control={form.control} render={({ field: input }) => <Select {...input} aria-label="Origin" showSearch optionFilterProp="label" loading={locations.isLoading} options={referenceOptions(locations.data)} style={{ width: 280 }} />} />)}
        {field('destinationLocationId', 'Destination', <Controller name="destinationLocationId" control={form.control} render={({ field: input }) => <Select {...input} aria-label="Destination" showSearch optionFilterProp="label" loading={locations.isLoading} options={referenceOptions(locations.data)} style={{ width: 280 }} />} />)}
        {field('requestedPickupAt', 'Requested pickup', <Controller name="requestedPickupAt" control={form.control} render={({ field: input }) => <DatePicker aria-label="Requested pickup" showTime value={input.value ? dayjs(input.value) : null} onChange={(value) => input.onChange(value?.toISOString() ?? '')} style={{ width: 240 }} />} />)}
        {field('requestedDeliveryAt', 'Requested delivery', <Controller name="requestedDeliveryAt" control={form.control} render={({ field: input }) => <DatePicker aria-label="Requested delivery" showTime value={input.value ? dayjs(input.value) : null} onChange={(value) => input.onChange(value?.toISOString() ?? '')} style={{ width: 240 }} />} />)}
        {field('serviceLevel', 'Service level code', <Controller name="serviceLevel" control={form.control} render={({ field: input }) => <Input {...input} aria-label="Service level code" placeholder="Enter configured code" maxLength={60} style={{ width: 220 }} />} />)}
        {field('priority', 'Priority code', <Controller name="priority" control={form.control} render={({ field: input }) => <Input {...input} aria-label="Priority code" placeholder="Enter configured code" maxLength={40} style={{ width: 220 }} />} />)}
      </Flex>
      {field('specialHandlingInstructions', 'Special handling instructions', <Controller name="specialHandlingInstructions" control={form.control} render={({ field: input }) => <Input.TextArea {...input} aria-label="Special handling instructions" rows={3} maxLength={2000} showCount />} />)}
      <Card size="small" title="Shipment lines" extra={<Button icon={<PlusOutlined />} onClick={() => lines.append({ description: '', quantity: 1 })}>Add line</Button>}>
        {form.formState.errors.lines?.root?.message && <Alert type="error" message={form.formState.errors.lines.root.message} />}
        <Flex vertical gap={12}>{lines.fields.map((line, index) => <Flex key={line.id} gap={12} align="start" wrap>
          <Form.Item label={index === 0 ? 'Description' : undefined} validateStatus={form.formState.errors.lines?.[index]?.description ? 'error' : undefined} help={form.formState.errors.lines?.[index]?.description?.message} style={{ flex: 1, minWidth: 260 }}><Controller name={`lines.${index}.description`} control={form.control} render={({ field: input }) => <Input {...input} aria-label={`Line ${index + 1} description`} maxLength={500} />} /></Form.Item>
          <Form.Item label={index === 0 ? 'Quantity' : undefined} validateStatus={form.formState.errors.lines?.[index]?.quantity ? 'error' : undefined} help={form.formState.errors.lines?.[index]?.quantity?.message}><Controller name={`lines.${index}.quantity`} control={form.control} render={({ field: input }) => <InputNumber {...input} aria-label={`Line ${index + 1} quantity`} min={0.0001} precision={4} style={{ width: 160 }} />} /></Form.Item>
          <Button aria-label={`Remove line ${index + 1}`} danger icon={<DeleteOutlined />} disabled={lines.fields.length === 1} onClick={() => lines.remove(index)} style={{ marginTop: index === 0 ? 30 : 0 }} />
        </Flex>)}</Flex>
      </Card>
      <Space style={{ marginTop: 18 }}><Button type="primary" htmlType="submit" loading={save.isPending}>Save freight order</Button><Button onClick={() => navigate(-1)}>Cancel</Button></Space>
    </Form></Card>
  </Flex>;
}
