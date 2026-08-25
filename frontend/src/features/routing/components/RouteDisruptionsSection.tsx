import { AlertOutlined, CheckCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { Alert, App as AntApp, Button, Card, Descriptions, Empty, Flex, Popconfirm, Space, Spin, Tag, Typography } from 'antd';
import { useState } from 'react';
import { useAuth } from '../../../auth/AuthContext';
import { useResolveRouteDisruption, useRouteDisruptions } from '../hooks/useRouteHistoryAndDisruptions';
import { RouteDisruptionModal } from './RouteDisruptionModal';
import type { DisruptionSeverity } from '../types/route';

interface RouteDisruptionsSectionProps {
  routeId: string;
}

const severityColors: Record<DisruptionSeverity, string> = {
  CRITICAL: 'error',
  HIGH: 'warning',
  MEDIUM: 'gold',
  LOW: 'processing',
};

export function RouteDisruptionsSection({ routeId }: RouteDisruptionsSectionProps) {
  const { hasPermission } = useAuth();
  const { message } = AntApp.useApp();
  const [modalOpen, setModalOpen] = useState(false);
  const { data: disruptions, isLoading, isError } = useRouteDisruptions(routeId);
  const resolveMutation = useResolveRouteDisruption(routeId);

  const canManageDisruptions = hasPermission('ROUTE_DISRUPTION_MANAGE') || hasPermission('ROUTE_UPDATE');

  const handleResolve = async (disruptionId: string) => {
    try {
      await resolveMutation.mutateAsync(disruptionId);
      void message.success('Disruption marked as resolved');
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { message?: string } }; message?: string };
      void message.error(apiErr?.response?.data?.message ?? apiErr?.message ?? 'Failed to resolve disruption');
    }
  };

  if (isLoading) {
    return (
      <Card size="small" title={<><AlertOutlined /> Route Disruptions</>}>
        <Flex justify="center" style={{ padding: 16 }}>
          <Spin size="small" aria-label="Loading disruptions" />
        </Flex>
      </Card>
    );
  }

  if (isError) {
    return (
      <Card size="small" title={<><AlertOutlined /> Route Disruptions</>}>
        <Alert type="error" showIcon message="Disruptions could not be loaded" />
      </Card>
    );
  }

  return (
    <>
      <Card
        size="small"
        title={<><AlertOutlined /> Route Disruptions ({disruptions?.length ?? 0})</>}
        extra={canManageDisruptions && (
          <Button
            size="small"
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setModalOpen(true)}
            data-testid="report-disruption-btn"
          >
            Report Disruption
          </Button>
        )}
      >
        {!disruptions || disruptions.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No disruptions reported on this route" />
        ) : (
          <Flex vertical gap={12}>
            {disruptions.map((d) => (
              <Card
                key={d.id}
                size="small"
                type="inner"
                data-testid={`disruption-card-${d.id}`}
                title={
                  <Flex justify="space-between" align="center">
                    <Space>
                      <Tag color={severityColors[d.severity]}>{d.severity}</Tag>
                      <Tag color="geekblue">{d.disruptionType.replace('_', ' ')}</Tag>
                      <Tag color={d.status === 'ACTIVE' ? 'volcano' : 'green'}>{d.status}</Tag>
                    </Space>
                    {d.status === 'ACTIVE' && canManageDisruptions && (
                      <Popconfirm
                        title="Resolve disruption"
                        description="Mark this disruption as resolved and restore normal traffic status?"
                        onConfirm={() => handleResolve(d.id)}
                        okText="Yes, Resolve"
                        cancelText="Cancel"
                      >
                        <Button
                          size="small"
                          type="dashed"
                          icon={<CheckCircleOutlined />}
                          loading={resolveMutation.isPending}
                          data-testid={`resolve-disruption-btn-${d.id}`}
                        >
                          Resolve
                        </Button>
                      </Popconfirm>
                    )}
                  </Flex>
                }
              >
                <Descriptions size="small" column={{ xs: 1, sm: 2 }} bordered>
                  <Descriptions.Item label="Description" span={2}>
                    <Typography.Text strong>{d.description}</Typography.Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="Effective From">
                    {new Date(d.effectiveFrom).toLocaleString()}
                  </Descriptions.Item>
                  <Descriptions.Item label="Effective Until">
                    {d.effectiveUntil ? new Date(d.effectiveUntil).toLocaleString() : 'Indefinite / Ongoing'}
                  </Descriptions.Item>
                  {d.detourRouteId && (
                    <Descriptions.Item label="Detour Route ID" span={2}>
                      <Tag color="cyan">{d.detourRouteId}</Tag>
                    </Descriptions.Item>
                  )}
                  <Descriptions.Item label="Reported By">
                    {d.createdBy} ({new Date(d.createdAt).toLocaleString()})
                  </Descriptions.Item>
                  {d.status === 'RESOLVED' && (
                    <Descriptions.Item label="Resolved By">
                      {d.resolvedBy} ({d.resolvedAt ? new Date(d.resolvedAt).toLocaleString() : 'N/A'})
                    </Descriptions.Item>
                  )}
                </Descriptions>
              </Card>
            ))}
          </Flex>
        )}
      </Card>

      {modalOpen && (
        <RouteDisruptionModal
          routeId={routeId}
          open={modalOpen}
          onClose={() => setModalOpen(false)}
        />
      )}
    </>
  );
}
