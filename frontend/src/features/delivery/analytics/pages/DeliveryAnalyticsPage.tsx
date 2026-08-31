import React, { useState } from 'react';
import { Button, Flex, Tabs, Typography } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import {
  DELIVERY_ANALYTICS_QUERY_KEYS,
  useDeliveryAnalyticsFailures,
  useDeliveryAnalyticsRegions,
  useDeliveryAnalyticsSummary,
} from '../hooks/useDeliveryAnalytics';
import { DeliveryAnalyticsKpiCards } from '../components/DeliveryAnalyticsKpiCards';
import { DeliveryAnalyticsFilterBar } from '../components/DeliveryAnalyticsFilterBar';
import { DeliveryFailuresTable } from '../components/DeliveryFailuresTable';
import { DeliveryRegionsTable } from '../components/DeliveryRegionsTable';
import { DeliveryTrendsSection } from '../components/DeliveryTrendsSection';
import { DeliveryAnalyticsFilters } from '../types/deliveryAnalytics';

const { Title, Paragraph } = Typography;

export const DeliveryAnalyticsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<DeliveryAnalyticsFilters>({
    from: dayjs().subtract(30, 'days').format('YYYY-MM-DD'),
    to: dayjs().format('YYYY-MM-DD'),
  });

  const { data: summary, isLoading: summaryLoading } = useDeliveryAnalyticsSummary(filters);
  const { data: failures, isLoading: failuresLoading } = useDeliveryAnalyticsFailures(filters);
  const { data: regions, isLoading: regionsLoading } = useDeliveryAnalyticsRegions(filters);

  const handleRefresh = () => {
    queryClient.invalidateQueries({ queryKey: DELIVERY_ANALYTICS_QUERY_KEYS.all });
  };

  return (
    <div style={{ padding: 24 }}>
      <Flex justify="space-between" align="center" style={{ marginBottom: 16 }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>
            Delivery Performance Analytics
          </Title>
          <Paragraph type="secondary" style={{ margin: 0 }}>
            Tenant-scoped KPIs, punctuality benchmarks, failure analysis, and completion trends.
          </Paragraph>
        </div>
        <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
          Refresh
        </Button>
      </Flex>

      <DeliveryAnalyticsFilterBar filters={filters} onFilterChange={setFilters} />

      <DeliveryAnalyticsKpiCards summary={summary} loading={summaryLoading} />

      <Tabs
        defaultActiveKey="trends"
        style={{ marginTop: 24 }}
        items={[
          {
            key: 'trends',
            label: 'Trends & Volumes',
            children: <DeliveryTrendsSection filters={filters} />,
          },
          {
            key: 'regions',
            label: 'Regional Performance',
            children: <DeliveryRegionsTable data={regions} loading={regionsLoading} />,
          },
          {
            key: 'failures',
            label: 'Failure Analysis',
            children: <DeliveryFailuresTable data={failures} loading={failuresLoading} />,
          },
        ]}
      />
    </div>
  );
};

export default DeliveryAnalyticsPage;
