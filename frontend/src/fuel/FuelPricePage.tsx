import { useState } from 'react';
import {
  Alert,
  App as AntApp,
  Button,
  Card,
  DatePicker,
  Flex,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from 'antd';
import { PlusOutlined, ReloadOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { Navigate } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { useAuth } from '../auth/AuthContext';
import { useFuelPrices, useFuelVendors, useSaveFuelPrice } from './hooks/useFuelPurchases';
import type { FuelPrice } from './purchaseTypes';

export default function FuelPricePage() {
  const { hasPermission } = useAuth();
  const { message } = AntApp.useApp();
  const prices = useFuelPrices();
  const vendors = useFuelVendors();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<FuelPrice>();
  const [form] = Form.useForm();
  const save = useSaveFuelPrice(editing?.id);

  if (!hasPermission('FUEL_PRICE_VIEW')) return <Navigate to="/workspace" replace />;

  const canManage = hasPermission('FUEL_PRICE_MANAGE');

  const show = (price?: FuelPrice) => {
    setEditing(price);
    form.setFieldsValue(
      price
        ? {
            ...price,
            effectiveFrom: dayjs(price.effectiveFrom),
            effectiveTo: price.effectiveTo ? dayjs(price.effectiveTo) : null,
          }
        : { fuelType: 'DIESEL', currencyCode: 'LKR', active: true }
    );
    setOpen(true);
  };

  const submit = async () => {
    try {
      const v = await form.validateFields();
      await save.mutateAsync({
        ...v,
        effectiveFrom: v.effectiveFrom.format('YYYY-MM-DD'),
        effectiveTo: v.effectiveTo?.format('YYYY-MM-DD') ?? null,
      });
      void message.success(editing ? 'Fuel price updated' : 'Fuel price created');
      setOpen(false);
      form.resetFields();
    } catch {
      /* validation or API shown */
    }
  };

  const error = isAxiosError<{ message?: string }>(save.error) ? save.error.response?.data?.message : undefined;

  const columns: TableColumnsType<FuelPrice> = [
    {
      title: 'Vendor',
      dataIndex: 'vendorId',
      render: (id: string) => vendors.data?.find((v) => v.id === id)?.name ?? id,
    },
    { title: 'Fuel type', dataIndex: 'fuelType' },
    {
      title: 'Unit price',
      dataIndex: 'unitPrice',
      align: 'right',
      render: (value: number, row: FuelPrice) =>
        new Intl.NumberFormat(undefined, { style: 'currency', currency: row.currencyCode }).format(value),
    },
    { title: 'Currency', dataIndex: 'currencyCode' },
    { title: 'Effective from', dataIndex: 'effectiveFrom' },
    { title: 'Effective to', dataIndex: 'effectiveTo', render: (v) => v ?? 'Open ended' },
    {
      title: 'State',
      dataIndex: 'active',
      render: (v: boolean) => <Tag color={v ? 'success' : 'default'}>{v ? 'Active' : 'Inactive'}</Tag>,
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, row) =>
        canManage ? (
          <Button type="link" onClick={() => show(row)}>
            Edit
          </Button>
        ) : null,
    },
  ];

  return (
    <Flex vertical gap={18}>
      <Flex align="flex-start" justify="space-between" gap={16} wrap>
        <Space direction="vertical" size={4}>
          <Typography.Text type="secondary">
            Expected vendor prices used for purchase variance comparison.
          </Typography.Text>
          <Tag icon={<SafetyCertificateOutlined />} color={canManage ? 'success' : 'warning'}>
            {canManage ? 'Full management access' : 'Read-only access'}
          </Tag>
        </Space>
        <Space>
          {canManage && (
            <Button type="primary" onClick={() => show()}>
              Add price
            </Button>
          )}
          <Button icon={<ReloadOutlined />} loading={prices.isFetching} onClick={() => void prices.refetch()}>
            Refresh
          </Button>
        </Space>
      </Flex>

      {prices.isError && <Alert type="error" showIcon message="Fuel prices could not be loaded" />}

      <Card className="resource-list-card">
        <Table<FuelPrice>
          rowKey="id"
          loading={prices.isLoading}
          dataSource={prices.data ?? []}
          columns={columns}
          scroll={{ x: 'max-content' }}
          pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (total) => `${total} records` }}
          locale={{ emptyText: 'No fuel prices found' }}
        />
      </Card>

      <Modal
        open={open}
        title={editing ? 'Edit fuel price' : 'Add fuel price'}
        onCancel={() => setOpen(false)}
        onOk={() => void submit()}
        confirmLoading={save.isPending}
      >
        {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}
        <Form form={form} layout="vertical">
          <Form.Item name="vendorId" label="Vendor" rules={[{ required: true, message: 'Vendor is required' }]}>
            <Select
              showSearch
              placeholder="Select vendor"
              optionFilterProp="label"
              options={(vendors.data ?? []).map((v) => ({ value: v.id, label: `${v.code} — ${v.name}` }))}
            />
          </Form.Item>
          <Space align="start" wrap>
            <Form.Item name="fuelType" label="Fuel type" rules={[{ required: true }]}>
              <Select style={{ width: 160 }} options={['DIESEL', 'PETROL', 'ELECTRIC', 'OTHER'].map((value) => ({ value }))} />
            </Form.Item>
            <Form.Item
              name="unitPrice"
              label="Unit price"
              rules={[{ required: true, message: 'Unit price is required' }, { type: 'number', min: 0.0001, message: 'Must be positive' }]}
            >
              <InputNumber min={0} precision={4} style={{ width: 140 }} />
            </Form.Item>
            <Form.Item
              name="currencyCode"
              label="Currency"
              rules={[{ required: true, pattern: /^[A-Za-z]{3}$/, message: '3-letter code' }]}
            >
              <Input maxLength={3} style={{ width: 90 }} />
            </Form.Item>
          </Space>
          <Space wrap>
            <Form.Item name="effectiveFrom" label="Effective from" rules={[{ required: true, message: 'Start date is required' }]}>
              <DatePicker />
            </Form.Item>
            <Form.Item name="effectiveTo" label="Effective to">
              <DatePicker />
            </Form.Item>
          </Space>
          <Form.Item name="active" label="Active" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </Flex>
  );
}
