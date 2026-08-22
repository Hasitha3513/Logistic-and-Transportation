import React, { useState } from 'react';
import {
  Badge,
  App,
  Button,
  Drawer,
  Empty,
  Flex,
  List,
  Space,
  Spin,
  Tag,
  Tooltip,
  Typography,
} from 'antd';
import {
  BellOutlined,
  CheckOutlined,
  InfoCircleOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import type { NotificationItem, NotificationSeverity } from './types';
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
  useUnreadNotificationCount,
} from './useNotifications';

const { Text, Paragraph } = Typography;

const SEVERITY_ICONS: Record<NotificationSeverity, React.ReactNode> = {
  INFO: <InfoCircleOutlined style={{ color: '#1677ff' }} />,
  WARNING: <WarningOutlined style={{ color: '#fa8c16' }} />,
  CRITICAL: <WarningOutlined style={{ color: '#f5222d' }} />,
};

const SEVERITY_COLORS: Record<NotificationSeverity, string> = {
  INFO: 'blue',
  WARNING: 'orange',
  CRITICAL: 'red',
};

export const NotificationCenter: React.FC = () => {
  const { message } = App.useApp();
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const { hasPermission } = useAuth();

  const canView = hasPermission('NOTIFICATION_VIEW');
  const { data: unreadCount = 0 } = useUnreadNotificationCount();
  const { data: notifications = [], isLoading, refetch } = useNotifications(50);
  const markReadMutation = useMarkNotificationRead();
  const markAllReadMutation = useMarkAllNotificationsRead();

  if (!canView) {
    return null;
  }

  const handleOpen = () => {
    setOpen(true);
    void refetch();
  };

  const handleMarkRead = async (e: React.MouseEvent, item: NotificationItem) => {
    e.stopPropagation();
    try {
      await markReadMutation.mutateAsync(item.id);
      message.success('Notification marked as read');
    } catch {
      message.error('Failed to mark notification as read');
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllReadMutation.mutateAsync();
      message.success('All notifications marked as read');
    } catch {
      message.error('Failed to mark all notifications as read');
    }
  };

  const handleNotificationClick = (item: NotificationItem) => {
    if (item.status !== 'READ') {
      void markReadMutation.mutateAsync(item.id);
    }
    if (item.relatedRoute) {
      navigate(item.relatedRoute);
      setOpen(false);
    }
  };

  return (
    <>
      <Tooltip title="Notifications">
        <Badge count={unreadCount} overflowCount={99} offset={[-2, 6]}>
          <Button
            type="text"
            icon={<BellOutlined style={{ fontSize: 18 }} />}
            onClick={handleOpen}
            aria-label={`Open notifications (${unreadCount} unread)`}
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}
          />
        </Badge>
      </Tooltip>

      <Drawer
        title={
          <Flex justify="space-between" align="center">
            <Space>
              <BellOutlined />
              <span>Notifications</span>
              {unreadCount > 0 && <Tag color="blue">{unreadCount} unread</Tag>}
            </Space>
            {notifications.length > 0 && unreadCount > 0 && (
              <Button
                type="link"
                size="small"
                icon={<CheckOutlined />}
                onClick={handleMarkAllRead}
                loading={markAllReadMutation.isPending}
                aria-label="Mark all as read"
              >
                Mark all as read
              </Button>
            )}
          </Flex>
        }
        placement="right"
        width={420}
        onClose={() => setOpen(false)}
        open={open}
        destroyOnClose={false}
      >
        {isLoading ? (
          <Flex justify="center" align="center" style={{ minHeight: 200 }}>
            <Spin size="default" />
          </Flex>
        ) : notifications.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="No notifications received yet"
            style={{ marginTop: 40 }}
          />
        ) : (
          <List
            itemLayout="vertical"
            dataSource={notifications}
            renderItem={(item) => {
              const isUnread = item.status !== 'READ';
              return (
                <List.Item
                  key={item.id}
                  onClick={() => handleNotificationClick(item)}
                  style={{
                    padding: '12px 16px',
                    borderRadius: 8,
                    marginBottom: 8,
                    backgroundColor: isUnread ? '#f0f5ff' : '#fafafa',
                    border: isUnread ? '1px solid #adc6ff' : '1px solid #f0f0f0',
                    cursor: item.relatedRoute ? 'pointer' : 'default',
                    transition: 'all 0.2s ease',
                  }}
                >
                  <Flex justify="space-between" align="flex-start" gap={8}>
                    <Space size={6} align="start">
                      {SEVERITY_ICONS[item.severity] ?? <InfoCircleOutlined />}
                      <div>
                        <Text strong={isUnread} style={{ fontSize: 14 }}>
                          {item.title}
                        </Text>
                      </div>
                    </Space>
                    <Space size={6}>
                      <Tag style={{ margin: 0 }}>{item.channel === 'IN_APP' ? 'In-app' : 'Email'}</Tag>
                      <Tag color={SEVERITY_COLORS[item.severity]} style={{ margin: 0 }}>
                        {item.severity}
                      </Tag>
                      {isUnread && (
                        <Tooltip title="Mark as read">
                          <Button
                            type="text"
                            size="small"
                            icon={<CheckOutlined />}
                            onClick={(e) => handleMarkRead(e, item)}
                            aria-label={`Mark ${item.title} as read`}
                          />
                        </Tooltip>
                      )}
                    </Space>
                  </Flex>

                  <Paragraph
                    ellipsis={{ rows: 2 }}
                    type="secondary"
                    style={{ margin: '6px 0 4px 0', fontSize: 13 }}
                  >
                    {item.message}
                  </Paragraph>

                  <Flex justify="space-between" align="center">
                    <Text type="secondary" style={{ fontSize: 11 }}>
                      {new Date(item.createdAt).toLocaleString()}
                    </Text>
                    <Tag color="cyan" style={{ fontSize: 11 }}>
                      {item.eventType}
                    </Tag>
                  </Flex>
                </List.Item>
              );
            }}
          />
        )}
      </Drawer>
    </>
  );
};

export default NotificationCenter;
