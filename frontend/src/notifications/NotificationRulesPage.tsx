import React, { useState } from 'react';
import {
  Alert,
  App,
  Badge,
  Button,
  Flex,
  Popconfirm,
  Space,
  Switch,
  Table,
  Tag,
  Tabs,
  Tooltip,
  Typography,
} from 'antd';
import {
  BellOutlined,
  DeleteOutlined,
  EditOutlined,
  MailOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { useAuth } from '../auth/AuthContext';
import type { NotificationRule, NotificationSeverity } from './types';
import {
  useDeleteNotificationRule,
  useDisableNotificationRule,
  useEnableNotificationRule,
  useNotificationRules,
  useNotificationEventCatalogue,
} from './useNotificationRules';
import { NotificationRuleModal } from './NotificationRuleModal';
import { NotificationDeliveryDiagnostics } from './NotificationDeliveryDiagnostics';

const { Text, Paragraph } = Typography;

const SEVERITY_COLORS: Record<NotificationSeverity, string> = {
  INFO: 'blue',
  WARNING: 'orange',
  CRITICAL: 'red',
};

export const NotificationRulesPage: React.FC = () => {
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const [modalOpen, setModalOpen] = useState(false);
  const [ruleToEdit, setRuleToEdit] = useState<NotificationRule | null>(null);

  const canView = hasPermission('NOTIFICATION_RULE_VIEW');
  const canManage = hasPermission('NOTIFICATION_RULE_MANAGE');

  const { data: rules = [], isLoading, isError, error, refetch } = useNotificationRules();
  const catalogue = useNotificationEventCatalogue();
  const enableMutation = useEnableNotificationRule();
  const disableMutation = useDisableNotificationRule();
  const deleteMutation = useDeleteNotificationRule();

  if (!canView) {
    return (
      <Alert
        message="Access Denied"
        description="You do not have the NOTIFICATION_RULE_VIEW permission required to view notification rules."
        type="error"
        showIcon
      />
    );
  }

  const handleOpenCreate = () => {
    setRuleToEdit(null);
    setModalOpen(true);
  };

  const handleOpenEdit = (rule: NotificationRule) => {
    setRuleToEdit(rule);
    setModalOpen(true);
  };

  const handleToggleEnable = async (rule: NotificationRule, checked: boolean) => {
    try {
      if (checked) {
        await enableMutation.mutateAsync(rule.id);
        message.success(`Enabled rule "${rule.name}"`);
      } else {
        await disableMutation.mutateAsync(rule.id);
        message.success(`Disabled rule "${rule.name}"`);
      }
    } catch (err: unknown) {
      const apiError = err as { response?: { data?: { message?: string } } };
      message.error(apiError.response?.data?.message ?? 'Failed to update rule status');
    }
  };

  const handleDelete = async (ruleId: string) => {
    try {
      await deleteMutation.mutateAsync(ruleId);
      message.success('Notification rule deleted');
    } catch (err: unknown) {
      const apiError = err as { response?: { data?: { message?: string } } };
      message.error(apiError.response?.data?.message ?? 'Failed to delete rule');
    }
  };

  const columns = [
    {
      title: 'Rule Name & Description',
      key: 'name',
      render: (_: unknown, record: NotificationRule) => (
        <Space direction="vertical" size={2}>
          <Text strong>{record.name}</Text>
          {record.description && <Text type="secondary">{record.description}</Text>}
        </Space>
      ),
    },
    {
      title: 'Event Type',
      dataIndex: 'eventType',
      key: 'eventType',
      render: (eventType: string) => (
        <Tag color="cyan" style={{ fontWeight: 500 }}>
          {eventType}
        </Tag>
      ),
    },
    {
      title: 'Channel',
      dataIndex: 'channel',
      key: 'channel',
      render: (channel: string) => (
        <Space size={6}>
          {channel === 'IN_APP' ? <BellOutlined style={{ color: '#1677ff' }} /> : <MailOutlined style={{ color: '#52c41a' }} />}
          <Text>{channel === 'IN_APP' ? 'In-App' : 'Email'}</Text>
        </Space>
      ),
    },
    {
      title: 'Template',
      dataIndex: 'templateCode',
      key: 'templateCode',
      render: (templateCode: string) => <Text code>{templateCode}</Text>,
    },
    {
      title: 'Recipient',
      key: 'recipient',
      render: (_: unknown, record: NotificationRule) => (
        <Space size={6}>
          <Tag color="purple">{record.recipientType}</Tag>
          <Text code>{record.recipientValue}</Text>
        </Space>
      ),
    },
    {
      title: 'Severity Threshold',
      dataIndex: 'severityThreshold',
      key: 'severityThreshold',
      render: (severity: NotificationSeverity) => (
        <Tag color={SEVERITY_COLORS[severity] ?? 'default'}>
          {severity}
        </Tag>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled: boolean, record: NotificationRule) => {
        if (!canManage) {
          return enabled ? <Badge status="success" text="Enabled" /> : <Badge status="default" text="Disabled" />;
        }
        return (
          <Switch
            checked={enabled}
            onChange={(checked) => handleToggleEnable(record, checked)}
            loading={enableMutation.isPending || disableMutation.isPending}
            aria-label={`Toggle rule ${record.name}`}
          />
        );
      },
    },
    {
      title: 'Policy',
      key: 'policy',
      render: (_: unknown, record: NotificationRule) => {
        const parts = [record.suppressionWindowMinutes ? `Suppress ${record.suppressionWindowMinutes} min` : 'No suppression'];
        if (record.quietHoursEnabled) parts.push(`Quiet ${record.quietDays.join(', ')} ${record.quietStartTime}-${record.quietEndTime}`);
        if (record.escalationEnabled) parts.push(`Fallback ${record.escalationRecipientType} ${record.escalationRecipientValue} after ${record.escalationDelayMinutes} min`);
        return <Tooltip title={parts.join(' · ')}><Space direction="vertical" size={2}>{parts.map((part) => <Text key={part} type="secondary">{part}</Text>)}</Space></Tooltip>;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: unknown, record: NotificationRule) => {
        if (!canManage) return <Text type="secondary">—</Text>;
        return (
          <Space size={8}>
            <Button
              type="text"
              icon={<EditOutlined />}
              onClick={() => handleOpenEdit(record)}
              aria-label={`Edit ${record.name}`}
            >
              Edit
            </Button>
            <Popconfirm
              title="Delete Notification Rule"
              description={`Are you sure you want to delete "${record.name}"?`}
              onConfirm={() => handleDelete(record.id)}
              okText="Delete"
              okButtonProps={{ danger: true }}
              cancelText="Cancel"
            >
              <Button
                type="text"
                danger
                icon={<DeleteOutlined />}
                loading={deleteMutation.isPending}
                aria-label={`Delete ${record.name}`}
              >
                Delete
              </Button>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  return (
    <>
      <Flex justify="space-between" align="center" style={{ marginBottom: 20 }}>
        <div>
          <Paragraph type="secondary" style={{ margin: 0 }}>
            Configure automated event triggers, delivery channels, and recipient routing for operational events.
          </Paragraph>
        </div>
        {canManage && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleOpenCreate}
            aria-label="Create Notification Rule"
          >
            Create Notification Rule
          </Button>
        )}
      </Flex>

      {isError && (
        <Alert
          message="Failed to load notification rules"
          description={String(error)}
          type="error"
          showIcon
          action={<Button size="small" onClick={() => refetch()}>Retry</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      <Tabs items={[
        { key: 'rules', label: 'Rules', children: <Table dataSource={rules} columns={columns} rowKey="id" loading={isLoading}
          scroll={{ x: 1250 }} pagination={{ pageSize: 10, showSizeChanger: true }} locale={{ emptyText: 'No notification rules defined yet.' }} /> },
        { key: 'deliveries', label: 'Delivery Diagnostics', children: <NotificationDeliveryDiagnostics
          eventTypes={(catalogue.data ?? []).map((item) => item.eventType)} canManage={canManage} /> },
      ]} />

      <NotificationRuleModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        ruleToEdit={ruleToEdit}
      />
    </>
  );
};

export default NotificationRulesPage;
