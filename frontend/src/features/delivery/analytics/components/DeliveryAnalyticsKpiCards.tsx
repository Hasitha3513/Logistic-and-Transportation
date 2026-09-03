import React from 'react';
import { Card, Col, Row, Statistic, Typography } from 'antd';
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  RedoOutlined,
  StopOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import { DeliveryAnalyticsSummary } from '../types/deliveryAnalytics';

const { Text } = Typography;

interface DeliveryAnalyticsKpiCardsProps {
  summary?: DeliveryAnalyticsSummary;
  loading?: boolean;
}

export const DeliveryAnalyticsKpiCards: React.FC<DeliveryAnalyticsKpiCardsProps> = ({
  summary,
  loading = false,
}) => {
  const formatPercent = (val: number | null | undefined) => {
    if (val === null || val === undefined) return 'N/A';
    return `${val.toFixed(1)}%`;
  };

  const formatNumber = (val: number | null | undefined) => {
    if (val === null || val === undefined) return 'N/A';
    return val.toString();
  };

  return (
    <Row gutter={[16, 16]}>
      <Col xs={24} sm={12} lg={6}>
        <Card loading={loading} style={{ height: '100%' }}>
          <Statistic
            title="Order Success Rate"
            value={formatPercent(summary?.orderSuccessRate)}
            prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
            suffix={
              summary?.terminalCompletedOrders ? (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  ({summary.deliveredOrders}/{summary.terminalCompletedOrders})
                </Text>
              ) : null
            }
          />
          <div style={{ marginTop: 8 }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Delivered: {formatNumber(summary?.deliveredOrders)} | Returned: {formatNumber(summary?.returnedToBaseOrders)}
            </Text>
          </div>
        </Card>
      </Col>

      <Col xs={24} sm={12} lg={6}>
        <Card loading={loading} style={{ height: '100%' }}>
          <Statistic
            title="On-Time Delivery Rate"
            value={formatPercent(summary?.onTimeDeliveryRate)}
            prefix={<ClockCircleOutlined style={{ color: '#1890ff' }} />}
            suffix={
              summary?.deliveredOrders ? (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  ({summary.onTimeDeliveredOrders}/{summary.deliveredOrders})
                </Text>
              ) : null
            }
          />
          <div style={{ marginTop: 8 }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Avg Late Delay: {summary?.averageDelayMinutes != null ? `${summary.averageDelayMinutes} mins` : 'N/A'}
            </Text>
          </div>
        </Card>
      </Col>

      <Col xs={24} sm={12} lg={6}>
        <Card loading={loading} style={{ height: '100%' }}>
          <Statistic
            title="First-Attempt Success"
            value={formatPercent(summary?.firstAttemptSuccessRate)}
            prefix={<SyncOutlined style={{ color: '#722ed1' }} />}
          />
          <div style={{ marginTop: 8 }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Failed Attempts: {formatNumber(summary?.totalFailedAttempts)} (Avg {summary?.averageFailedAttemptsPerOrder ?? 0}/order)
            </Text>
          </div>
        </Card>
      </Col>

      <Col xs={24} sm={12} lg={6}>
        <Card loading={loading} style={{ height: '100%' }}>
          <Statistic
            title="Redelivery Rate"
            value={formatPercent(summary?.redeliveryRate)}
            prefix={<RedoOutlined style={{ color: '#fa8c16' }} />}
          />
          <div style={{ marginTop: 8 }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Redelivery Success: {formatPercent(summary?.redeliverySuccessRate)} | RTO: {formatPercent(summary?.returnToBaseRate)}
            </Text>
          </div>
        </Card>
      </Col>
    </Row>
  );
};
