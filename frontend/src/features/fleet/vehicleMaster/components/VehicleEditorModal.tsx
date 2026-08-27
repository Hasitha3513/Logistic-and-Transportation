import { zodResolver } from '@hookform/resolvers/zod';
import { App as AntApp, Input, InputNumber, Modal, Select, Switch } from 'antd';
import axios from 'axios';
import type { ReactNode } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import {
  useCreateVehicle,
  useUpdateVehicle,
  useVehicleReferences,
} from '../hooks/useVehicles';
import type { Vehicle, VehicleApiError, VehicleInput } from '../types/vehicle';
import { vehicleSchema, type VehicleFormValues } from '../validation/vehicleSchema';

interface VehicleEditorModalProps {
  open: boolean;
  vehicle?: Vehicle;
  onClose: () => void;
}

function formValues(vehicle?: Vehicle): VehicleFormValues {
  return {
    registrationNumber: vehicle?.registrationNumber ?? '',
    chassisNumber: vehicle?.chassisNumber ?? '',
    engineNumber: vehicle?.engineNumber ?? '',
    categoryId: vehicle?.categoryId ?? '',
    typeId: vehicle?.typeId ?? '',
    manufacturer: vehicle?.manufacturer ?? '',
    model: vehicle?.model ?? '',
    manufactureYear: vehicle?.manufactureYear ?? null,
    ownershipType: vehicle?.ownershipType ?? 'COMPANY_OWNED',
    operationalStatus: vehicle?.operationalStatus ?? 'AVAILABLE',
    currentOdometerKm: vehicle?.currentOdometerKm ?? null,
    engineHours: vehicle?.engineHours ?? null,
    capacityKg: vehicle?.capacityKg ?? null,
    tareWeightKg: vehicle?.tareWeightKg ?? null,
    grossVehicleWeightKg: vehicle?.grossVehicleWeightKg ?? null,
    cargoVolumeCapacityM3: vehicle?.cargoVolumeCapacityM3 ?? null,
    axleCount: vehicle?.axleCount ?? null,
    maxAxleLoadKg: vehicle?.maxAxleLoadKg ?? null,
    active: vehicle?.active ?? true,
  };
}

