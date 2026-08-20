import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  ToolOutlined,
} from '@ant-design/icons';
import {
  Alert,
  App as AntApp,
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from 'antd';
import dayjs from 'dayjs';
import { isAxiosError } from 'axios';
import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import type { MaintenanceSchedule, MaintenanceScheduleRequest, MaintenanceStatus } from './types';
import {
  useCancelMaintenanceSchedule,
  useCompleteMaintenanceSchedule,
  useCreateMaintenanceSchedule,
  useUpdateMaintenanceSchedule,
  useVehicleMaintenanceSchedules,
} from './useVehicleMaintenance';

const { RangePicker } = DatePicker;
const { Text } = Typography;

interface VehicleMaintenanceSectionProps {
  vehicleId: string;
}

function isFormValidationError(error: unknown): boolean {
  return typeof error === 'object' && error !== null && 'errorFields' in error;
}

function apiErrorMessage(error: unknown, fallback: string): string {
  return isAxiosError<{ message?: string }>(error)
    ? error.response?.data?.message ?? fallback
    : fallback;
}

function statusTag(status: MaintenanceStatus) {
  switch (status) {
    case 'SCHEDULED':
      return <Tag color="processing">Scheduled</Tag>;
    case 'IN_PROGRESS':
      return <Tag color="warning">In progress</Tag>;
    case 'COMPLETED':
      return <Tag color="success">Completed</Tag>;
    case 'CANCELLED':
      return <Tag color="default">Cancelled</Tag>;
    default:
      return <Tag>{status}</Tag>;
  }
}

export default function VehicleMaintenanceSection({ vehicleId }: VehicleMaintenanceSectionProps) {
  const { message, modal } = AntApp.useApp();
  const { hasPermission } = useAuth();
  const canManage = hasPermission('VEHICLE_MAINTENANCE_MANAGE');

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState<MaintenanceSchedule | null>(null);

  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();

  const schedules = useVehicleMaintenanceSchedules(vehicleId);
  const createMutation = useCreateMaintenanceSchedule(vehicleId);
  const updateMutation = useUpdateMaintenanceSchedule(vehicleId);
  const cancelMutation = useCancelMaintenanceSchedule(vehicleId);
  const completeMutation = useCompleteMaintenanceSchedule(vehicleId);

  const handleCreate = async () => {
    try {
      const values = await createForm.validateFields();
      const [start, end] = values.scheduleRange as [dayjs.Dayjs, dayjs.Dayjs];

      const payload: MaintenanceScheduleRequest = {
        maintenanceType: values.maintenanceType,
        scheduledStart: start.toISOString(),
        scheduledEnd: end.toISOString(),
        description: values.description,
        serviceProvider: values.serviceProvider,
        cost: values.cost != null ? Number(values.cost) : undefined,
      };

      await createMutation.mutateAsync(payload);
      void message.success('Maintenance schedule created successfully');
      setCreateModalOpen(false);
      createForm.resetFields();
    } catch (error: unknown) {
      if (isFormValidationError(error)) return;
      void message.error(apiErrorMessage(error, 'Failed to create maintenance schedule'));
    }
  };

  const handleUpdate = async () => {
    if (!editingSchedule) return;
    try {
      const values = await editForm.validateFields();
      const [start, end] = values.scheduleRange as [dayjs.Dayjs, dayjs.Dayjs];

      await updateMutation.mutateAsync({
        scheduleId: editingSchedule.id,
        payload: {
          maintenanceType: values.maintenanceType,
          scheduledStart: start.toISOString(),
          scheduledEnd: end.toISOString(),
          description: values.description,
          serviceProvider: values.serviceProvider,
          cost: values.cost != null ? Number(values.cost) : undefined,
        },
      });
      void message.success('Maintenance schedule updated successfully');
      setEditingSchedule(null);
      editForm.resetFields();
    } catch (error: unknown) {
      if (isFormValidationError(error)) return;
      void message.error(apiErrorMessage(error, 'Failed to update maintenance schedule'));
    }
  };

  const handleCancel = (schedule: MaintenanceSchedule) => {
    let remarks = '';
    modal.confirm({
      title: 'Cancel maintenance schedule',
      content: (
        <Space direction="vertical" style={{ width: '100%', marginTop: 12 }}>
          <Text type="secondary">Provide an optional cancellation reason:</Text>
          <Input.TextArea
            rows={3}
            placeholder="Cancellation reason..."
            onChange={(e) => {
              remarks = e.target.value;
            }}
          />
        </Space>
      ),
      okText: 'Confirm cancellation',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await cancelMutation.mutateAsync({ scheduleId: schedule.id, remarks });
          void message.success('Maintenance schedule cancelled');
        } catch (error: unknown) {
          void message.error(apiErrorMessage(error, 'Failed to cancel maintenance schedule'));
        }
      },
    });
  };

  const handleComplete = (schedule: MaintenanceSchedule) => {
    let remarks = '';
    modal.confirm({
      title: 'Complete maintenance schedule',
      content: (
        <Space direction="vertical" style={{ width: '100%', marginTop: 12 }}>
          <Text type="secondary">Provide optional completion remarks:</Text>
          <Input.TextArea
            rows={3}
            placeholder="Completion remarks..."
            onChange={(e) => {
              remarks = e.target.value;
            }}
          />
        </Space>
      ),
      okText: 'Mark completed',
      onOk: async () => {
        try {
          await completeMutation.mutateAsync({ scheduleId: schedule.id, remarks });
          void message.success('Maintenance schedule marked as completed');
        } catch (error: unknown) {
          void message.error(apiErrorMessage(error, 'Failed to complete maintenance schedule'));
        }
      },
    });
  };

  const columns: TableColumnsType<MaintenanceSchedule> = [
    {
      title: 'Type',
      dataIndex: 'maintenanceType',
      key: 'maintenanceType',
      render: (text) => <Text strong>{text}</Text>,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status) => statusTag(status),
    },
    {
      title: 'Scheduled interval',
      key: 'interval',
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text style={{ fontSize: 13 }}>
            {dayjs(record.scheduledStart).format('YYYY-MM-DD HH:mm')} — {dayjs(record.scheduledEnd).format('YYYY-MM-DD HH:mm')}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Provider / Cost',
      key: 'providerCost',
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text>{record.serviceProvider || '—'}</Text>
          {record.cost != null && <Text type="secondary">${Number(record.cost).toFixed(2)}</Text>}
        </Space>
      ),
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (text) => text || '—',
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => {
        if (!canManage) return '—';
        const isTerminal = record.status === 'COMPLETED' || record.status === 'CANCELLED';
        if (isTerminal) return <Text type="secondary">No actions</Text>;

        return (
          <Space direction="horizontal" size="small" wrap>
            <Button
              size="small"
              icon={<EditOutlined />}
              onClick={() => {
                setEditingSchedule(record);
                editForm.setFieldsValue({
                  maintenanceType: record.maintenanceType,
                  scheduleRange: [dayjs(record.scheduledStart), dayjs(record.scheduledEnd)],
                  description: record.description,
                  serviceProvider: record.serviceProvider,
                  cost: record.cost,
                });
              }}
            >
              Reschedule
            </Button>
            <Button
              size="small"
              type="primary"
              icon={<CheckCircleOutlined />}
              onClick={() => handleComplete(record)}
            >
              Complete
            </Button>
            <Button
              size="small"
              danger
              icon={<CloseCircleOutlined />}
              onClick={() => handleCancel(record)}
            >
              Cancel
            </Button>
          </Space>
        );
      },
    },
  ];

  return (
    <Card
      size="small"
      title={
        <Space>
          <ToolOutlined />
          <span>Scheduled maintenance</span>
        </Space>
      }
      extra={
        <Space>
          {canManage && (
            <Button
              size="small"
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => {
                createForm.resetFields();
                createForm.setFieldsValue({
                  scheduleRange: [dayjs().add(1, 'day').startOf('hour'), dayjs().add(2, 'day').startOf('hour')],
                });
                setCreateModalOpen(true);
              }}
            >
              Schedule maintenance
            </Button>
          )}
          <Button
            size="small"
            icon={<ReloadOutlined />}
            loading={schedules.isFetching}
            onClick={() => void schedules.refetch()}
          >
            Refresh
          </Button>
        </Space>
      }
    >
      {schedules.isError && (
        <Alert
          type="error"
          showIcon
          message="Maintenance schedules could not be loaded"
          description="Check backend connectivity and permissions, then retry."
          style={{ marginBottom: 16 }}
        />
      )}

      <Table<MaintenanceSchedule>
        rowKey="id"
        size="small"
        columns={columns}
        dataSource={schedules.data ?? []}
        loading={schedules.isLoading}
        pagination={false}
        locale={{ emptyText: 'No maintenance schedules found' }}
      />

      {/* Create Maintenance Schedule Modal */}
      <Modal
        title="Schedule Preventive Maintenance"
        open={createModalOpen}
        onOk={handleCreate}
        onCancel={() => setCreateModalOpen(false)}
        confirmLoading={createMutation.isPending}
        okText="Create Schedule"
        destroyOnHidden
      >
        <Form form={createForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="maintenanceType"
            label="Maintenance Type"
            rules={[{ required: true, message: 'Please specify the maintenance type' }]}
          >
            <Input placeholder="e.g. 50,000 km Service, Brake Pad Replacement, Engine Overhaul" />
          </Form.Item>

          <Form.Item
            name="scheduleRange"
            label="Scheduled Interval (Start — End)"
            rules={[{ required: true, message: 'Please select scheduled start and end dates' }]}
          >
            <RangePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="serviceProvider" label="Service Provider / Garage">
            <Input placeholder="e.g. Authorized Dealership, Central Workshop" />
          </Form.Item>

          <Form.Item name="cost" label="Estimated Cost">
            <InputNumber min={0} precision={2} prefix="$" style={{ width: '100%' }} placeholder="0.00" />
          </Form.Item>

          <Form.Item name="description" label="Description / Work Order Notes">
            <Input.TextArea rows={3} placeholder="Additional details or scheduled checklist items..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Reschedule / Edit Modal */}
      <Modal
        title="Reschedule / Edit Maintenance"
        open={Boolean(editingSchedule)}
        onOk={handleUpdate}
        onCancel={() => setEditingSchedule(null)}
        confirmLoading={updateMutation.isPending}
        okText="Save Changes"
        destroyOnHidden
      >
        <Form form={editForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="maintenanceType"
            label="Maintenance Type"
            rules={[{ required: true, message: 'Please specify the maintenance type' }]}
          >
            <Input placeholder="e.g. Routine Service" />
          </Form.Item>

          <Form.Item
            name="scheduleRange"
            label="Scheduled Interval (Start — End)"
            rules={[{ required: true, message: 'Please select scheduled start and end dates' }]}
          >
            <RangePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="serviceProvider" label="Service Provider / Garage">
            <Input placeholder="e.g. Central Workshop" />
          </Form.Item>

          <Form.Item name="cost" label="Estimated Cost">
            <InputNumber min={0} precision={2} prefix="$" style={{ width: '100%' }} placeholder="0.00" />
          </Form.Item>

          <Form.Item name="description" label="Description / Work Order Notes">
            <Input.TextArea rows={3} placeholder="Additional notes..." />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
