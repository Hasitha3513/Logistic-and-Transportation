import { useState } from 'react';
import { Alert, Card, Col, DatePicker, Flex, Radio, Row, Select, Space, Statistic, Table, Tag, Typography } from 'antd';
import dayjs from 'dayjs';
import { useFuelPerformance } from '../hooks/useFuelPerformance';
import type { DataQuality, DriverPerformance, FuelPerformanceFilters, Metrics, VehiclePerformance } from '../types/fuelPerformance';

const { Paragraph, Text } = Typography;

const value = (number: number | null | undefined, suffix = '') =>
  number == null ? 'N/A' : `${number.toFixed(3)}${suffix}`;

const qualityColor: Record<DataQuality, string> = {
  COMPLETE: 'green', PARTIAL: 'gold', INSUFFICIENT: 'default', INVALID_SOURCE_DATA: 'red',
};

const Quality = ({ metrics }: { metrics: Metrics }) => (
  <Space wrap>
    <Tag color={qualityColor[metrics.quality]}>{metrics.quality.replaceAll('_', ' ')}</Tag>
    {metrics.indicators.map((indicator) => (
      <Tag color="orange" key={indicator}>{indicator.replaceAll('_', ' ')}</Tag>
    ))}
  </Space>
);

export default function FuelPerformancePage() {
  const [filters, setFilters] = useState<FuelPerformanceFilters>({ preset: 30, measurementMode: 'DISTANCE' });
  const queries = useFuelPerformance(filters);
  const summary = queries.summary.data;

  const changePreset = (preset: 7 | 30 | 90) => setFilters({
    preset, measurementMode: filters.measurementMode, fuelType: filters.fuelType,
  });

  return (
    <div>
      <Paragraph type="secondary">
        Tenant-scoped vehicle and driver efficiency with transparent historical baselines and review indicators.
      </Paragraph>
      <Flex gap={12} wrap="wrap" align="center" aria-label="Fuel performance filters">
        <Radio.Group value={filters.preset} onChange={(event) => changePreset(event.target.value)}>
          <Radio.Button value={7}>7 days</Radio.Button>
          <Radio.Button value={30}>30 days</Radio.Button>
          <Radio.Button value={90}>90 days</Radio.Button>
        </Radio.Group>
        <DatePicker.RangePicker
          aria-label="Custom analysis period"
          value={filters.from && filters.to ? [dayjs(filters.from), dayjs(filters.to)] : null}
          onChange={(dates) => setFilters({
            ...filters, preset: undefined,
            from: dates?.[0]?.format('YYYY-MM-DD'), to: dates?.[1]?.format('YYYY-MM-DD'),
          })}
        />
        <Select
          aria-label="Measurement mode"
          value={filters.measurementMode}
          onChange={(measurementMode) => setFilters({ ...filters, measurementMode })}
          options={[{ value: 'DISTANCE', label: 'Distance (L/100km)' },
            { value: 'ENGINE_HOURS', label: 'Engine hours (L/hour)' }]}
        />
      </Flex>

      {summary && (
        <Text type="secondary">Period {summary.period.from}–{summary.period.to} ({summary.period.timeZone})</Text>
      )}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} md={6}><Card loading={queries.summary.isLoading}><Statistic title="Fuel consumed" value={summary?.metrics.consumedLitres ?? 'N/A'} suffix={summary?.metrics.consumedLitres == null ? '' : 'L'} /></Card></Col>
        <Col xs={24} md={6}><Card loading={queries.summary.isLoading}><Statistic title="Consumption rate" value={value(summary?.metrics.consumptionRate)} suffix={filters.measurementMode === 'DISTANCE' ? 'L/km' : 'L/hour'} /></Card></Col>
        <Col xs={24} md={6}><Card loading={queries.summary.isLoading}><Statistic title="Vehicles" value={summary?.vehicleCount ?? 'N/A'} /></Card></Col>
        <Col xs={24} md={6}><Card loading={queries.summary.isLoading}><Statistic title="Drivers" value={summary?.driverCount ?? 'N/A'} /></Card></Col>
      </Row>

      {summary && <div style={{ marginTop: 12 }}><Quality metrics={summary.metrics} /></div>}
      {summary?.metrics.exclusionReasons && Object.keys(summary.metrics.exclusionReasons).length > 0 && (
        <Alert style={{ marginTop: 12 }} type="warning" showIcon message="Some source facts were excluded"
          description={Object.entries(summary.metrics.exclusionReasons).map(([reason, count]) => `${reason}: ${count}`).join(', ')} />
      )}

      <Card title="Actual vs historical baseline trend" style={{ marginTop: 16 }} loading={queries.trends.isLoading}>
        {(queries.trends.data ?? []).length === 0 ? <Text type="secondary">Insufficient data — N/A</Text> :
          (queries.trends.data ?? []).map((trend) => (
            <Flex key={trend.bucketStart} justify="space-between" gap={12}>
              <Text>{trend.bucketStart}</Text>
              <Text>Actual {value(trend.actualRate)} / baseline {value(trend.baselineRate)}</Text>
              <Tag color={qualityColor[trend.quality]}>{trend.quality.replaceAll('_', ' ')}</Tag>
            </Flex>
          ))}
      </Card>

      <Card title="Vehicle comparison" style={{ marginTop: 16 }}>
        <Table<VehiclePerformance> rowKey="vehicleId" loading={queries.vehicles.isLoading}
          dataSource={queries.vehicles.data?.content ?? []} pagination={{ pageSize: 20 }} columns={[
            { title: 'Vehicle', dataIndex: 'vehicleLabel' },
            { title: 'Fuel', dataIndex: 'fuelType' },
            { title: 'Consumption', render: (_, row) => value(row.metrics.consumptionRate) },
            { title: 'Historical variance', render: (_, row) => value(row.metrics.adverseVariancePercent, '%') },
            { title: 'Quality / review', render: (_, row) => <Quality metrics={row.metrics} /> },
          ]} />
      </Card>

      <Card title="Driver comparison — operational review" style={{ marginTop: 16 }}>
        <Table<DriverPerformance> rowKey="driverId" loading={queries.drivers.isLoading}
          dataSource={queries.drivers.data?.content ?? []} pagination={{ pageSize: 20 }} columns={[
            { title: 'Driver', dataIndex: 'driverLabel' },
            { title: 'Fuel', dataIndex: 'fuelType' },
            { title: 'Consumption', render: (_, row) => value(row.metrics.consumptionRate) },
            { title: 'Historical variance', render: (_, row) => value(row.metrics.adverseVariancePercent, '%') },
            { title: 'Quality / review', render: (_, row) => <Quality metrics={row.metrics} /> },
          ]} />
      </Card>
    </div>
  );
}