export function VehicleEditorModal({ open, vehicle, onClose }: VehicleEditorModalProps) {
  const { message } = AntApp.useApp();
  const createVehicle = useCreateVehicle();
  const updateVehicle = useUpdateVehicle();
  const { categories, types } = useVehicleReferences(open);
  const form = useForm<VehicleFormValues>({
    resolver: zodResolver(vehicleSchema),
    values: formValues(vehicle),
  });
  const categoryId = useWatch({ control: form.control, name: 'categoryId' });
  const pending = createVehicle.isPending || updateVehicle.isPending;
  const title = vehicle ? 'Edit Vehicle registry' : 'Create Vehicle registry';
  const availableTypes = (types.data ?? []).filter((type) => !categoryId || type.categoryId === categoryId);

  const mapApiError = (error: unknown) => {
    if (!axios.isAxiosError<VehicleApiError>(error)) {
      void message.error('Vehicle registry could not be saved');
      return;
    }
    const body = error.response?.data;
    body?.fieldErrors?.forEach(({ field, message: fieldMessage }) => {
      if (field in form.getValues()) form.setError(field as keyof VehicleFormValues, { message: fieldMessage });
    });
    const mappedField: Partial<Record<string, keyof VehicleFormValues>> = {
      VEHICLE_REGISTRATION_DUPLICATE: 'registrationNumber',
      VEHICLE_CHASSIS_DUPLICATE: 'chassisNumber',
      VEHICLE_ENGINE_DUPLICATE: 'engineNumber',
      VEHICLE_STATUS_TRANSITION_INVALID: 'operationalStatus',
      VEHICLE_MASTER_REFERENCE_INVALID: 'typeId',
    };
    const field = body?.code ? mappedField[body.code] : undefined;
    if (field) form.setError(field, { message: body?.message ?? 'The backend rejected this value' });
    void message.error(body?.message ?? 'Vehicle registry could not be saved');
  };

  const submit = form.handleSubmit(async (values) => {
    try {
      const input = values as VehicleInput;
      if (vehicle) await updateVehicle.mutateAsync({ id: vehicle.id, input });
      else await createVehicle.mutateAsync(input);
      void message.success(`${title} saved`);
      onClose();
    } catch (error) {
      mapApiError(error);
    }
  });

  const field = (name: keyof VehicleFormValues, label: string, control: ReactNode, required = false) => (
    <div className="resource-editor-field">
      <label htmlFor={`resource-${name}`}>{label}{required ? ' *' : ''}</label>
      {control}
      {form.formState.errors[name] && <span className="resource-editor-error" role="alert">{form.formState.errors[name]?.message}</span>}
    </div>
  );

  const textInput = (name: keyof VehicleFormValues) => (
    <Controller name={name} control={form.control} render={({ field: input }) => (
      <Input id={`resource-${name}`} value={String(input.value ?? '')} onChange={input.onChange} />
    )} />
  );

  const numberInput = (name: keyof VehicleFormValues, min = 0) => (
    <Controller name={name} control={form.control} render={({ field: input }) => (
      <InputNumber id={`resource-${name}`} value={input.value as number | null | undefined}
        min={min} onChange={input.onChange} className="resource-editor-control" />
    )} />
  );

  return (
    <Modal title={title} open={open} okText="Save" confirmLoading={pending}
      onOk={() => void submit()} onCancel={onClose} destroyOnHidden width={680}>
      <form className="resource-editor-form" onSubmit={(event) => void submit(event)}>
        {field('registrationNumber', 'Registration number', textInput('registrationNumber'), true)}
        {field('chassisNumber', 'Chassis number', textInput('chassisNumber'))}
        {field('engineNumber', 'Engine number', textInput('engineNumber'))}
        {field('categoryId', 'Category', <Controller name="categoryId" control={form.control} render={({ field: input }) => (
          <Select id="resource-categoryId" {...input} loading={categories.isLoading} showSearch optionFilterProp="label"
            options={(categories.data ?? []).map((item) => ({ value: item.id, label: item.name }))}
            onChange={(value) => { input.onChange(value); form.setValue('typeId', ''); }} />
        )} />, true)}
        {field('typeId', 'Vehicle type', <Controller name="typeId" control={form.control} render={({ field: input }) => (
          <Select id="resource-typeId" {...input} loading={types.isLoading} showSearch optionFilterProp="label"
            options={availableTypes.map((item) => ({ value: item.id, label: item.name }))} />
        )} />, true)}
        {field('manufacturer', 'Manufacturer', textInput('manufacturer'))}
        {field('model', 'Model', textInput('model'))}
        {field('manufactureYear', 'Manufacture year', numberInput('manufactureYear', 1900))}
        {field('ownershipType', 'Ownership', <Controller name="ownershipType" control={form.control} render={({ field: input }) => (
          <Select id="resource-ownershipType" {...input} options={[
            { value: 'COMPANY_OWNED', label: 'Company owned' },
            { value: 'LEASED', label: 'Leased / rental' },
          ]} />
        )} />, true)}
        {field('operationalStatus', 'Operational status', <Controller name="operationalStatus" control={form.control} render={({ field: input }) => (
          <Select id="resource-operationalStatus" {...input} options={[
            { value: 'AVAILABLE', label: 'Available' },
            { value: 'ALLOCATED', label: 'Allocated' },
            { value: 'MAINTENANCE', label: 'Maintenance' },
            { value: 'OUT_OF_SERVICE', label: 'Out of service' },
            { value: 'BROKEN_DOWN', label: 'Broken down' },
          ]} />
        )} />, true)}
        {field('currentOdometerKm', 'Current odometer (km)', numberInput('currentOdometerKm'))}
        {field('engineHours', 'Engine hours', numberInput('engineHours'))}
        {field('capacityKg', 'Payload capacity (kg)', numberInput('capacityKg'))}
        {field('tareWeightKg', 'Tare weight (kg)', numberInput('tareWeightKg'))}
        {field('grossVehicleWeightKg', 'Gross vehicle weight (kg)', numberInput('grossVehicleWeightKg'))}
        {field('cargoVolumeCapacityM3', 'Cargo volume capacity (m³)', numberInput('cargoVolumeCapacityM3'))}
        {field('axleCount', 'Axle count', numberInput('axleCount', 1))}
        {field('maxAxleLoadKg', 'Max axle load (kg)', numberInput('maxAxleLoadKg'))}
        {field('active', 'Active', <Controller name="active" control={form.control} render={({ field: input }) => (
          <Switch id="resource-active" checked={input.value} onChange={input.onChange} />
        )} />)}
      </form>
    </Modal>
  );
}
