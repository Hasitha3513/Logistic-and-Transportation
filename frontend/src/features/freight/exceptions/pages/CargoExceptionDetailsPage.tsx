import React, { useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Divider,
  Form,
  Input,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { isAxiosError } from 'axios';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../../auth/AuthContext';
import {
  useCargoException,
  useEscalateException,
  useHoldException,
  useRejectException,
  useReleaseException,
  useResolveException,
} from '../hooks/useCargoExceptions';
import type {
  ExceptionSeverity,
  ExceptionStatus,
} from '../types';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

interface ActionModalFormValues {
  restriction?: string;
  reason?: string;
  resolution?: string;
  correctiveAction?: string;
}

export const CargoExceptionDetailsPage: React.FC = () => {
  const { hasPermission } = useAuth();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [activeModal, setActiveModal] = useState<
    'HOLD' | 'ESCALATE' | 'RELEASE' | 'REJECT' | 'RESOLVE' | null
  >(null);

  const [form] = Form.useForm<ActionModalFormValues>();

  const { data: exception, isLoading, isError } = useCargoException(id || '');

  const holdMutation = useHoldException(id || '');
  const escalateMutation = useEscalateException(id || '');
  const releaseMutation = useReleaseException(id || '');
  const rejectMutation = useRejectException(id || '');
  const resolveMutation = useResolveException(id || '');

  if (!hasPermission('CARGO_EXCEPTION_VIEW')) {
    return (
      <div style={{ padding: '24px' }}>
        <Card>
          <Text type="danger">You do not have permission to view cargo exceptions.</Text>
        </Card>
      </div>
    );
  }

  const getStatusColor = (status: ExceptionStatus) => {
    switch (status) {
      case 'OPEN':
        return 'blue';
      case 'HELD':
        return 'warning';
      case 'ESCALATED':
        return 'purple';
      case 'RESOLVED':
        return 'success';
      case 'REJECTED':
        return 'red';
      default:
        return 'default';
    }
  };

  const getSeverityColor = (severity: ExceptionSeverity) => {
    switch (severity) {
      case 'LOW':
        return 'default';
      case 'MEDIUM':
        return 'blue';
      case 'HIGH':
        return 'orange';
      case 'CRITICAL':
        return 'red';
      default:
        return 'default';
    }
  };

  const handleOpenModal = (modalType: 'HOLD' | 'ESCALATE' | 'RELEASE' | 'REJECT' | 'RESOLVE') => {
    form.resetFields();
    if (modalType === 'HOLD' && exception?.restriction) {
      form.setFieldsValue({ restriction: exception.restriction });
    }
    setActiveModal(modalType);
  };

  const handleActionSubmit = async (values: ActionModalFormValues) => {
    if (!exception) return;

    try {
      if (activeModal === 'HOLD') {
        await holdMutation.mutateAsync({
          restriction: values.restriction?.trim(),
          reason: values.reason?.trim(),
          version: exception.version,
        });
        message.success('Exception placed on HOLD');
      } else if (activeModal === 'ESCALATE') {
        await escalateMutation.mutateAsync({
          reason: values.reason!.trim(),
          version: exception.version,
        });
        message.success('Exception ESCALATED');
      } else if (activeModal === 'RELEASE') {
        await releaseMutation.mutateAsync({
          reason: values.reason!.trim(),
          version: exception.version,
        });
        message.success('Exception RELEASED');
      } else if (activeModal === 'REJECT') {
        await rejectMutation.mutateAsync({
          reason: values.reason!.trim(),
          version: exception.version,
        });
        message.success('Exception REJECTED');
      } else if (activeModal === 'RESOLVE') {
        await resolveMutation.mutateAsync({
          resolution: values.resolution!.trim(),
          correctiveAction: values.correctiveAction?.trim(),
          reason: values.reason?.trim(),
          version: exception.version,
        });
        message.success('Exception RESOLVED');
      }

      setActiveModal(null);
    } catch (err: unknown) {
      const msg = isAxiosError<{ message?: string }>(err)
        ? err.response?.data?.message ?? `Failed to perform action`
        : `Failed to perform action`;
      message.error(msg);
    }
  };

  const historyColumns = [
    {
      title: 'Action',
      dataIndex: 'action',
      key: 'action',
      render: (action: string) => <Tag color="blue">{action}</Tag>,
    },
    {
      title: 'Actor',
      dataIndex: 'actor',
      key: 'actor',
    },
    {
      title: 'Timestamp',
      dataIndex: 'occurredAt',
      key: 'occurredAt',
      render: (ts: string) => new Date(ts).toLocaleString(),
    },
    {
      title: 'Reason / Outcome',
      dataIndex: 'reason',
      key: 'reason',
      render: (reason: string | null) => reason || '—',
    },
    {
      title: 'Details / Restriction',
      dataIndex: 'details',
      key: 'details',
      render: (details: string | null) => details || '—',
    },
  ];

  if (isLoading) {
    return (
      <div style={{ padding: '24px' }}>
        <Card loading={true} />
      </div>
    );
  }

  if (isError || !exception) {
    return (
      <div style={{ padding: '24px' }}>
        <Card>
          <Text type="danger">Failed to load cargo exception details.</Text>
          <Button style={{ marginTop: 16 }} onClick={() => navigate('/freight/exceptions')}>
            Back to Exceptions
          </Button>
        </Card>
      </div>
    );
  }

  const isClosed = exception.status === 'RESOLVED' || exception.status === 'REJECTED';
  const canManage = hasPermission('CARGO_EXCEPTION_MANAGE');

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <Button
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate('/freight/exceptions')}
              style={{ marginRight: 16 }}
            />
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <Title level={3} style={{ margin: 0 }}>{exception.exceptionNumber}</Title>
                <Tag color={getStatusColor(exception.status)}>{exception.status}</Tag>
                <Tag color={getSeverityColor(exception.severity)}>{exception.severity}</Tag>
              </div>
              <Text type="secondary">Type: {exception.exceptionType}</Text>
            </div>
          </div>

          {/* Workflow Action Buttons */}
          {canManage && !isClosed && (
            <Space wrap>
              {(exception.status === 'OPEN' || exception.status === 'ESCALATED') && (
                <Button
                  icon={<PauseCircleOutlined />}
                  onClick={() => handleOpenModal('HOLD')}
                >
                  Hold
                </Button>
              )}

              {exception.status === 'HELD' && (
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  onClick={() => handleOpenModal('RELEASE')}
                >
                  Release
                </Button>
              )}

              {(exception.status === 'OPEN' || exception.status === 'HELD') && (
                <Button
                  icon={<WarningOutlined />}
                  onClick={() => handleOpenModal('ESCALATE')}
                >
                  Escalate
                </Button>
              )}

              <Button
                danger
                icon={<CloseCircleOutlined />}
                onClick={() => handleOpenModal('REJECT')}
              >
                Reject
              </Button>

              <Button
                type="primary"
                style={{ background: '#52c41a', borderColor: '#52c41a' }}
                icon={<CheckCircleOutlined />}
                onClick={() => handleOpenModal('RESOLVE')}
              >
                Resolve
              </Button>
            </Space>
          )}
        </div>

        {/* Details section */}
        <Descriptions bordered column={{ xs: 1, sm: 2, md: 3 }} size="middle">
          <Descriptions.Item label="Exception Type">
            <Text strong>{exception.exceptionType}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Status">
            <Tag color={getStatusColor(exception.status)}>{exception.status}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Severity">
            <Tag color={getSeverityColor(exception.severity)}>{exception.severity}</Tag>
          </Descriptions.Item>

          <Descriptions.Item label="Freight Order ID">
            <a onClick={() => navigate(`/freight/orders/${exception.freightOrderId}`)}>
              {exception.freightOrderId}
            </a>
          </Descriptions.Item>
          <Descriptions.Item label="Manifest ID">
            {exception.manifestId ? (
              <a onClick={() => navigate(`/freight/manifests/${exception.manifestId}`)}>
                {exception.manifestId}
              </a>
            ) : (
              '—'
            )}
          </Descriptions.Item>
          <Descriptions.Item label="Manifest Item ID">
            {exception.manifestItemId || '—'}
          </Descriptions.Item>

          <Descriptions.Item label="Description" span={3}>
            <Paragraph style={{ margin: 0 }}>{exception.description}</Paragraph>
          </Descriptions.Item>

          <Descriptions.Item label="Operational Impact" span={3}>
            {exception.impact || '—'}
          </Descriptions.Item>

          <Descriptions.Item label="Active Restriction" span={3}>
            {exception.restriction ? (
              <Text type="danger" strong>{exception.restriction}</Text>
            ) : (
              'None'
            )}
          </Descriptions.Item>

          <Descriptions.Item label="Initial Corrective Action" span={3}>
            {exception.correctiveAction || '—'}
          </Descriptions.Item>

          {exception.resolution && (
            <Descriptions.Item label="Resolution Outcome" span={3}>
              <Text type="success" strong>{exception.resolution}</Text>
            </Descriptions.Item>
          )}

          {exception.resolvedAt && (
            <Descriptions.Item label="Resolved At">
              {new Date(exception.resolvedAt).toLocaleString()}
            </Descriptions.Item>
          )}

          {exception.resolvedBy && (
            <Descriptions.Item label="Resolved By">
              {exception.resolvedBy}
            </Descriptions.Item>
          )}

          <Descriptions.Item label="Created At">
            {new Date(exception.createdAt).toLocaleString()}
          </Descriptions.Item>
          <Descriptions.Item label="Created By">
            {exception.createdBy}
          </Descriptions.Item>
          <Descriptions.Item label="Version">
            {exception.version}
          </Descriptions.Item>
        </Descriptions>

        <Divider />

        {/* Retained Resolution History (AC3) */}
        <div>
          <Title level={4}>Resolution & Workflow Audit History</Title>
          <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
            Immutable audit record of all holds, escalations, releases, rejections and resolution decisions
          </Text>
          <Table
            dataSource={exception.history || []}
            columns={historyColumns}
            rowKey="id"
            pagination={false}
            size="small"
          />
        </div>

        {/* Action Modals */}
        <Modal
          title={
            activeModal === 'HOLD'
              ? 'Apply Operational Hold'
              : activeModal === 'ESCALATE'
              ? 'Escalate Exception'
              : activeModal === 'RELEASE'
              ? 'Release Exception'
              : activeModal === 'REJECT'
              ? 'Reject Exception'
              : 'Resolve Exception'
          }
          open={activeModal !== null}
          onCancel={() => setActiveModal(null)}
          onOk={() => form.submit()}
          confirmLoading={
            holdMutation.isPending ||
            escalateMutation.isPending ||
            releaseMutation.isPending ||
            rejectMutation.isPending ||
            resolveMutation.isPending
          }
        >
          <Form form={form} layout="vertical" onFinish={handleActionSubmit}>
            {activeModal === 'HOLD' && (
              <>
                <Form.Item
                  name="restriction"
                  label="Dispatch / Movement Restriction"
                  rules={[{ required: true, message: 'Please specify the restriction' }]}
                >
                  <Input placeholder="e.g. Loading bay quarantine, dispatch hold" />
                </Form.Item>
                <Form.Item name="reason" label="Hold Reason">
                  <TextArea rows={3} placeholder="Reason for applying hold..." />
                </Form.Item>
              </>
            )}

            {activeModal === 'ESCALATE' && (
              <Form.Item
                name="reason"
                label="Escalation Reason"
                rules={[{ required: true, message: 'Please input escalation reason' }]}
              >
                <TextArea rows={4} placeholder="Why is senior manager or safety review needed?" />
              </Form.Item>
            )}

            {activeModal === 'RELEASE' && (
              <Form.Item
                name="reason"
                label="Release Justification"
                rules={[{ required: true, message: 'Please provide release reason' }]}
              >
                <TextArea rows={4} placeholder="Verification or safety clearance allowing release..." />
              </Form.Item>
            )}

            {activeModal === 'REJECT' && (
              <Form.Item
                name="reason"
                label="Rejection Reason"
                rules={[{ required: true, message: 'Please provide rejection reason' }]}
              >
                <TextArea rows={4} placeholder="Why is this exception deemed invalid or rejected?" />
              </Form.Item>
            )}

            {activeModal === 'RESOLVE' && (
              <>
                <Form.Item
                  name="resolution"
                  label="Resolution Summary"
                  rules={[{ required: true, message: 'Please provide resolution description' }]}
                >
                  <TextArea rows={3} placeholder="How was the exception resolved (repaired, repackaged, re-weighed)?" />
                </Form.Item>
                <Form.Item name="correctiveAction" label="Corrective Action Taken">
                  <TextArea rows={2} placeholder="Long-term correction or preventive measure..." />
                </Form.Item>
                <Form.Item name="reason" label="Resolution Notes / Inspection Details">
                  <TextArea rows={2} placeholder="Inspection sign-off notes..." />
                </Form.Item>
              </>
            )}
          </Form>
        </Modal>
      </Card>
    </div>
  );
};
