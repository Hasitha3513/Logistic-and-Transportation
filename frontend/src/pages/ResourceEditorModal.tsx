import { zodResolver } from '@hookform/resolvers/zod';
import { useQueries, useQueryClient } from '@tanstack/react-query';
import { App as AntApp, DatePicker, Input, InputNumber, Modal, Select, Switch } from 'antd';
import axios from 'axios';
import dayjs from 'dayjs';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { z } from 'zod';
import { api } from '../api/client';

export type ResourceValues = Record<string, unknown>;

export interface ResourceField {
  name: string;
  label: string;
  kind?: 'text' | 'textarea' | 'number' | 'select' | 'multi-select' | 'switch' | 'date';
  required?: boolean;
  positive?: boolean;
  options?: { value: string; label: string }[];
  referenceEndpoint?: string;
  referenceLabel?: string;
  dependsOn?: string;
  dependsOnKey?: string;
}

interface ApiErrorBody {
  code?: string;
  message?: string;
  fieldErrors?: { field: string; message: string }[];
}

interface ResourceEditorModalProps {
  open: boolean;
  title: string;
  endpoint: string;
  queryKey: string;
  fields: ResourceField[];
  initial?: ResourceValues;
  method?: 'post' | 'put' | 'patch';
  onClose: () => void;
}

function schemaFor(fields: ResourceField[]) {
  const shape: Record<string, z.ZodType> = {};
  fields.forEach((field) => {
    let value: z.ZodType;
    if (field.kind === 'number') {
      value = field.positive
        ? z.number({ message: `${field.label} is required` }).nonnegative(`${field.label} cannot be negative`)
        : z.number({ message: `${field.label} is required` });
    } else if (field.kind === 'switch') {
      value = z.boolean();
    } else if (field.kind === 'multi-select') {
      value = z.array(z.string());
    } else {
      value = field.required
        ? z.string().trim().min(1, `${field.label} is required`)
        : z.string().optional().or(z.literal(''));
    }
    shape[field.name] = field.required || field.kind === 'switch' || field.kind === 'multi-select'
      ? value
      : value.optional().nullable();
  });
  return z.object(shape);
}

function defaults(fields: ResourceField[], initial?: ResourceValues) {
  return Object.fromEntries(fields.map((field) => {
    const supplied = initial?.[field.name];
    if (supplied !== undefined && supplied !== null) return [field.name, supplied];
    if (field.kind === 'switch') return [field.name, true];
    if (field.kind === 'multi-select') return [field.name, []];
    return [field.name, undefined];
  }));
}

export default function ResourceEditorModal({ open, title, endpoint, queryKey, fields, initial, method = 'post', onClose }: ResourceEditorModalProps) {
  const { message } = AntApp.useApp();
  const queryClient = useQueryClient();
  const references = useQueries({
    queries: fields.map((field) => ({
      queryKey: ['editor-reference', field.referenceEndpoint],
      queryFn: async () => (await api.get<ResourceValues[]>(field.referenceEndpoint!)).data,
      enabled: open && Boolean(field.referenceEndpoint),
      staleTime: 30_000,
    })),
  });
  const form = useForm<ResourceValues>({
    resolver: zodResolver(schemaFor(fields)),
    values: defaults(fields, initial),
  });
  const watchedValues = useWatch({ control: form.control });

  const submit = form.handleSubmit(async (values) => {
    try {
      await api.request({ url: endpoint, method, data: values });
      await queryClient.invalidateQueries({ queryKey: [queryKey] });
      void message.success(`${title} saved`);
      onClose();
    } catch (error) {
      if (axios.isAxiosError<ApiErrorBody>(error)) {
        const errorData = error.response?.data;
        errorData?.fieldErrors?.forEach((violation) => form.setError(violation.field, { message: violation.message }));
        if (errorData?.code === 'VEHICLE_REGISTRATION_DUPLICATE') {
          form.setError('registrationNumber', { message: errorData.message || 'Registration number already exists' });
        } else if (errorData?.code === 'VEHICLE_CHASSIS_DUPLICATE') {
          form.setError('chassisNumber', { message: errorData.message || 'Chassis number already exists' });
        } else if (errorData?.code === 'VEHICLE_ENGINE_DUPLICATE') {
          form.setError('engineNumber', { message: errorData.message || 'Engine number already exists' });
        } else if (errorData?.code === 'VEHICLE_STATUS_TRANSITION_INVALID') {
          form.setError('operationalStatus', { message: errorData.message || 'Invalid status transition' });
        } else if (errorData?.code === 'VEHICLE_MASTER_REFERENCE_INVALID') {
          form.setError('typeId', { message: errorData.message || 'Invalid master data reference' });
        }
        void message.error(errorData?.message ?? `${title} could not be saved`);
        return;
      }
      void message.error(`${title} could not be saved`);
    }
  });

  return (
    <Modal title={title} open={open} okText="Save" confirmLoading={form.formState.isSubmitting}
      onOk={() => void submit()} onCancel={onClose} destroyOnHidden>
      <form className="resource-editor-form" onSubmit={(event) => void submit(event)}>
        {fields.map((field, index) => {
          let reference = references[index]?.data ?? [];
          if (field.dependsOn) {
            const parentVal = watchedValues ? watchedValues[field.dependsOn] : undefined;
            if (parentVal) {
              const filterKey = field.dependsOnKey ?? field.dependsOn;
              reference = reference.filter((item) => String(item[filterKey]) === String(parentVal));
            }
          }
          const options = field.options ?? reference.map((item) => ({
            value: String(item.id),
            label: String(item[field.referenceLabel ?? 'name'] ?? item.id),
          }));
          return (
            <Controller key={field.name} name={field.name} control={form.control} render={({ field: control, fieldState }) => (
              <div className="resource-editor-field">
                <label htmlFor={`resource-${field.name}`}>{field.label}{field.required ? ' *' : ''}</label>
                {field.kind === 'number' ? (
                  <InputNumber id={`resource-${field.name}`} value={control.value as number | null | undefined}
                    onChange={control.onChange} min={field.positive ? 0 : undefined} className="resource-editor-control" />
                ) : field.kind === 'select' || field.kind === 'multi-select' ? (
                  <Select id={`resource-${field.name}`} value={control.value as string | string[] | undefined}
                    onChange={control.onChange} options={options} loading={references[index]?.isLoading}
                    mode={field.kind === 'multi-select' ? 'multiple' : undefined} allowClear showSearch
                    optionFilterProp="label" className="resource-editor-control" />
                ) : field.kind === 'switch' ? (
                  <Switch id={`resource-${field.name}`} checked={Boolean(control.value)} onChange={control.onChange} />
                ) : field.kind === 'date' ? (
                  <DatePicker id={`resource-${field.name}`} value={control.value ? dayjs(String(control.value)) : null}
                    onChange={(value) => control.onChange(value?.format('YYYY-MM-DD'))} className="resource-editor-control" />
                ) : field.kind === 'textarea' ? (
                  <Input.TextArea id={`resource-${field.name}`} value={(control.value as string | undefined) ?? ''}
                    onChange={control.onChange} rows={3} />
                ) : (
                  <Input id={`resource-${field.name}`} value={(control.value as string | undefined) ?? ''} onChange={control.onChange} />
                )}
                {fieldState.error && <span className="resource-editor-error">{fieldState.error.message}</span>}
              </div>
            )} />
          );
        })}
      </form>
    </Modal>
  );
}
