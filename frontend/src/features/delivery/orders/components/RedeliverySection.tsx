import React, { useState } from 'react';
import {
  Card,
  Button,
  Modal,
  Form,
  Input,
  Select,
  DatePicker,
  Table,
  Tag,
  Space,
  Typography,
  Alert,
  message,
  Divider
} from 'antd';
import {
  CalendarOutlined,
  ClockCircleOutlined,
  ReloadOutlined,
  CheckCircleOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import axios from 'axios';
import type { DeliveryOrder } from '../types/deliveryOrder';
import type {
  RedeliverySchedule,
  RedeliverySuggestion,
  RedeliverySchedulingMethod
} from '../types/redelivery';
import { useRedelivery } from '../hooks/useRedelivery';
import { useAuth } from '../../../../auth/AuthContext';

const { Text } = Typography;

interface RedeliverySectionProps {
  delivery: DeliveryOrder;
}

interface ScheduleFormValues {
  schedulingMethod?: RedeliverySchedulingMethod;
  preferenceRange?: [dayjs.Dayjs | null, dayjs.Dayjs | null];
  customerPreferenceNotes?: string;
  scheduledRange?: [dayjs.Dayjs, dayjs.Dayjs];
}

interface RescheduleFormValues {
  scheduledRange?: [dayjs.Dayjs, dayjs.Dayjs];
  supersedeReason?: string;
}

export const RedeliverySection: React.FC<RedeliverySectionProps> = ({ delivery }) => {
  const { hasPermission } = useAuth();
  const canSchedule = hasPermission('DELIVERY_REDELIVERY_SCHEDULE');
  const canView = hasPermission('DELIVERY_REDELIVERY_VIEW') || canSchedule;

  const {
    history,
    isLoadingHistory,
    getSuggestions,
    isGettingSuggestions,
    suggestions,
    scheduleRedelivery,
    isScheduling,
    rescheduleRedelivery,
    isRescheduling
  } = useRedelivery(delivery.id);

  const [isScheduleModalVisible, setIsScheduleModalVisible] = useState(false);
  const [isRescheduleModalVisible, setIsRescheduleModalVisible] = useState(false);
  const [scheduleForm] = Form.useForm<ScheduleFormValues>();
  const [rescheduleForm] = Form.useForm<RescheduleFormValues>();

  const isEligibleForInitialSchedule = delivery.status === 'FAILED_ATTEMPT';
  const isEligibleForReschedule = delivery.status === 'READY_FOR_ASSIGNMENT' && history.some(s => s.status === 'CONFIRMED');

  const handleFetchSuggestions = async () => {
    try {
      const values = scheduleForm.getFieldsValue();
      const prefRange = values.preferenceRange;
      await getSuggestions({
        preferredStartTime: prefRange && prefRange[0] ? prefRange[0].toISOString() : null,
        preferredEndTime: prefRange && prefRange[1] ? prefRange[1].toISOString() : null,
        customerPreferenceNotes: values.customerPreferenceNotes,
      });
      message.success('Fetched slot suggestions');
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        message.error(err.response?.data?.message || 'Failed to fetch suggestions');
      } else {
        message.error('Failed to fetch suggestions');
      }
    }
  };

  const handleApplySuggestion = (sug: RedeliverySuggestion) => {
    scheduleForm.setFieldsValue({
      scheduledRange: [dayjs(sug.startTime), dayjs(sug.endTime)],
      schedulingMethod: 'AUTOMATIC' as RedeliverySchedulingMethod,
    });
    message.info(`Applied suggestion: ${sug.slotLabel}`);
  };

  const handleApplyRescheduleSuggestion = (sug: RedeliverySuggestion) => {
    rescheduleForm.setFieldsValue({
      scheduledRange: [dayjs(sug.startTime), dayjs(sug.endTime)],
    });
    message.info(`Applied suggestion: ${sug.slotLabel}`);
  };

  const handleScheduleSubmit = async (values: ScheduleFormValues) => {
    if (!values.scheduledRange || values.scheduledRange.length < 2) {
      message.error('Please select a valid scheduled delivery window');
      return;
    }
    try {
      const prefRange = values.preferenceRange;
      await scheduleRedelivery({
        expectedVersion: delivery.version,
        schedulingMethod: values.schedulingMethod || 'AGENT_ASSISTED',
        preferredStartTime: prefRange && prefRange[0] ? prefRange[0].toISOString() : null,
        preferredEndTime: prefRange && prefRange[1] ? prefRange[1].toISOString() : null,
        customerPreferenceNotes: values.customerPreferenceNotes || null,
        scheduledStartTime: values.scheduledRange[0].toISOString(),
        scheduledEndTime: values.scheduledRange[1].toISOString(),
      });
      message.success('Re-delivery scheduled successfully');
      setIsScheduleModalVisible(false);
      scheduleForm.resetFields();
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        message.error(err.response?.data?.message || 'Failed to schedule re-delivery');
      } else {
        message.error('Failed to schedule re-delivery');
      }
    }
  };

  const handleRescheduleSubmit = async (values: RescheduleFormValues) => {
    if (!values.scheduledRange || values.scheduledRange.length < 2) {
      message.error('Please select a new scheduled delivery window');
      return;
    }
    try {
      await rescheduleRedelivery({
        expectedVersion: delivery.version,
        supersedeReason: values.supersedeReason || null,
        scheduledStartTime: values.scheduledRange[0].toISOString(),
        scheduledEndTime: values.scheduledRange[1].toISOString(),
      });
      message.success('Re-delivery rescheduled successfully');
      setIsRescheduleModalVisible(false);
      rescheduleForm.resetFields();
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        message.error(err.response?.data?.message || 'Failed to reschedule re-delivery');
      } else {
        message.error('Failed to reschedule re-delivery');
      }
    }
  };

  const historyColumns = [
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const color = status === 'CONFIRMED' ? 'green' : status === 'SUPERSEDED' ? 'orange' : 'red';
        return <Tag color={color}>{status}</Tag>;
      },
    },
    {
      title: 'Method',
      dataIndex: 'schedulingMethod',
      key: 'schedulingMethod',
      render: (method: string) => <Tag color="blue">{method}</Tag>,
    },
    {
      title: 'Scheduled Window',
      key: 'window',
      render: (_: unknown, record: RedeliverySchedule) => (
        <span>
          {dayjs(record.scheduledStartTime).format('YYYY-MM-DD HH:mm')} — {dayjs(record.scheduledEndTime).format('HH:mm')}
        </span>
      ),
    },
    {
      title: 'Customer Preference',
      key: 'preference',
      render: (_: unknown, record: RedeliverySchedule) => (
        <div>
          {record.preferredStartTime && (
            <div style={{ fontSize: 12 }}>
              Pref: {dayjs(record.preferredStartTime).format('YYYY-MM-DD HH:mm')} - {dayjs(record.preferredEndTime).format('HH:mm')}
            </div>
          )}
          {record.customerPreferenceNotes && (
            <div style={{ fontSize: 12, color: '#666' }}>Notes: {record.customerPreferenceNotes}</div>
          )}
          {!record.preferredStartTime && !record.customerPreferenceNotes && <span>—</span>}
        </div>
      ),
    },
    {
      title: 'Scheduled By / At',
      key: 'audit',
      render: (_: unknown, record: RedeliverySchedule) => (
        <div>
          <div>{record.scheduledBy}</div>
          <div style={{ fontSize: 12, color: '#888' }}>{dayjs(record.scheduledAt).format('YYYY-MM-DD HH:mm')}</div>
          {record.supersedeReason && (
            <div style={{ fontSize: 12, color: '#fa8c16' }}>Superseded: {record.supersedeReason}</div>
          )}
        </div>
      ),
    },
  ];

  if (!canView) {
    return null;
  }

  return (
    <Card
      title={
        <Space>
          <CalendarOutlined />
          <span>Re-Delivery Scheduling (US-60)</span>
        </Space>
      }
      extra={
        <Space>
          {isEligibleForInitialSchedule && canSchedule && (
            <Button
              type="primary"
              icon={<ClockCircleOutlined />}
              onClick={() => setIsScheduleModalVisible(true)}
              data-testid="schedule-redelivery-btn"
            >
              Schedule Re-Delivery
            </Button>
          )}
          {isEligibleForReschedule && canSchedule && (
            <Button
              type="default"
              icon={<ReloadOutlined />}
              onClick={() => setIsRescheduleModalVisible(true)}
              data-testid="reschedule-redelivery-btn"
            >
              Reschedule
            </Button>
          )}
        </Space>
      }
      style={{ marginTop: 16 }}
      data-testid="redelivery-section"
    >
      {isEligibleForInitialSchedule && (
        <Alert
          message="Order is eligible for Re-Delivery"
          description="A previous delivery attempt failed. You can use customer preferences and standard depot slot suggestions to schedule the next delivery window."
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      {delivery.status === 'RETURN_TO_BASE' && (
        <Alert
          message="Order Returned to Base"
          description="This delivery has been returned to base and cannot be rescheduled."
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      {delivery.status === 'DELIVERED' && (
        <Alert
          message="Delivery Completed"
          description="This order has been delivered successfully. Re-delivery scheduling is closed."
          type="success"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      <Table
        dataSource={history}
        columns={historyColumns}
        rowKey="id"
        loading={isLoadingHistory}
        pagination={false}
        locale={{ emptyText: 'No re-delivery schedules recorded' }}
        size="small"
      />

      {/* Schedule Modal */}
      <Modal
        title="Schedule Re-Delivery Attempt"
        open={isScheduleModalVisible}
        onCancel={() => setIsScheduleModalVisible(false)}
        footer={null}
        destroyOnClose
        width={650}
      >
        <Form
          form={scheduleForm}
          layout="vertical"
          onFinish={handleScheduleSubmit}
          initialValues={{ schedulingMethod: 'AGENT_ASSISTED' }}
        >
          <Form.Item label="Scheduling Method" name="schedulingMethod" rules={[{ required: true }]}>
            <Select>
              <Select.Option value="AGENT_ASSISTED">Agent Assisted (Manual Window Selection)</Select.Option>
              <Select.Option value="AUTOMATIC">Automatic (Suggested Window)</Select.Option>
            </Select>
          </Form.Item>

          <Divider orientation="left" plain>Customer Preference (Advisory)</Divider>
          <Form.Item label="Preferred Date & Time Window" name="preferenceRange">
            <DatePicker.RangePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="Customer Preference Notes" name="customerPreferenceNotes">
            <Input.TextArea maxLength={500} rows={2} placeholder="e.g. Recipient requested morning delivery" />
          </Form.Item>

          <Button
            type="dashed"
            icon={<ReloadOutlined />}
            onClick={handleFetchSuggestions}
            loading={isGettingSuggestions}
            style={{ marginBottom: 16, width: '100%' }}
          >
            Get Available Slot Suggestions
          </Button>

          {suggestions.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              <Text strong>Available Suggestions (Click to select):</Text>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 8 }}>
                {suggestions.map((sug, idx) => (
                  <Card
                    key={idx}
                    size="small"
                    style={{
                      cursor: sug.available ? 'pointer' : 'not-allowed',
                      borderColor: sug.available ? '#1890ff' : '#d9d9d9',
                      background: sug.available ? '#e6f7ff' : '#fafafa'
                    }}
                    onClick={() => sug.available && handleApplySuggestion(sug)}
                  >
                    <Space direction="horizontal" style={{ width: '100%', justifyContent: 'space-between' }}>
                      <div>
                        <Text strong>{sug.slotLabel}</Text>
                        <div>
                          {dayjs(sug.startTime).format('YYYY-MM-DD HH:mm')} — {dayjs(sug.endTime).format('HH:mm')}
                        </div>
                        <Text type="secondary" style={{ fontSize: 12 }}>{sug.note}</Text>
                      </div>
                      {sug.available ? (
                        <Tag color="green" icon={<CheckCircleOutlined />}>Available</Tag>
                      ) : (
                        <Tag color="red">Capacity Full</Tag>
                      )}
                    </Space>
                  </Card>
                ))}
              </div>
            </div>
          )}

          <Divider orientation="left" plain>Confirmed Scheduled Delivery Window</Divider>
          <Form.Item
            label="Scheduled Window (Mandatory 08:00 - 20:00)"
            name="scheduledRange"
            rules={[{ required: true, message: 'Scheduled delivery window is required' }]}
          >
            <DatePicker.RangePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setIsScheduleModalVisible(false)}>Cancel</Button>
              <Button type="primary" htmlType="submit" loading={isScheduling} data-testid="confirm-schedule-submit-btn">
                Confirm & Schedule
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Reschedule Modal */}
      <Modal
        title="Reschedule Re-Delivery Attempt"
        open={isRescheduleModalVisible}
        onCancel={() => setIsRescheduleModalVisible(false)}
        footer={null}
        destroyOnClose
        width={550}
      >
        <Form form={rescheduleForm} layout="vertical" onFinish={handleRescheduleSubmit}>
          <Button
            type="dashed"
            icon={<ReloadOutlined />}
            onClick={() => getSuggestions()}
            loading={isGettingSuggestions}
            style={{ marginBottom: 16, width: '100%' }}
            data-testid="reschedule-get-suggestions-btn"
          >
            Get Available Slot Suggestions
          </Button>

          {suggestions.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              <Text strong>Available Suggestions (Click to select):</Text>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 8 }}>
                {suggestions.map((sug, idx) => (
                  <Card
                    key={idx}
                    size="small"
                    style={{
                      cursor: sug.available ? 'pointer' : 'not-allowed',
                      borderColor: sug.available ? '#1890ff' : '#d9d9d9',
                      background: sug.available ? '#e6f7ff' : '#fafafa'
                    }}
                    onClick={() => sug.available && handleApplyRescheduleSuggestion(sug)}
                  >
                    <Space direction="horizontal" style={{ width: '100%', justifyContent: 'space-between' }}>
                      <div>
                        <Text strong>{sug.slotLabel}</Text>
                        <div>
                          {dayjs(sug.startTime).format('YYYY-MM-DD HH:mm')} — {dayjs(sug.endTime).format('HH:mm')}
                        </div>
                        <Text type="secondary" style={{ fontSize: 12 }}>{sug.note}</Text>
                      </div>
                      {sug.available ? (
                        <Tag color="green" icon={<CheckCircleOutlined />}>Available</Tag>
                      ) : (
                        <Tag color="red">Capacity Full</Tag>
                      )}
                    </Space>
                  </Card>
                ))}
              </div>
            </div>
          )}

          <Form.Item
            label="New Scheduled Delivery Window"
            name="scheduledRange"
            rules={[{ required: true, message: 'New scheduled delivery window is required' }]}
          >
            <DatePicker.RangePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="Reason for Rescheduling" name="supersedeReason">
            <Input.TextArea maxLength={500} rows={3} placeholder="Explain why the scheduled window is changing" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setIsRescheduleModalVisible(false)}>Cancel</Button>
              <Button type="primary" htmlType="submit" loading={isRescheduling} data-testid="confirm-reschedule-submit-btn">
                Confirm Reschedule
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};
