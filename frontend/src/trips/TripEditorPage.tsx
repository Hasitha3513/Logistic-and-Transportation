import { zodResolver } from '@hookform/resolvers/zod';
import { useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, App as AntApp, Button, Card, DatePicker, Flex, Input, InputNumber, Select, Spin, Typography } from 'antd';
import axios from 'axios';
import dayjs, { type Dayjs } from 'dayjs';
import { Controller, useForm } from 'react-hook-form';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { z } from 'zod';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { Trip } from './types';

const schema = z.object({
  customerId: z.string().optional(), departmentId: z.string().optional(), projectId: z.string().optional(),
  routeId: z.string().optional(), priority: z.string().min(1),
  originLocationId: z.string().min(1, 'Origin is required'),
  destinationLocationId: z.string().min(1, 'Destination is required'),
  requestedStartTime: z.string().min(1, 'Planned start is required'),
  requestedEndTime: z.string().min(1, 'Planned end is required'),
  requiredVehicleTypeId: z.string().optional(), requiredCapacityKg: z.number().nonnegative().optional(),
  cargoDescription: z.string().optional(), passengerCount: z.number().int().nonnegative().optional(),
  customerInstructions: z.string().optional(), notes: z.string().optional(),
}).refine((value) => new Date(value.requestedStartTime) < new Date(value.requestedEndTime), {
  path: ['requestedEndTime'], message: 'Planned end must be after planned start',
}).refine((value) => value.originLocationId !== value.destinationLocationId, {
  path: ['destinationLocationId'], message: 'Destination must differ from origin',
});

type TripForm = z.infer<typeof schema>;
type Reference = Record<string, unknown> & { id: string };
interface ErrorBody { message?: string; fieldErrors?: { field: string; message: string }[] }

const referenceDefinitions = [
  ['customers', '/customers', 'name'], ['departments', '/departments', 'name'],
  ['projects', '/projects', 'name'], ['locations', '/locations', 'name'],
  ['routes', '/routes', 'name'], ['vehicle-types', '/vehicle-types', 'name'],
] as const;

