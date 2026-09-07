import { zodResolver } from '@hookform/resolvers/zod';
import { Form, Input, InputNumber, Modal, Select, Space } from 'antd';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import type { BillingRecord } from '../types';

const createSchema = z.object({ sourceType: z.enum(['TRIP', 'FREIGHT_ORDER']), sourceId: z.string().uuid(),
  currency: z.string().regex(/^[A-Z]{3}$/) });
type CreateValues = z.infer<typeof createSchema>;
export function BillingCreateModal({ open, busy, onClose, onSubmit }: { open: boolean; busy: boolean;
  onClose: () => void; onSubmit: (value: CreateValues) => void }) {
  const form = useForm<CreateValues>({ resolver: zodResolver(createSchema),
    defaultValues: { sourceType: 'TRIP', sourceId: '', currency: 'LKR' } });
  return <Modal title="Create transport billing record" open={open} confirmLoading={busy}
    onCancel={onClose} onOk={form.handleSubmit(onSubmit)} destroyOnClose>
    <Form layout="vertical">
      <Form.Item label="Billable source type" validateStatus={form.formState.errors.sourceType ? 'error' : undefined}>
        <Controller name="sourceType" control={form.control} render={({ field }) => <Select {...field}
          options={[{ value: 'TRIP' }, { value: 'FREIGHT_ORDER' }]} />} />
      </Form.Item>
      <Form.Item label="Source ID" validateStatus={form.formState.errors.sourceId ? 'error' : undefined}
        help={form.formState.errors.sourceId?.message}>
        <Controller name="sourceId" control={form.control} render={({ field }) => <Input {...field} />} />
      </Form.Item>
      <Form.Item label="Currency" validateStatus={form.formState.errors.currency ? 'error' : undefined}>
        <Controller name="currency" control={form.control} render={({ field }) => <Input {...field} maxLength={3} />} />
      </Form.Item>
    </Form>
  </Modal>;
}

const editSchema = z.object({ baseCharge: z.number().positive(), surcharge: z.number().min(0), penalty: z.number().min(0),
  credit: z.number().min(0), taxStatus: z.enum(['SUPPLIED', 'NOT_SUPPLIED']), taxAmount: z.number().min(0),
  taxRate: z.number().min(0), costCentreCode: z.string().min(1).max(80) });
type EditValues = z.infer<typeof editSchema>;
export function BillingEditModal({ record, busy, onClose, onSubmit }: { record?: BillingRecord; busy: boolean;
  onClose: () => void; onSubmit: (value: Record<string, unknown>) => void }) {
  const form = useForm<EditValues>({ resolver: zodResolver(editSchema), defaultValues: { baseCharge: 0,
    surcharge: 0, penalty: 0, credit: 0, taxStatus: 'NOT_SUPPLIED', taxAmount: 0, taxRate: 0,
    costCentreCode: 'OPERATIONS' } });
  const submit = (value: EditValues) => { const subtotal = value.baseCharge + value.surcharge + value.penalty - value.credit;
    onSubmit({ version: record!.version, lines: [line('BASE_CHARGE', 'CONTRACT', value.baseCharge),
      ...(value.surcharge ? [line('SURCHARGE', 'AUTHORIZED_SURCHARGE', value.surcharge)] : []),
      ...(value.penalty ? [line('PENALTY', 'AUTHORIZED_PENALTY', value.penalty)] : []),
      ...(value.credit ? [line('CREDIT_ADJUSTMENT', 'AUTHORIZED_CREDIT', value.credit)] : [])],
    tax: value.taxStatus === 'SUPPLIED' ? { status: 'SUPPLIED', category: 'SUPPLIED_TAX', taxableAmount: subtotal,
      rate: value.taxRate, taxAmount: value.taxAmount, provenance: 'Operator supplied external tax fact',
      snapshotHash: '0'.repeat(64) } : { status: 'NOT_SUPPLIED' },
    costCentres: [{ code: value.costCentreCode, allocationPercent: 100, source: 'Operator allocation' }] }); };
  return <Modal title="Edit draft commercial facts" open={!!record} confirmLoading={busy} onCancel={onClose}
    onOk={form.handleSubmit(submit)} destroyOnClose><Form layout="vertical"><Space wrap>
      {number(form, 'baseCharge', 'Base charge')}{number(form, 'surcharge', 'Surcharge')}
      {number(form, 'penalty', 'Penalty')}{number(form, 'credit', 'Credit adjustment')}
    </Space><Form.Item label="Tax fact status"><Controller name="taxStatus" control={form.control}
      render={({ field }) => <Select {...field} options={[{ value: 'NOT_SUPPLIED' }, { value: 'SUPPLIED' }]} />} />
    </Form.Item><Space wrap>{number(form, 'taxAmount', 'Supplied tax amount')}{number(form, 'taxRate', 'Supplied rate')}</Space>
    <Form.Item label="Cost centre code"><Controller name="costCentreCode" control={form.control}
      render={({ field }) => <Input {...field} maxLength={80} />} /></Form.Item></Form></Modal>;
}
function line(category: string, reasonCode: string, amount: number) { return { category, reasonCode,
  provenance: 'Operator-entered approved billing fact', quantity: 1, unitRate: amount, amount }; }
function number(form: ReturnType<typeof useForm<EditValues>>, name: keyof EditValues, label: string) {
  return <Form.Item label={label} validateStatus={form.formState.errors[name] ? 'error' : undefined}>
    <Controller name={name} control={form.control} render={({ field }) => <InputNumber {...field}
      value={typeof field.value === 'number' ? field.value : undefined} min={0} precision={2} />} />
  </Form.Item>;
}
