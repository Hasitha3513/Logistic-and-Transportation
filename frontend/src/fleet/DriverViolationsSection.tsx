import React, { useState } from 'react';
import {
  Alert,
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import { PlusOutlined, DollarOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { useAuth } from '../auth/AuthContext';
import {
  useDriverViolations,
  useRecordDriverViolation,
  usePayDriverViolation,
  useWaiveDriverViolation,
  useDisputeDriverViolation,
} from './useDriverViolations';
import type {
  DriverViolation,
  DriverViolationRequest,
  DriverViolationType,
  ViolationSeverity,
} from './types';

const { Text, Title } = Typography;

interface DriverViolationsSectionProps {
  driverId: string;
}

interface ViolationFormValues {
  violationType: DriverViolationType;
  severity: ViolationSeverity;
  violationDate: Dayjs;
  penaltyPoints?: number;
  fineAmount?: number;
  location?: string;
  description?: string;
}

interface PayFineFormValues {
  paidAt?: Dayjs;
  paymentReference?: string;
}

interface ReasonFormValues {
  reason: string;
}

export const DriverViolationsSection: React.FC<DriverViolationsSectionProps> = ({ driverId }) => {
  const { hasPermission } = useAuth();
  const canManage = hasPermission('DRIVER_VIOLATION_MANAGE');

  const { data: violations = [], isLoading, error } = useDriverViolations(driverId);
  const recordMutation = useRecordDriverViolation(driverId);
  const payMutation = usePayDriverViolation(driverId);
  const waiveMutation = useWaiveDriverViolation(driverId);
  const disputeMutation = useDisputeDriverViolation(driverId);

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [payModalViolation, setPayModalViolation] = useState<DriverViolation | null>(null);
  const [waiveModalViolation, setWaiveModalViolation] = useState<DriverViolation | null>(null);
  const [disputeModalViolation, setDisputeModalViolation] = useState<DriverViolation | null>(null);

  const [createForm] = Form.useForm();
  const [payForm] = Form.useForm();
  const [waiveForm] = Form.useForm();
  const [disputeForm] = Form.useForm();

  const handleCreate = async (values: ViolationFormValues) => {
    const payload: DriverViolationRequest = {
      violationType: values.violationType,
      severity: values.severity,
      violationDate: values.violationDate.toISOString(),
      penaltyPoints: values.penaltyPoints || 0,
      fineAmount: values.fineAmount || 0,
      location: values.location,
      description: values.description,
    };
    await recordMutation.mutateAsync(payload);
    createForm.resetFields();
    setCreateModalOpen(false);
  };

  const handlePay = async (values: PayFineFormValues) => {
    if (!payModalViolation) return;
    await payMutation.mutateAsync({
      violationId: payModalViolation.id,
      payload: {
        paidAt: values.paidAt ? values.paidAt.toISOString() : undefined,
        paymentReference: values.paymentReference,
      },
    });
    payForm.resetFields();
    setPayModalViolation(null);
  };

  const handleWaive = async (values: ReasonFormValues) => {
    if (!waiveModalViolation) return;
    await waiveMutation.mutateAsync({
      violationId: waiveModalViolation.id,
      payload: { reason: values.reason },
    });
    waiveForm.resetFields();
    setWaiveModalViolation(null);
  };

  const handleDispute = async (values: ReasonFormValues) => {
    if (!disputeModalViolation) return;
    await disputeMutation.mutateAsync({
      violationId: disputeModalViolation.id,
      payload: { reason: values.reason },
    });
    disputeForm.resetFields();
    setDisputeModalViolation(null);
  };

  const getSeverityTag = (sev: ViolationSeverity) => {
    switch (sev) {
      case 'CRITICAL':
        return <Tag color="error">CRITICAL</Tag>;
      case 'MAJOR':
        return <Tag color="volcano">MAJOR</Tag>;
      case 'MODERATE':
        return <Tag color="warning">MODERATE</Tag>;
      case 'MINOR':
      default:
        return <Tag color="blue">MINOR</Tag>;
    }
  };

  const getStatusTag = (status: string) => {
    switch (status) {
      case 'PAID':
        return <Tag color="success">PAID</Tag>;
      case 'WAIVED':
        return <Tag color="default">WAIVED</Tag>;
      case 'DISPUTED':
        return <Tag color="purple">DISPUTED</Tag>;
      case 'UNPAID':
      default:
        return <Tag color="error">UNPAID</Tag>;
    }
  };

  const columns = [
    {
      title: 'Date',
      dataIndex: 'violationDate',
      key: 'violationDate',
      render: (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: 'Violation Type',
      dataIndex: 'violationType',
      key: 'violationType',
      render: (type: DriverViolationType) => <Tag color="geekblue">{type}</Tag>,
    },
    {
      title: 'Severity',
      dataIndex: 'severity',
      key: 'severity',
      render: (sev: ViolationSeverity) => getSeverityTag(sev),
    },
    {
      title: 'Points',
      dataIndex: 'penaltyPoints',
      key: 'penaltyPoints',
      render: (pts: number) => <Text strong>{pts}</Text>,
    },
    {
      title: 'Fine',
      dataIndex: 'fineAmount',
      key: 'fineAmount',
      render: (amt: number) => `$${Number(amt || 0).toFixed(2)}`,
    },
    {
      title: 'Payment',
      dataIndex: 'paymentStatus',
      key: 'paymentStatus',
      render: (status: string) => getStatusTag(status),
    },
    {
      title: 'Location',
      dataIndex: 'location',
      key: 'location',
      render: (loc: string) => loc || '—',
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: unknown, record: DriverViolation) => {
        if (!canManage || record.paymentStatus === 'PAID' || record.paymentStatus === 'WAIVED') {
          return null;
        }
        return (
          <Space size="small">
            <Button
              size="small"
              type="link"
              icon={<DollarOutlined />}
              onClick={() => {
                setPayModalViolation(record);
                payForm.setFieldsValue({ paidAt: dayjs() });
              }}
            >
              Pay
            </Button>
            <Button
              size="small"
              type="link"
              onClick={() => {
                setWaiveModalViolation(record);
                waiveForm.resetFields();
              }}
            >
              Waive
            </Button>
            {record.paymentStatus !== 'DISPUTED' && (
              <Button
                size="small"
                type="link"
                danger
                onClick={() => {
                  setDisputeModalViolation(record);
                  disputeForm.resetFields();
                }}
              >
                Dispute
              </Button>
            )}
          </Space>
        );
      },
    },
  ];

  return (
    <div style={{ marginTop: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <Title level={5} style={{ margin: 0 }}>
          Traffic Violations & Infractions
        </Title>
        {canManage && (
          <Button
            type="primary"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => {
              createForm.resetFields();
              createForm.setFieldsValue({
                severity: 'MINOR',
                violationType: 'SPEEDING',
                violationDate: dayjs(),
                penaltyPoints: 0,
                fineAmount: 0,
              });
              setCreateModalOpen(true);
            }}
          >
            Record Violation
          </Button>
        )}
      </div>

      {error && (
        <Alert
          type="error"
          message="Failed to load driver violations"
          description={(error as Error).message}
          style={{ marginBottom: 12 }}
          showIcon
        />
      )}

      <Table
        dataSource={violations}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        size="small"
        pagination={{ pageSize: 5 }}
      />

      {/* Record Violation Modal */}
      <Modal
        title="Record Traffic Violation"
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        onOk={() => createForm.submit()}
        confirmLoading={recordMutation.isPending}
      >
        <Form form={createForm} layout="vertical" onFinish={handleCreate}>
          <Form.Item
            name="violationType"
            label="Violation Type"
            rules={[{ required: true, message: 'Please select violation type' }]}
          >
            <Select>
              <Select.Option value="SPEEDING">Speeding</Select.Option>
              <Select.Option value="RED_LIGHT">Red Light Infraction</Select.Option>
              <Select.Option value="RECKLESS_DRIVING">Reckless Driving</Select.Option>
              <Select.Option value="UNAUTHORIZED_STOP">Unauthorized Stop</Select.Option>
              <Select.Option value="LOGBOOK_VIOLATION">Logbook / Hours Violation</Select.Option>
              <Select.Option value="ACCIDENT_FAULT">Accident Fault</Select.Option>
              <Select.Option value="OVERLOADING">Overloading</Select.Option>
              <Select.Option value="OTHER">Other</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="severity"
            label="Severity"
            rules={[{ required: true, message: 'Please select severity' }]}
          >
            <Select>
              <Select.Option value="MINOR">Minor</Select.Option>
              <Select.Option value="MODERATE">Moderate</Select.Option>
              <Select.Option value="MAJOR">Major</Select.Option>
              <Select.Option value="CRITICAL">Critical</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="violationDate"
            label="Violation Date & Time"
            rules={[{ required: true, message: 'Please select violation date' }]}
          >
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="penaltyPoints" label="Penalty Points">
            <InputNumber min={0} max={50} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="fineAmount" label="Fine Amount ($)">
            <InputNumber min={0} precision={2} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="location" label="Location">
            <Input placeholder="e.g. Highway A1, KM 42" />
          </Form.Item>

          <Form.Item name="description" label="Description / Officer Remarks">
            <Input.TextArea rows={3} placeholder="Provide details about the incident" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Pay Fine Modal */}
      <Modal
        title="Record Fine Payment"
        open={Boolean(payModalViolation)}
        onCancel={() => setPayModalViolation(null)}
        onOk={() => payForm.submit()}
        confirmLoading={payMutation.isPending}
      >
        <Form form={payForm} layout="vertical" onFinish={handlePay}>
          <Text style={{ display: 'block', marginBottom: 16 }}>
            Settling fine of <Text strong>${Number(payModalViolation?.fineAmount || 0).toFixed(2)}</Text> for violation on{' '}
            {dayjs(payModalViolation?.violationDate).format('YYYY-MM-DD')}.
          </Text>

          <Form.Item name="paidAt" label="Payment Date">
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="paymentReference" label="Payment Reference / Receipt ID">
            <Input placeholder="e.g. REC-849204" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Waive Fine Modal */}
      <Modal
        title="Waive Violation Fine"
        open={Boolean(waiveModalViolation)}
        onCancel={() => setWaiveModalViolation(null)}
        onOk={() => waiveForm.submit()}
        confirmLoading={waiveMutation.isPending}
      >
        <Form form={waiveForm} layout="vertical" onFinish={handleWaive}>
          <Form.Item
            name="reason"
            label="Reason for Waiver"
            rules={[{ required: true, message: 'Please provide reason for waiving fine' }]}
          >
            <Input.TextArea rows={3} placeholder="e.g. First offense warning or company subsidized" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Dispute Fine Modal */}
      <Modal
        title="Dispute Violation"
        open={Boolean(disputeModalViolation)}
        onCancel={() => setDisputeModalViolation(null)}
        onOk={() => disputeForm.submit()}
        confirmLoading={disputeMutation.isPending}
      >
        <Form form={disputeForm} layout="vertical" onFinish={handleDispute}>
          <Form.Item
            name="reason"
            label="Dispute Reason / Details"
            rules={[{ required: true, message: 'Please provide reason for dispute' }]}
          >
            <Input.TextArea rows={3} placeholder="e.g. Disputing speed radar accuracy with traffic department" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default DriverViolationsSection;
