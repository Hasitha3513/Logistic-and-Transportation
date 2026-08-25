import { CheckCircleOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { Alert, App as AntApp, Button, Card, Col, Descriptions, Flex, List, Modal, Row, Spin, Tag, Typography } from 'antd';
import { useEffect } from 'react';
import { useApplyOptimization, useOptimizeRoute } from '../hooks/useRouteHistoryAndDisruptions';

interface RouteOptimizerModalProps {
  open: boolean;
  routeId: string;
  onClose: () => void;
  onApplied?: () => void;
}

export function RouteOptimizerModal({ open, routeId, onClose, onApplied }: RouteOptimizerModalProps) {
  const { message } = AntApp.useApp();
  const optimizeMutation = useOptimizeRoute(routeId);
  const applyMutation = useApplyOptimization(routeId);

  useEffect(() => {
    if (open && routeId) {
      optimizeMutation.mutate();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, routeId]);

  const handleApply = async () => {
    if (!optimizeMutation.data?.optimizedStopLocationIds) return;
    try {
      await applyMutation.mutateAsync(optimizeMutation.data.optimizedStopLocationIds);
      void message.success('Optimized route sequence applied successfully');
      onApplied?.();
      onClose();
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { message?: string } }; message?: string };
      void message.error(apiErr?.response?.data?.message ?? apiErr?.message ?? 'Failed to apply optimization');
    }
  };

  const data = optimizeMutation.data;
  const hasSavings = (data?.distanceSavedKm ?? 0) > 0 || (data?.durationSavedMinutes ?? 0) > 0;

  return (
    <Modal
      open={open}
      title={
        <SpaceTitle>
          <ThunderboltOutlined style={{ color: '#fa8c16' }} />
          <span>Route Stop Optimizer</span>
        </SpaceTitle>
      }
      onCancel={onClose}
      footer={
        <Flex justify="flex-end" gap={8}>
          <Button onClick={onClose}>Close</Button>
          <Button
            type="primary"
            disabled={!data || !hasSavings}
            loading={applyMutation.isPending}
            onClick={() => void handleApply()}
          >
            Apply Optimization
          </Button>
        </Flex>
      }
      width={600}
      destroyOnHidden
    >
      {optimizeMutation.isPending && (
        <Flex justify="center" align="center" style={{ padding: '40px 0' }} vertical gap={12}>
          <Spin size="large" />
          <Typography.Text type="secondary">Calculating optimal waypoint sequence...</Typography.Text>
        </Flex>
      )}

      {optimizeMutation.isError && (
        <Alert
          type="error"
          showIcon
          message="Optimization Failed"
          description={
            (optimizeMutation.error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message ??
            'Unable to compute route optimization preview.'
          }
          style={{ margin: '16px 0' }}
        />
      )}

      {data && (
        <Flex vertical gap={16} style={{ marginTop: 12 }}>
          {hasSavings ? (
            <Alert
              type="success"
              showIcon
              message={
                <Flex justify="space-between" align="center">
                  <span>
                    Optimization found! Potential savings: <strong>{data.distanceSavedKm} km</strong> ({data.percentageDistanceImprovement}%)
                  </span>
                  <Tag color="success">+{data.percentageDistanceImprovement}% FASTER</Tag>
                </Flex>
              }
              description={`Estimated duration will be reduced by ${data.durationSavedMinutes} minutes.`}
            />
          ) : (
            <Alert
              type="info"
              showIcon
              icon={<CheckCircleOutlined />}
              message="Already Optimal"
              description="The current stop sequence is already optimal. No further distance reduction was found."
            />
          )}

          <Card size="small" title="Optimization Comparison">
            <Descriptions size="small" column={2} bordered>
              <Descriptions.Item label="Original Distance">{data.originalEstimatedDistanceKm} km</Descriptions.Item>
              <Descriptions.Item label="Optimized Distance">
                <Typography.Text strong type={hasSavings ? 'success' : undefined}>
                  {data.optimizedEstimatedDistanceKm} km
                </Typography.Text>
              </Descriptions.Item>
              <Descriptions.Item label="Original Duration">{data.originalEstimatedDurationMinutes} min</Descriptions.Item>
              <Descriptions.Item label="Optimized Duration">
                <Typography.Text strong type={hasSavings ? 'success' : undefined}>
                  {data.optimizedEstimatedDurationMinutes} min
                </Typography.Text>
              </Descriptions.Item>
              <Descriptions.Item label="Distance Saved">
                <Tag color={data.distanceSavedKm > 0 ? 'green' : 'default'}>{data.distanceSavedKm} km</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Duration Saved">
                <Tag color={data.durationSavedMinutes > 0 ? 'green' : 'default'}>{data.durationSavedMinutes} min</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Intermediate Stops" span={2}>
                {data.optimizedStopLocationIds.length} stop(s) sequenced
              </Descriptions.Item>
            </Descriptions>
          </Card>
          <Row gutter={12}>
            <Col xs={24} sm={12}>
              <Card size="small" title="Before">
                <List
                  size="small"
                  dataSource={data.originalStopLocationIds}
                  renderItem={(locationId, index) => <List.Item>{index + 1}. {locationId}</List.Item>}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12}>
              <Card size="small" title="After">
                <List
                  size="small"
                  dataSource={data.optimizedStopLocationIds}
                  renderItem={(locationId, index) => <List.Item>{index + 1}. {locationId}</List.Item>}
                />
              </Card>
            </Col>
          </Row>
        </Flex>
      )}
    </Modal>
  );
}

function SpaceTitle({ children }: { children: React.ReactNode }) {
  return <Flex align="center" gap={8}>{children}</Flex>;
}
