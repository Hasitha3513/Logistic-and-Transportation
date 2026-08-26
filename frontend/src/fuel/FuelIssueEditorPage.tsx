import { useEffect } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery } from '@tanstack/react-query';
import { Alert, App as AntApp, Button, Card, DatePicker, Flex, Form, Input, InputNumber, Select, Space, Typography } from 'antd';
import dayjs from 'dayjs';
import { Controller, useForm } from 'react-hook-form';
import { Navigate, useNavigate, useParams } from 'react-router-dom';
import { z } from 'zod';
import { isAxiosError } from 'axios';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { Trip, VehicleReference } from '../trips/types';
import { useFuelIssue, useFuelStations, useSaveFuelIssue } from './hooks/useFuelIssues';

const schema = z.object({
  vehicleId: z.string().min(1, 'Vehicle is required'), tripId: z.string().optional(), driverId: z.string().optional(),
  fuelType: z.string().trim().min(1, 'Fuel type is required'), quantity: z.number().positive('Quantity must be greater than zero'),
  unitPrice: z.number().min(0).optional(), stationId: z.string().min(1, 'Station is required'), odometer: z.number().min(0).optional(),
  engineHours: z.number().min(0).optional(), issueDateTime: z.string().min(1), notes: z.string().max(1000).optional(),
});
type Values = z.infer<typeof schema>;

