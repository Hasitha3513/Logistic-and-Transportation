import { AreaChartOutlined, CheckCircleOutlined, ClockCircleOutlined, WarningOutlined } from '@ant-design/icons';
import { Alert, Card, Col, DatePicker, Descriptions, Empty, Flex, Row, Spin, Statistic, Tag } from 'antd';
import dayjs from 'dayjs';
import { useState } from 'react';
import { useRoutePerformance } from '../hooks/useRouteHistoryAndDisruptions';

interface RoutePerformanceSectionProps {
  routeId: string;
}

export function RoutePerformanceSection({ routeId }: RoutePerformanceSectionProps) {
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs | null, dayjs.Dayjs | null] | null>(null);

  const from = dateRange?.[0]?.toISOString();
  const to = dateRange?.[1]?.toISOString();

  const { data: perf, isLoading, isError } = useRoutePerformance(routeId, from, to);

  if (isLoading) {
    return (
      <Card size="small" title={<><AreaChartOutlined /> Route Operational Performance</>}>
        <Flex justify="center" style={{ padding: 16 }}>
          <Spin size="small" aria-label="Loading performance analytics" />
        </Flex>
      </Card>
    );
  }

  if (isError) {
    return (
      <Card size="small" title={<><AreaChartOutlined /> Route Operational Performance</>}>
        <Alert type="error" showIcon message="Route performance analytics could not be loaded" />
      </Card>
    );
  }

  const onTimeRate = perf && perf.completedTripCount > 0
    ? Math.round((perf.onTimeTripCount / perf.completedTripCount) * 100)
    : null;

  return (
    <Card
      size="small"
      title={<><AreaChartOutlined /> Route Operational Performance</>}
      extra={
        <DatePicker.RangePicker
          size="small"
          value={dateRange}
          onChange={(dates) => setDateRange(dates as [dayjs.Dayjs | null, dayjs.Dayjs | null] | null)}
          allowClear
        />
      }
    >
      {!perf || perf.totalTripCount === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="No trip operations recorded for this route yet"
        />
      ) : (
        <Flex vertical gap={12}>
          <Row gutter={8}>
            <Col span={6}>
              <Card size="small" variant="outlined">
                <Statistic title="Total Trips" value={perf.totalTripCount} />
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small" variant="outlined">
                <Statistic title="Completed Trips" value={perf.completedTripCount} />
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small" variant="outlined">
                <Statistic
                  title="On-Time Rate"
                  value={onTimeRate !== null ? `${onTimeRate}%` : 'N/A'}
                  valueStyle={{ color: onTimeRate !== null && onTimeRate >= 80 ? '#3f8600' : '#cf1322' }}
                  prefix={onTimeRate !== null && onTimeRate >= 80 ? <CheckCircleOutlined /> : <WarningOutlined />}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small" variant="outlined">
                <Statistic
                  title="Avg Delay (min)"
                  value={perf.averageDelayMinutes ?? 0}
                  precision={1}
                  valueStyle={{ color: (perf.averageDelayMinutes ?? 0) > 15 ? '#cf1322' : '#3f8600' }}
                  prefix={<ClockCircleOutlined />}
                />
              </Card>
            </Col>
          </Row>

          <Descriptions size="small" column={{ xs: 1, sm: 2 }} bordered title="Variance Analysis">
            <Descriptions.Item label="Planned Distance">{perf.plannedDistanceKm} km</Descriptions.Item>
            <Descriptions.Item label="Avg Actual Distance">
              {perf.averageActualDistanceKm != null ? `${perf.averageActualDistanceKm} km` : '—'}
            </Descriptions.Item>

            <Descriptions.Item label="Distance Variance">
              {perf.distanceVarianceKm != null ? (
                <Tag color={perf.distanceVarianceKm <= 0 ? 'green' : 'orange'}>
                  {perf.distanceVarianceKm > 0 ? `+${perf.distanceVarianceKm}` : perf.distanceVarianceKm} km
                  {perf.distanceVariancePercent != null ? ` (${perf.distanceVariancePercent}%)` : ''}
                </Tag>
              ) : '—'}
            </Descriptions.Item>

            <Descriptions.Item label="Planned Duration">{perf.plannedDurationMinutes} min</Descriptions.Item>

            <Descriptions.Item label="Avg Actual Duration">
              {perf.averageActualDurationMinutes != null ? `${perf.averageActualDurationMinutes} min` : '—'}
            </Descriptions.Item>

            <Descriptions.Item label="Duration Variance">
              {perf.durationVarianceMinutes != null ? (
                <Tag color={perf.durationVarianceMinutes <= 0 ? 'green' : 'orange'}>
                  {perf.durationVarianceMinutes > 0 ? `+${perf.durationVarianceMinutes}` : perf.durationVarianceMinutes} min
                  {perf.durationVariancePercent != null ? ` (${perf.durationVariancePercent}%)` : ''}
                </Tag>
              ) : '—'}
            </Descriptions.Item>

            <Descriptions.Item label="On-Time vs Delayed" span={2}>
              <Flex align="center" gap={8}>
                <Tag color="success">{perf.onTimeTripCount} On-Time</Tag>
                <Tag color={perf.delayedTripCount > 0 ? 'error' : 'default'}>{perf.delayedTripCount} Delayed</Tag>
              </Flex>
            </Descriptions.Item>
          </Descriptions>
        </Flex>
      )}
    </Card>
  );
}
