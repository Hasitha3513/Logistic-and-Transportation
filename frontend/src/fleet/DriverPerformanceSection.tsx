import React from 'react';
import { Alert, Card, Col, Progress, Row, Spin, Statistic, Tag, Typography } from 'antd';
import {
  CheckCircleOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { useDriverPerformance } from './useDriverPerformance';
import type { PerformanceRating } from './types';

const { Text, Title } = Typography;

interface DriverPerformanceSectionProps {
  driverId: string;
}

export const DriverPerformanceSection: React.FC<DriverPerformanceSectionProps> = ({ driverId }) => {
  const { data: performance, isLoading, error } = useDriverPerformance(driverId);

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 24 }}>
        <Spin />
        <div style={{ marginTop: 8 }}>
          <Text type="secondary">Loading driver performance scorecard...</Text>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <Alert
        type="error"
        message="Failed to load driver performance"
        description={(error as Error).message}
        showIcon
        style={{ marginTop: 16 }}
      />
    );
  }

  if (!performance) {
    return null;
  }

  const getRatingTag = (rating: PerformanceRating) => {
    switch (rating) {
      case 'EXCELLENT':
        return <Tag color="success">EXCELLENT</Tag>;
      case 'GOOD':
        return <Tag color="blue">GOOD</Tag>;
      case 'SATISFACTORY':
        return <Tag color="cyan">SATISFACTORY</Tag>;
      case 'NEEDS_IMPROVEMENT':
        return <Tag color="warning">NEEDS IMPROVEMENT</Tag>;
      case 'AT_RISK':
      default:
        return <Tag color="error">AT RISK</Tag>;
    }
  };

  const getSafetyProgressColor = (score: number) => {
    if (score >= 85) return '#52c41a';
    if (score >= 60) return '#faad14';
    return '#ff4d4f';
  };

  return (
    <div style={{ marginTop: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <Title level={5} style={{ margin: 0 }}>
            Driver Performance Scorecard
          </Title>
          <Text type="secondary" style={{ fontSize: 12 }}>
            Evaluated: {dayjs(performance.evaluatedAt).format('YYYY-MM-DD HH:mm')}
          </Text>
        </div>
        <div>
          <Text style={{ marginRight: 8 }}>Rating:</Text>
          {getRatingTag(performance.overallRating)}
        </div>
      </div>

      <Row gutter={[16, 16]}>
        {/* Safety Score Card */}
        <Col xs={24} sm={12} md={8}>
          <Card size="small" title="Safety Score" variant="outlined">
            <div style={{ textAlign: 'center', padding: '8px 0' }}>
              <Progress
                type="dashboard"
                percent={performance.safetyScore}
                strokeColor={getSafetyProgressColor(performance.safetyScore)}
                format={(percent) => `${percent}/100`}
                size={110}
              />
              <div style={{ marginTop: 8 }}>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  Based on penalty points & infraction severity
                </Text>
              </div>
            </div>
          </Card>
        </Col>

        {/* Trip Reliability Card */}
        <Col xs={24} sm={12} md={8}>
          <Card size="small" title="Trip Reliability" variant="outlined">
            <Statistic
              title="Completion Rate"
              value={performance.tripCompletionRate}
              precision={1}
              suffix="%"
              prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
            />
            <div style={{ marginTop: 12 }}>
              <Row gutter={8}>
                <Col span={8}>
                  <Statistic title="Assigned" value={performance.totalTripsAssigned} valueStyle={{ fontSize: 14 }} />
                </Col>
                <Col span={8}>
                  <Statistic title="Completed" value={performance.totalTripsCompleted} valueStyle={{ fontSize: 14, color: '#52c41a' }} />
                </Col>
                <Col span={8}>
                  <Statistic title="Cancelled" value={performance.totalTripsCancelled} valueStyle={{ fontSize: 14, color: '#ff4d4f' }} />
                </Col>
              </Row>
            </div>
          </Card>
        </Col>

        {/* Compliance & Fines Card */}
        <Col xs={24} sm={24} md={8}>
          <Card size="small" title="Compliance & Penalties" variant="outlined">
            <Row gutter={[8, 8]}>
              <Col span={12}>
                <Statistic
                  title="Violations"
                  value={performance.totalViolations}
                  prefix={<WarningOutlined style={{ color: performance.totalViolations > 0 ? '#faad14' : '#52c41a' }} />}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="Penalty Points"
                  value={performance.totalPenaltyPoints}
                  valueStyle={{ color: performance.totalPenaltyPoints > 6 ? '#ff4d4f' : undefined }}
                />
              </Col>
              <Col span={12} style={{ marginTop: 8 }}>
                <Statistic
                  title="Total Fines"
                  value={performance.totalFines}
                  precision={2}
                  prefix="$"
                  valueStyle={{ fontSize: 14 }}
                />
              </Col>
              <Col span={12} style={{ marginTop: 8 }}>
                <Statistic
                  title="Unpaid Fines"
                  value={performance.unpaidFines}
                  precision={2}
                  prefix="$"
                  valueStyle={{
                    fontSize: 14,
                    color: performance.unpaidFines > 0 ? '#ff4d4f' : '#52c41a',
                  }}
                />
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default DriverPerformanceSection;
