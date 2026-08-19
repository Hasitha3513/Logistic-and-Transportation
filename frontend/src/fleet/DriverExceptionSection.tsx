import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  UserOutlined,
} from '@ant-design/icons';
import {
  Alert,
  App as AntApp,
  Button,
  Card,
  DatePicker,
  Flex,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from 'antd';
import dayjs from 'dayjs';
import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import type {
  DriverException,
  DriverExceptionPatchRequest,
  DriverExceptionRequest,
  DriverExceptionStatus,
  DriverExceptionType,
} from './types';
import {
  useCancelDriverException,
  useCompleteDriverException,
  useCreateDriverException,
  useDriverExceptions,
  useUpdateDriverException,
} from './useDriverExceptions';

const { RangePicker } = DatePicker;
const { Text } = Typography;

interface DriverExceptionSectionProps {
  driverId: string;
}

function statusTag(status: DriverExceptionStatus) {
  switch (status) {
    case 'SCHEDULED':
      return <Tag color="processing">Scheduled</Tag>;
    case 'ACTIVE':
      return <Tag color="warning">Active</Tag>;
    case 'COMPLETED':
      return <Tag color="success">Completed</Tag>;
    case 'CANCELLED':
      return <Tag color="default">Cancelled</Tag>;
    default:
      return <Tag>{status}</Tag>;
  }
}

function typeTag(type: DriverExceptionType) {
  switch (type) {
    case 'LEAVE':
      return <Tag color="blue">Leave</Tag>;
    case 'DISCIPLINARY_SUSPENSION':
      return <Tag color="error">Suspension</Tag>;
    case 'MEDICAL_EMERGENCY':
      return <Tag color="magenta">Medical</Tag>;
    case 'OTHER':
      return <Tag color="purple">Other</Tag>;
    default:
      return <Tag>{type}</Tag>;
  }
}

export default function DriverExceptionSection({ driverId }: DriverExceptionSectionProps) {
  const { message, modal } = AntApp.useApp();
  const { hasPermission } = useAuth();
  const canManage = hasPermission('DRIVER_EXCEPTION_MANAGE');

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editingException, setEditingException] = useState<DriverException | null>(null);

  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();

  const exceptions = useDriverExceptions(driverId);
  const createMutation = useCreateDriverException(driverId);
  const updateMutation = useUpdateDriverException(driverId);
  const cancelMutation = useCancelDriverException(driverId);
  const completeMutation = useCompleteDriverException(driverId);

  const handleCreate = async () => {
    try {
      const values = await createForm.validateFields();
      const [start, end] = values.timeRange as [dayjs.Dayjs, dayjs.Dayjs];

      const payload: DriverExceptionRequest = {
        exceptionType: values.exceptionType,
        startTime: start.toISOString(),
        endTime: end.toISOString(),
        reason: values.reason,
        remarks: values.remarks,
      };

      await createMutation.mutateAsync(payload);
      void message.success('Driver exception created successfully');
      setCreateModalOpen(false);
      createForm.resetFields();
    } catch (error: any) {
      if (error?.errorFields) return;
      const apiMessage = error?.response?.data?.message || 'Failed to create driver exception';
      void message.error(apiMessage);
    }
  };

  const handleEditOpen = (exception: DriverException) => {
    setEditingException(exception);
    editForm.setFieldsValue({
      exceptionType: exception.exceptionType,
      timeRange: [dayjs(exception.startTime), dayjs(exception.endTime)],
      reason: exception.reason,
      remarks: exception.remarks,
    });
  };

  const handleUpdate = async () => {
    if (!editingException) return;
    try {
      const values = await editForm.validateFields();
      const [start, end] = values.timeRange as [dayjs.Dayjs, dayjs.Dayjs];

      const payload: DriverExceptionPatchRequest = {
        exceptionType: values.exceptionType,
        startTime: start.toISOString(),
        endTime: end.toISOString(),
        reason: values.reason,
        remarks: values.remarks,
      };

      await updateMutation.mutateAsync({ exceptionId: editingException.id, payload });
      void message.success('Driver exception updated successfully');
      setEditingException(null);
      editForm.resetFields();
    } catch (error: any) {
      if (error?.errorFields) return;
      const apiMessage = error?.response?.data?.message || 'Failed to update driver exception';
      void message.error(apiMessage);
    }
  };

  const handleCancel = (exception: DriverException) => {
    let remarksInput = '';
    modal.confirm({
      title: 'Cancel Driver Exception',
      content: (
        <Space direction="vertical" style={{ width: '100%', marginTop: 8 }}>
          <Text>Are you sure you want to cancel this driver exception?</Text>
          <Input.TextArea
            placeholder="Cancellation reason/remarks (optional)"
            rows={3}
            onChange={(e) => {
              remarksInput = e.target.value;
            }}
          />
        </Space>
      ),
      okText: 'Cancel Exception',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await cancelMutation.mutateAsync({
            exceptionId: exception.id,
            remarks: remarksInput.trim() || undefined,
          });
          void message.success('Driver exception cancelled');
        } catch (error: any) {
          const apiMessage = error?.response?.data?.message || 'Failed to cancel driver exception';
          void message.error(apiMessage);
        }
      },
    });
  };

  const handleComplete = (exception: DriverException) => {
    let remarksInput = '';
    modal.confirm({
      title: 'Complete Driver Exception',
      content: (
        <Space direction="vertical" style={{ width: '100%', marginTop: 8 }}>
          <Text>Mark this driver exception/leave as completed?</Text>
          <Input.TextArea
            placeholder="Completion remarks (optional)"
            rows={3}
            onChange={(e) => {
              remarksInput = e.target.value;
            }}
          />
        </Space>
      ),
      okText: 'Complete',
      onOk: async () => {
        try {
          await completeMutation.mutateAsync({
            exceptionId: exception.id,
            remarks: remarksInput.trim() || undefined,
          });
          void message.success('Driver exception completed');
        } catch (error: any) {
          const apiMessage = error?.response?.data?.message || 'Failed to complete driver exception';
          void message.error(apiMessage);
        }
      },
    });
  };

  const columns: TableColumnsType<DriverException> = [
    {
      title: 'Type',
      dataIndex: 'exceptionType',
      key: 'type',
      width: 140,
      render: (val: DriverExceptionType) => typeTag(val),
    },
    {
      title: 'Time Window',
      key: 'window',
      render: (_, r) => (
        <Space direction="vertical" size={0}>
          <Text>{dayjs(r.startTime).format('YYYY-MM-DD HH:mm')}</Text>
          <Text type="secondary">to {dayjs(r.endTime).format('YYYY-MM-DD HH:mm')}</Text>
        </Space>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (val: DriverExceptionStatus) => statusTag(val),
    },
    {
      title: 'Reason / Remarks',
      key: 'details',
      render: (_, r) => (
        <Space direction="vertical" size={0}>
          {r.reason && <Text strong>{r.reason}</Text>}
          {r.remarks && <Text type="secondary">{r.remarks}</Text>}
          {!r.reason && !r.remarks && <Text type="secondary">—</Text>}
        </Space>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 170,
      render: (_, record) => {
        if (!canManage) return null;
        const isTerminal = record.status === 'COMPLETED' || record.status === 'CANCELLED';
        if (isTerminal) return <Text type="secondary">—</Text>;

        return (
          <Space size="small">
            <Button
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEditOpen(record)}
            >
              Edit
            </Button>
            {record.status === 'ACTIVE' || record.status === 'SCHEDULED' ? (
              <Button
                size="small"
                icon={<CheckCircleOutlined />}
                onClick={() => handleComplete(record)}
              >
                Done
              </Button>
            ) : null}
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
          <UserOutlined />
          <span>Driver Exceptions & Leave</span>
        </Space>
      }
      extra={
        <Space>
          <Button
            size="small"
            icon={<ReloadOutlined />}
            loading={exceptions.isFetching}
            onClick={() => void exceptions.refetch()}
          >
            Refresh
          </Button>
          {canManage && (
            <Button
              size="small"
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setCreateModalOpen(true)}
            >
              Record Exception
            </Button>
          )}
        </Space>
      }
    >
      <Flex vertical gap="middle">
        <Alert
          type="info"
          showIcon
          message="Availability Impact"
          description="Scheduled and Active exceptions automatically block driver allocations during their time windows. Completed and Cancelled exceptions do not block."
        />

        <Table<DriverException>
          rowKey="id"
          size="small"
          columns={columns}
          dataSource={exceptions.data ?? []}
          loading={exceptions.isLoading}
          pagination={{ pageSize: 5, hideOnSinglePage: true }}
          locale={{ emptyText: 'No exceptions or leaves recorded for this driver' }}
        />
      </Flex>

      {/* Create Modal */}
      <Modal
        title="Record Driver Exception / Leave"
        open={createModalOpen}
        okText="Create"
        confirmLoading={createMutation.isPending}
        onOk={handleCreate}
        onCancel={() => {
          setCreateModalOpen(false);
          createForm.resetFields();
        }}
      >
        <Form form={createForm} layout="vertical" initialValues={{ exceptionType: 'LEAVE' }}>
          <Form.Item
            name="exceptionType"
            label="Exception Type"
            rules={[{ required: true, message: 'Please select exception type' }]}
          >
            <Select>
              <Select.Option value="LEAVE">Leave (Annual / Sick / Personal)</Select.Option>
              <Select.Option value="DISCIPLINARY_SUSPENSION">Disciplinary Suspension</Select.Option>
              <Select.Option value="MEDICAL_EMERGENCY">Medical Emergency</Select.Option>
              <Select.Option value="OTHER">Other Operational Exception</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="timeRange"
            label="Time Window"
            rules={[{ required: true, message: 'Please select start and end date/time' }]}
          >
            <RangePicker
              showTime={{ format: 'HH:mm' }}
              format="YYYY-MM-DD HH:mm"
              style={{ width: '100%' }}
            />
          </Form.Item>

          <Form.Item name="reason" label="Reason / Category">
            <Input placeholder="e.g. Annual Leave, Sick Leave, Family Event" maxLength={255} />
          </Form.Item>

          <Form.Item name="remarks" label="Remarks / Details">
            <Input.TextArea rows={3} placeholder="Additional context or handover notes" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit Modal */}
      <Modal
        title="Reschedule / Edit Driver Exception"
        open={Boolean(editingException)}
        okText="Save Changes"
        confirmLoading={updateMutation.isPending}
        onOk={handleUpdate}
        onCancel={() => {
          setEditingException(null);
          editForm.resetFields();
        }}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item
            name="exceptionType"
            label="Exception Type"
            rules={[{ required: true, message: 'Please select exception type' }]}
          >
            <Select>
              <Select.Option value="LEAVE">Leave</Select.Option>
              <Select.Option value="DISCIPLINARY_SUSPENSION">Disciplinary Suspension</Select.Option>
              <Select.Option value="MEDICAL_EMERGENCY">Medical Emergency</Select.Option>
              <Select.Option value="OTHER">Other Operational Exception</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="timeRange"
            label="Time Window"
            rules={[{ required: true, message: 'Please select start and end date/time' }]}
          >
            <RangePicker
              showTime={{ format: 'HH:mm' }}
              format="YYYY-MM-DD HH:mm"
              style={{ width: '100%' }}
            />
          </Form.Item>

          <Form.Item name="reason" label="Reason">
            <Input maxLength={255} />
          </Form.Item>

          <Form.Item name="remarks" label="Remarks">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
