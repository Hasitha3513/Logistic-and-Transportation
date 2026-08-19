import React, { useState } from 'react';
import {
  Card,
  Table,
  Button,
  Tag,
  Space,
  Modal,
  Form,
  Input,
  InputNumber,
  Select,
  DatePicker,
  Alert,
  Typography,
} from 'antd';
import { PlusOutlined, ExperimentOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import {
  useVehicleLubricantLogs,
  useCreateVehicleLubricantLog,
} from './useVehicleLubricantLogs';
import type {
  LubricantLog,
  FluidType,
  MeasurementUnit,
} from './types';
import { useAuth } from '../auth/AuthContext';

const { Text } = Typography;

interface VehicleLubricantSectionProps {
  vehicleId: string;
}

const FLUID_TYPE_OPTIONS: { label: string; value: FluidType; color: string }[] = [
  { label: 'Engine Oil', value: 'ENGINE_OIL', color: 'volcano' },
  { label: 'Transmission Oil', value: 'TRANSMISSION_OIL', color: 'orange' },
  { label: 'Hydraulic Oil', value: 'HYDRAULIC_OIL', color: 'gold' },
  { label: 'Gear Oil', value: 'GEAR_OIL', color: 'lime' },
  { label: 'Coolant', value: 'COOLANT', color: 'blue' },
  { label: 'Brake Fluid', value: 'BRAKE_FLUID', color: 'purple' },
  { label: 'Grease', value: 'GREASE', color: 'magenta' },
  { label: 'Other Fluid', value: 'OTHER', color: 'default' },
];

const UNIT_OPTIONS: { label: string; value: MeasurementUnit }[] = [
  { label: 'Litre (L)', value: 'LITRE' },
  { label: 'Millilitre (mL)', value: 'MILLILITRE' },
  { label: 'Kilogram (kg)', value: 'KILOGRAM' },
  { label: 'Gram (g)', value: 'GRAM' },
];

export const VehicleLubricantSection: React.FC<VehicleLubricantSectionProps> = ({ vehicleId }) => {
  const { hasPermission } = useAuth();
  const canManage = hasPermission('LUBRICANT_LOG_MANAGE') || hasPermission('FLEET_MANAGER') || hasPermission('ADMIN');

  const { data: logs = [], isLoading, error } = useVehicleLubricantLogs(vehicleId);
  const createMutation = useCreateVehicleLubricantLog(vehicleId);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form] = Form.useForm();

  const handleCreate = async (values: any) => {
    await createMutation.mutateAsync({
      fluidType: values.fluidType,
      quantity: values.quantity,
      unit: values.unit,
      recordedAt: values.recordedAt.toISOString(),
      odometerKm: values.odometerKm,
      engineHours: values.engineHours,
      supplierName: values.supplierName,
      referenceNumber: values.referenceNumber,
      remarks: values.remarks,
    });
    setIsModalOpen(false);
    form.resetFields();
  };

  const getFluidTypeTag = (type: FluidType) => {
    const found = FLUID_TYPE_OPTIONS.find((opt) => opt.value === type);
    return <Tag color={found?.color || 'default'}>{found?.label || type}</Tag>;
  };

  const columns = [
    {
      title: 'Fluid / Lubricant Type',
      dataIndex: 'fluidType',
      key: 'fluidType',
      render: (type: FluidType) => getFluidTypeTag(type),
    },
    {
      title: 'Quantity',
      key: 'quantity',
      render: (_: any, record: LubricantLog) => (
        <Text strong>
          {record.quantity} {record.unit.toLowerCase()}
        </Text>
      ),
    },
    {
      title: 'Recorded At',
      dataIndex: 'recordedAt',
      key: 'recordedAt',
      render: (val: string) => dayjs(val).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: 'Odometer (km)',
      dataIndex: 'odometerKm',
      key: 'odometerKm',
      render: (km?: number | null) => (km != null ? `${km.toLocaleString()} km` : '-'),
    },
    {
      title: 'Engine Hours',
      dataIndex: 'engineHours',
      key: 'engineHours',
      render: (hrs?: number | null) => (hrs != null ? `${hrs} hrs` : '-'),
    },
    {
      title: 'Supplier',
      dataIndex: 'supplierName',
      key: 'supplierName',
      render: (name?: string | null) => name || '-',
    },
    {
      title: 'Reference #',
      dataIndex: 'referenceNumber',
      key: 'referenceNumber',
      render: (ref?: string | null) => ref || '-',
    },
    {
      title: 'Remarks',
      dataIndex: 'remarks',
      key: 'remarks',
      ellipsis: true,
      render: (rem?: string | null) => rem || '-',
    },
  ];

  return (
    <Card
      title={
        <Space>
          <ExperimentOutlined />
          <span>Lubricant & Fluid Consumption Logs</span>
        </Space>
      }
      extra={
        canManage && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              form.resetFields();
              form.setFieldsValue({
                unit: 'LITRE',
                recordedAt: dayjs(),
              });
              setIsModalOpen(true);
            }}
          >
            Add Lubricant / Fluid Log
          </Button>
        )
      }
      style={{ marginTop: 16 }}
    >
      {error && (
        <Alert
          type="error"
          message="Failed to load lubricant consumption logs"
          description={(error as Error).message}
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      <Table
        dataSource={logs}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        pagination={{ pageSize: 5 }}
        size="small"
        locale={{ emptyText: 'No lubricant or fluid consumption logs recorded for this vehicle.' }}
      />

      <Modal
        title="Record Lubricant / Fluid Consumption"
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={createMutation.isPending}
        destroyOnHidden
      >
        {createMutation.error && (
          <Alert
            type="error"
            message="Failed to save record"
            description={(createMutation.error as Error).message}
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}

        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item
            name="fluidType"
            label="Fluid / Lubricant Type"
            rules={[{ required: true, message: 'Please select a fluid type' }]}
          >
            <Select placeholder="Select fluid type">
              {FLUID_TYPE_OPTIONS.map((opt) => (
                <Select.Option key={opt.value} value={opt.value}>
                  {opt.label}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item
              name="quantity"
              label="Quantity"
              rules={[
                { required: true, message: 'Please enter quantity' },
                {
                  validator: (_, value) =>
                    value > 0
                      ? Promise.resolve()
                      : Promise.reject(new Error('Quantity must be greater than zero')),
                },
              ]}
            >
              <InputNumber min={0.01} step={0.5} placeholder="e.g. 10.5" style={{ width: 200 }} />
            </Form.Item>

            <Form.Item
              name="unit"
              label="Unit"
              rules={[{ required: true, message: 'Please select unit' }]}
            >
              <Select placeholder="Unit" style={{ width: 160 }}>
                {UNIT_OPTIONS.map((opt) => (
                  <Select.Option key={opt.value} value={opt.value}>
                    {opt.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Space>

          <Form.Item
            name="recordedAt"
            label="Recorded Date & Time"
            rules={[{ required: true, message: 'Please select recorded date and time' }]}
          >
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>

          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item
              name="odometerKm"
              label="Odometer (km)"
              rules={[
                {
                  validator: (_, value) =>
                    value == null || value >= 0
                      ? Promise.resolve()
                      : Promise.reject(new Error('Odometer cannot be negative')),
                },
              ]}
            >
              <InputNumber min={0} placeholder="e.g. 75000" style={{ width: 200 }} />
            </Form.Item>

            <Form.Item
              name="engineHours"
              label="Engine Hours"
              rules={[
                {
                  validator: (_, value) =>
                    value == null || value >= 0
                      ? Promise.resolve()
                      : Promise.reject(new Error('Engine hours cannot be negative')),
                },
              ]}
            >
              <InputNumber min={0} placeholder="e.g. 1500" style={{ width: 160 }} />
            </Form.Item>
          </Space>

          <Form.Item name="supplierName" label="Supplier / Vendor">
            <Input placeholder="e.g. Mobil, Shell, Castrol" maxLength={150} />
          </Form.Item>

          <Form.Item name="referenceNumber" label="Reference / Invoice #">
            <Input placeholder="e.g. INV-2026-99" maxLength={100} />
          </Form.Item>

          <Form.Item name="remarks" label="Remarks / Specifications">
            <Input.TextArea placeholder="e.g. 15W-40 Synthetic blend, oil filter replaced" rows={3} maxLength={1000} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};
