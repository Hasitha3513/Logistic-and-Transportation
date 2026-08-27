import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  ExperimentOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { Alert, Badge, Button, Card, Descriptions, Flex, Space, Table, Tag, Typography, message } from 'antd';
import { useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../../auth/AuthContext';
import { useLoadPlan, useSaveLoadPlan } from '../hooks/useLoadPlans';
import type { LoadPlanItemPlacement, LoadPlanValidationResponse, LoadValidationResultResponse } from '../types/loadPlan';

export default function LoadPlanDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const { hasPermission } = useAuth();
  const query = useLoadPlan(id);
  const actions = useSaveLoadPlan(id);

  const [layoutResult, setLayoutResult] = useState<LoadPlanValidationResponse | null>(null);
  const [wvResult, setWvResult] = useState<LoadValidationResultResponse | null>(null);
  const [readyError, setReadyError] = useState<string | null>(null);

  if (!hasPermission('LOAD_PLAN_VIEW')) {
    return <Navigate to="/workspace" replace />;
  }

  const handleValidateLayout = async () => {
    try {
      const res = await actions.validateLayout.mutateAsync();
      setLayoutResult(res);
      if (res.valid) {
        void message.success('Plan layout is structurally valid');
      } else {
        void message.warning('Layout has structural violations');
      }
    } catch {
      void message.error('Failed to validate layout');
    }
  };

  const handleValidateWeightVolume = async () => {
    try {
      const res = await actions.validateWeightVolume.mutateAsync();
      setWvResult(res);
      if (res.overallOutcome === 'PASS') {
        void message.success('Weight and volume validation PASSED');
      } else if (res.overallOutcome === 'INCOMPLETE') {
        void message.info('Weight and volume validation INCOMPLETE (missing authoritative measurements)');
      } else {
        void message.error('Weight and volume validation FAILED');
      }
    } catch {
      void message.error('Failed to validate weight and volume');
    }
  };

  const handleMarkReady = async () => {
    if (!query.data) return;
    setReadyError(null);
    try {
      await actions.markReady.mutateAsync({ version: query.data.version });
      void message.success('Load plan marked structurally ready');
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { code?: string; message?: string } }; message?: string };
      const serverMsg = errorObj.response?.data?.message || errorObj.message || 'Failed to mark load plan structurally ready';
      setReadyError(serverMsg);
      void message.error('Failed to mark load plan structurally ready: ' + serverMsg);
    }
  };

  const plan = query.data;

  return (
    <Flex vertical gap={18}>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <Space>
          <Link to="/freight/load-plans">
            <Button icon={<ArrowLeftOutlined />}>Back to list</Button>
          </Link>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {plan ? plan.loadPlanNumber : 'Load Plan Details'}
          </Typography.Title>
        </Space>
        <Space>
          {hasPermission('LOAD_PLAN_MANAGE') && (
            <>
              <Button
                type="primary"
                icon={<CheckCircleOutlined />}
                onClick={handleMarkReady}
                loading={actions.markReady.isPending}
                disabled={plan?.readinessStatus === 'STRUCTURALLY_READY'}
              >
                Mark Structurally Ready
              </Button>
              <Button
                icon={<ExperimentOutlined />}
                onClick={handleValidateLayout}
                loading={actions.validateLayout.isPending}
              >
                Validate Layout
              </Button>
              <Button
                icon={<ExperimentOutlined />}
                onClick={handleValidateWeightVolume}
                loading={actions.validateWeightVolume.isPending}
              >
                Validate Weight & Volume
              </Button>
            </>
          )}
          <Button icon={<ReloadOutlined />} onClick={() => void query.refetch()} loading={query.isFetching}>
            Refresh
          </Button>
        </Space>
      </Flex>

      {query.isError && <Alert type="error" showIcon message="Load plan could not be loaded" />}

      {readyError && (
        <Alert
          type="error"
          showIcon
          closable
          onClose={() => setReadyError(null)}
          message="Structural Readiness Failed"
          description={readyError}
        />
      )}

      {plan && (
        <>
          <Card title="Plan Summary">
            <Descriptions bordered column={{ xs: 1, sm: 2, md: 3 }}>
              <Descriptions.Item label="Plan Number">{plan.loadPlanNumber}</Descriptions.Item>
              <Descriptions.Item label="Readiness Status">
                {plan.readinessStatus === 'STRUCTURALLY_READY' ? (
                  <Tag color="green">STRUCTURALLY READY</Tag>
                ) : (
                  <Tag color="default">DRAFT</Tag>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="Cargo Manifest ID">
                <Link to={`/freight/manifests/${plan.cargoManifestId}`}>{plan.cargoManifestId}</Link>
              </Descriptions.Item>
              <Descriptions.Item label="Vehicle ID">{plan.vehicleId}</Descriptions.Item>
              <Descriptions.Item label="Version">{plan.version}</Descriptions.Item>
              <Descriptions.Item label="Created">{new Date(plan.createdAt).toLocaleString()}</Descriptions.Item>
              <Descriptions.Item label="Created By">{plan.createdBy}</Descriptions.Item>
              {plan.readyAt && (
                <Descriptions.Item label="Ready At">{new Date(plan.readyAt).toLocaleString()}</Descriptions.Item>
              )}
              {plan.readyBy && (
                <Descriptions.Item label="Ready By">{plan.readyBy}</Descriptions.Item>
              )}
              <Descriptions.Item label="Notes" span={3}>
                {plan.notes || '—'}
              </Descriptions.Item>
            </Descriptions>
          </Card>

          {layoutResult && (
            <Card
              title={
                <Space>
                  {layoutResult.valid ? (
                    <CheckCircleOutlined style={{ color: '#52c41a' }} />
                  ) : (
                    <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                  )}
                  <span>Layout Structural Validation</span>
                  <Tag color={layoutResult.valid ? 'green' : 'red'}>
                    {layoutResult.valid ? 'VALID' : 'VIOLATIONS DETECTED'}
                  </Tag>
                </Space>
              }
            >
              {layoutResult.valid ? (
                <Typography.Text type="success">
                  All cargo items are placed, sequence is valid, and compatibility constraints are satisfied.
                </Typography.Text>
              ) : (
                <Flex vertical gap={8}>
                  <Typography.Text type="danger">
                    The following structural issues must be resolved:
                  </Typography.Text>
                  {layoutResult.violations.map((v, i) => (
                    <Alert key={i} type="error" message={`${v.code}: ${v.message}`} />
                  ))}
                </Flex>
              )}
            </Card>
          )}

          {wvResult && (
            <Card
              title={
                <Space>
                  <ExclamationCircleOutlined style={{ color: '#faad14' }} />
                  <span>Weight, Volume & Capacity Validation (US-27)</span>
                  <Tag
                    color={
                      wvResult.overallOutcome === 'PASS'
                        ? 'green'
                        : wvResult.overallOutcome === 'FAIL'
                        ? 'red'
                        : 'orange'
                    }
                  >
                    {wvResult.overallOutcome}
                  </Tag>
                </Space>
              }
            >
              <Descriptions bordered column={{ xs: 1, sm: 2, md: 4 }} style={{ marginBottom: 16 }}>
                <Descriptions.Item label="Overall Outcome">
                  <Badge
                    status={
                      wvResult.overallOutcome === 'PASS'
                        ? 'success'
                        : wvResult.overallOutcome === 'FAIL'
                        ? 'error'
                        : 'warning'
                    }
                    text={wvResult.overallOutcome}
                  />
                </Descriptions.Item>
                <Descriptions.Item label="Payload Check">
                  <Tag color={wvResult.payloadResult === 'PASS' ? 'green' : 'orange'}>
                    {wvResult.payloadResult || 'INCOMPLETE'}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Volume Check">
                  <Tag color={wvResult.volumeResult === 'PASS' ? 'green' : 'orange'}>
                    {wvResult.volumeResult || 'INCOMPLETE'}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Axle Check">
                  <Tag color={wvResult.axleResult === 'PASS' ? 'green' : 'orange'}>
                    {wvResult.axleResult || 'INCOMPLETE'}
                  </Tag>
                </Descriptions.Item>
              </Descriptions>

              {wvResult.missingData && wvResult.missingData.length > 0 && (
                <Flex vertical gap={6} style={{ marginBottom: 16 }}>
                  <Typography.Text strong>Missing Authoritative Evidence:</Typography.Text>
                  <Space wrap>
                    {wvResult.missingData.map((item, idx) => (
                      <Tag key={idx} color="gold">
                        {item}
                      </Tag>
                    ))}
                  </Space>
                </Flex>
              )}

              {wvResult.violations && wvResult.violations.length > 0 && (
                <Flex vertical gap={6}>
                  <Typography.Text strong>Diagnostics & Actionable Guidance:</Typography.Text>
                  {wvResult.violations.map((v, i) => (
                    <Alert key={i} type="warning" showIcon message={`${v.code}: ${v.message}`} />
                  ))}
                </Flex>
              )}
            </Card>
          )}

          <Card title="Item Placements">
            <Table<LoadPlanItemPlacement>
              rowKey="id"
              dataSource={plan.placements}
              pagination={false}
              columns={[
                {
                  title: 'Order',
                  dataIndex: 'placementOrder',
                  width: 80,
                },
                {
                  title: 'Manifest Item ID',
                  dataIndex: 'manifestItemId',
                  render: (v) => <Typography.Text code>{String(v).substring(0, 8)}</Typography.Text>,
                },
                {
                  title: 'Zone',
                  dataIndex: 'zoneReference',
                  render: (v) => (v ? <Tag color="blue">{v}</Tag> : '—'),
                },
                {
                  title: 'Stack Group',
                  dataIndex: 'stackGroup',
                  render: (v) => (v ? <Tag color="purple">{v}</Tag> : '—'),
                },
                {
                  title: 'Container / Pallet',
                  dataIndex: 'containerReference',
                  render: (v) => v || '—',
                },
                {
                  title: 'Loading Seq',
                  dataIndex: 'loadingSequence',
                  width: 110,
                },
                {
                  title: 'Special Handling',
                  dataIndex: 'specialHandlingNotes',
                  render: (v) => (v ? <Tag color="warning">{v}</Tag> : '—'),
                },
              ]}
            />
          </Card>
        </>
      )}
    </Flex>
  );
}