export default function TripEditorPage() {
  const { tripId } = useParams();
  const editing = Boolean(tripId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { message } = AntApp.useApp();
  const { hasPermission } = useAuth();
  const allowed = hasPermission(editing ? 'TRIP_UPDATE' : 'TRIP_CREATE');
  const trip = useQuery({
    queryKey: ['trip', tripId], queryFn: async () => (await api.get<Trip>(`/trips/${tripId}`)).data,
    enabled: editing && allowed,
  });
  const references = useQueries({ queries: referenceDefinitions.map(([key, endpoint]) => ({
    queryKey: [key, 'trip-editor'], queryFn: async () => (await api.get<Reference[]>(endpoint)).data,
    enabled: allowed,
  })) });
  const initial = trip.data;
  const form = useForm<TripForm>({
    resolver: zodResolver(schema),
    values: {
      customerId: initial?.customerId ?? undefined, departmentId: initial?.departmentId ?? undefined,
      projectId: initial?.projectId ?? undefined, routeId: initial?.routeId ?? undefined,
      priority: initial?.priority ?? 'NORMAL', originLocationId: initial?.originLocationId ?? '',
      destinationLocationId: initial?.destinationLocationId ?? '',
      requestedStartTime: initial?.requestedStartTime ?? '', requestedEndTime: initial?.requestedEndTime ?? '',
      requiredVehicleTypeId: initial?.requiredVehicleTypeId ?? undefined,
      requiredCapacityKg: initial?.requiredCapacityKg ?? undefined, cargoDescription: initial?.cargoDescription ?? '',
      passengerCount: initial?.passengerCount ?? 0, customerInstructions: initial?.customerInstructions ?? '',
      notes: initial?.notes ?? '',
    },
  });

  if (!allowed) return <Navigate to="/trips" replace />;
  if (editing && trip.isLoading) return <Flex justify="center"><Spin size="large" /></Flex>;
  if (trip.isError) return <Alert type="error" showIcon message="Trip could not be loaded" />;

  const options = (index: number, label: string) => (references[index]?.data ?? []).map((item) => ({
    value: item.id, label: String(item[label] ?? item.id),
  }));
  const submit = form.handleSubmit(async (values) => {
    try {
      const response = editing ? await api.put<Trip>(`/trips/${tripId}`, values) : await api.post<Trip>('/trips', values);
      await queryClient.invalidateQueries({ queryKey: ['trips'] });
      void message.success(editing ? 'Trip updated' : 'Trip created');
      navigate(`/trips/${response.data.id}`);
    } catch (error) {
      if (axios.isAxiosError<ErrorBody>(error)) {
        error.response?.data.fieldErrors?.forEach((entry) => form.setError(entry.field as keyof TripForm, { message: entry.message }));
        void message.error(error.response?.data.message ?? 'Trip could not be saved');
      }
    }
  });
  const select = (name: keyof TripForm, label: string, optionValues: { value: string; label: string }[], required = false) => (
    <Controller name={name} control={form.control} render={({ field, fieldState }) => <div className="resource-editor-field">
      <label>{label}{required ? ' *' : ''}</label><Select value={field.value as string | undefined} onChange={field.onChange}
        options={optionValues} allowClear showSearch optionFilterProp="label" />
      {fieldState.error && <span className="resource-editor-error">{fieldState.error.message}</span>}
    </div>} />
  );
  const date = (name: 'requestedStartTime' | 'requestedEndTime', label: string) => (
    <Controller name={name} control={form.control} render={({ field, fieldState }) => <div className="resource-editor-field">
      <label>{label} *</label><DatePicker showTime value={field.value ? dayjs(field.value) : null}
        onChange={(value: Dayjs | null) => field.onChange(value?.toISOString() ?? '')} />
      {fieldState.error && <span className="resource-editor-error">{fieldState.error.message}</span>}
    </div>} />
  );

  return <Flex vertical gap={18}>
    <div><Typography.Title level={3}>{editing ? 'Edit trip' : 'Create trip'}</Typography.Title>
      <Typography.Text type="secondary">The backend validates route, period, lifecycle, and assignment-sensitive changes.</Typography.Text></div>
    <Card><form className="trip-editor-grid" onSubmit={(event) => void submit(event)}>
      {select('customerId', 'Customer', options(0, 'name'))}
      {select('departmentId', 'Department', options(1, 'name'))}
      {select('projectId', 'Project', options(2, 'name'))}
      {select('originLocationId', 'Origin', options(3, 'name'), true)}
      {select('destinationLocationId', 'Destination', options(3, 'name'), true)}
      {select('routeId', 'Route', options(4, 'name'))}
      {select('requiredVehicleTypeId', 'Required vehicle type', options(5, 'name'))}
      {select('priority', 'Priority', ['LOW', 'NORMAL', 'HIGH', 'URGENT'].map((value) => ({ value, label: value })))}
      {date('requestedStartTime', 'Planned start')}{date('requestedEndTime', 'Planned end')}
      <Controller name="requiredCapacityKg" control={form.control} render={({ field, fieldState }) => <div className="resource-editor-field"><label>Required capacity (kg)</label><InputNumber min={0} value={field.value} onChange={field.onChange} />{fieldState.error && <span className="resource-editor-error">{fieldState.error.message}</span>}</div>} />
      <Controller name="passengerCount" control={form.control} render={({ field, fieldState }) => <div className="resource-editor-field"><label>Passengers</label><InputNumber min={0} value={field.value} onChange={field.onChange} />{fieldState.error && <span className="resource-editor-error">{fieldState.error.message}</span>}</div>} />
      {(['cargoDescription', 'customerInstructions', 'notes'] as const).map((name) => <Controller key={name} name={name} control={form.control} render={({ field }) => <div className="resource-editor-field trip-editor-wide"><label>{name.replace(/([A-Z])/g, ' $1')}</label><Input.TextArea value={field.value ?? ''} onChange={field.onChange} rows={2} /></div>} />)}
      <Flex className="trip-editor-actions" gap={10}><Link to={editing ? `/trips/${tripId}` : '/trips'}><Button>Cancel</Button></Link><Button type="primary" htmlType="submit" loading={form.formState.isSubmitting}>Save trip</Button></Flex>
    </form></Card>
  </Flex>;
}