export default function FuelIssueEditorPage() {
  const { fuelIssueId } = useParams();
  const editing = Boolean(fuelIssueId);
  const { hasPermission } = useAuth();
  const { message } = AntApp.useApp();
  const navigate = useNavigate();
  const issue = useFuelIssue(fuelIssueId);
  const stations = useFuelStations();
  const vehicles = useQuery({ queryKey: ['vehicles', 'fuel-reference'], queryFn: async () => (await api.get<VehicleReference[]>('/vehicles')).data });
  const trips = useQuery({ queryKey: ['trips', 'fuel-reference'], queryFn: async () => (await api.get<Trip[]>('/trips')).data });
  const save = useSaveFuelIssue(fuelIssueId);
  const { control, handleSubmit, reset, setValue, setError, formState: { errors } } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: {
    vehicleId: '', tripId: undefined, driverId: undefined, fuelType: 'DIESEL', quantity: 0,
    unitPrice: undefined, stationId: '', odometer: undefined, engineHours: undefined, issueDateTime: new Date().toISOString(), notes: '',
  }});

  useEffect(() => { if (issue.data) reset({ vehicleId: issue.data.vehicle.id, tripId: issue.data.trip?.id ?? undefined,
    driverId: issue.data.driver?.id ?? undefined, fuelType: issue.data.fuelType, quantity: Number(issue.data.quantity),
    unitPrice: issue.data.unitPrice == null ? undefined : Number(issue.data.unitPrice), stationId: issue.data.station.id,
    odometer: issue.data.odometer == null ? undefined : Number(issue.data.odometer), engineHours: issue.data.engineHours == null ? undefined : Number(issue.data.engineHours),
    issueDateTime: issue.data.issueDateTime, notes: issue.data.notes ?? '' }); }, [issue.data, reset]);

  const permitted = editing ? hasPermission('FUEL_ISSUE_UPDATE') : hasPermission('FUEL_ISSUE_CREATE');
  if (!permitted) return <Navigate to="/fuel/issues" replace />;
  if (editing && issue.data && issue.data.status !== 'DRAFT') return <Navigate to={`/fuel/issues/${fuelIssueId}`} replace />;

  const submit = handleSubmit(async (values) => {
    try {
      const saved = await save.mutateAsync({ ...values, tripId: values.tripId || null, driverId: values.driverId || null });
      void message.success(editing ? 'Fuel issue updated' : 'Fuel issue created');
      navigate(`/fuel/issues/${saved.id}`);
    } catch (error: unknown) {
      const fieldErrors = isAxiosError<{ fieldErrors?: Record<string, string> }>(error)
        ? error.response?.data?.fieldErrors : undefined;
      Object.entries(fieldErrors ?? {}).forEach(([field, detail]) => setError(field as keyof Values, { message: detail }));
    }
  });

  const tripOptions = (trips.data ?? []).filter((trip) => ['ASSIGNED', 'DISPATCHED', 'IN_PROGRESS'].includes(trip.status))
    .map((trip) => ({ value: trip.id, label: trip.tripNumber }));
  const selectTrip = (tripId?: string) => { const trip = trips.data?.find((item) => item.id === tripId); if (trip?.vehicleId) setValue('vehicleId', trip.vehicleId); if (trip?.driverId) setValue('driverId', trip.driverId); };
  const item = (name: keyof Values, label: string, child: React.ReactElement) => <Form.Item label={label} validateStatus={errors[name] ? 'error' : undefined} help={errors[name]?.message}>{child}</Form.Item>;

  return <Flex vertical gap={18}><div><Typography.Title level={3}>{editing ? 'Edit fuel issue' : 'Create fuel issue'}</Typography.Title><Typography.Text type="secondary">The backend validates vehicle, trip, station, readings, and configured limits.</Typography.Text></div>
    {save.isError && <Alert type="error" showIcon message={isAxiosError<{ message?: string }>(save.error) ? save.error.response?.data?.message ?? 'Fuel issue could not be saved' : 'Fuel issue could not be saved'} />}
    <Card loading={editing && issue.isLoading}><Form layout="vertical" onFinish={() => void submit()}>
      <Flex wrap gap={16}>{item('vehicleId', 'Vehicle', <Controller name="vehicleId" control={control} render={({ field }) => <Select {...field} showSearch style={{ width: 280 }} options={(vehicles.data ?? []).map((v) => ({ value: v.id, label: v.registrationNumber }))} />} />)}
        {item('tripId', 'Trip (optional)', <Controller name="tripId" control={control} render={({ field }) => <Select {...field} allowClear showSearch style={{ width: 280 }} options={tripOptions} onChange={(value) => { field.onChange(value); selectTrip(value); }} />} />)}
        {item('stationId', 'Station', <Controller name="stationId" control={control} render={({ field }) => <Select {...field} style={{ width: 280 }} options={(stations.data ?? []).map((s) => ({ value: s.id, label: `${s.code} — ${s.name}` }))} />} />)}
        {item('fuelType', 'Fuel type', <Controller name="fuelType" control={control} render={({ field }) => <Select {...field} style={{ width: 180 }} options={['DIESEL', 'PETROL', 'ELECTRIC', 'OTHER'].map((value) => ({ value }))} />} />)}
        {item('quantity', 'Quantity', <Controller name="quantity" control={control} render={({ field }) => <InputNumber {...field} min={0} precision={3} style={{ width: 180 }} />} />)}
        {item('unitPrice', 'Unit price', <Controller name="unitPrice" control={control} render={({ field }) => <InputNumber {...field} min={0} precision={4} style={{ width: 180 }} />} />)}
        {item('odometer', 'Odometer', <Controller name="odometer" control={control} render={({ field }) => <InputNumber {...field} min={0} precision={2} style={{ width: 180 }} />} />)}
        {item('engineHours', 'Engine hours', <Controller name="engineHours" control={control} render={({ field }) => <InputNumber {...field} min={0} precision={2} style={{ width: 180 }} />} />)}
        {item('issueDateTime', 'Issue date and time', <Controller name="issueDateTime" control={control} render={({ field }) => <DatePicker showTime value={field.value ? dayjs(field.value) : null} onChange={(value) => field.onChange(value?.toISOString() ?? '')} />} />)}</Flex>
      {item('notes', 'Notes', <Controller name="notes" control={control} render={({ field }) => <Input.TextArea {...field} rows={3} />} />)}
      <Space><Button type="primary" htmlType="submit" loading={save.isPending}>Save draft</Button><Button onClick={() => navigate(-1)}>Cancel</Button></Space>
    </Form></Card></Flex>;
}
