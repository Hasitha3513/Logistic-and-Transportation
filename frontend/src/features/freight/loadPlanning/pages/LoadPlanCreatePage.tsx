import { ArrowLeftOutlined, SaveOutlined } from '@ant-design/icons';
import { Button, Card, Divider, Flex, Form, Input, InputNumber, Select, Space, Table, Typography, message } from 'antd';
import { useMemo, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../../auth/AuthContext';
import { useAvailableVehicles, useFinalizedManifests, useManifestForPlanning, useSaveLoadPlan } from '../hooks/useLoadPlans';
import type { LoadPlanItemPlacementPayload } from '../types/loadPlan';

export default function LoadPlanCreatePage() {
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [selectedManifestId, setSelectedManifestId] = useState<string>();
  const [editedPlacements, setEditedPlacements] = useState<LoadPlanItemPlacementPayload[]>();

  const manifestsQuery = useFinalizedManifests();
  const vehiclesQuery = useAvailableVehicles();
  const manifestDetailQuery = useManifestForPlanning(selectedManifestId);
  const saveActions = useSaveLoadPlan();

  const defaultPlacements = useMemo<LoadPlanItemPlacementPayload[]>(
    () =>
      (manifestDetailQuery.data?.items ?? []).map((item, index) => ({
        manifestItemId: item.id,
        placementOrder: index,
        zoneReference: 'FRONT',
        stackGroup: 'STACK-1',
        containerReference: 'PALLET-' + (index + 1),
        loadingSequence: index + 1,
        specialHandlingNotes: item.hazardous ? 'Hazardous: ' + (item.hazardousClassification || 'Class 1') : null,
      })),
    [manifestDetailQuery.data?.items],
  );
  const placements = editedPlacements ?? defaultPlacements;

  if (!hasPermission('LOAD_PLAN_MANAGE')) {
    return <Navigate to="/freight/load-plans" replace />;
  }

  const handlePlacementChange = (index: number, field: keyof LoadPlanItemPlacementPayload, value: unknown) => {
    setEditedPlacements((current) => {
      const next = [...(current ?? defaultPlacements)];
      next[index] = { ...next[index], [field]: value };
      return next;
    });
  };

  const handleFinish = async (values: { cargoManifestId: string; vehicleId: string; notes?: string }) => {
    try {
      const saved = await saveActions.create.mutateAsync({
        cargoManifestId: values.cargoManifestId,
        vehicleId: values.vehicleId,
        notes: values.notes,
        placements,
      });
      void message.success('Load plan created successfully');
      navigate(`/freight/load-plans/${saved.id}`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to create load plan';
      void message.error(msg);
    }
  };

  return (
    <Flex vertical gap={18}>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <Space>
          <Link to="/freight/load-plans">
            <Button icon={<ArrowLeftOutlined />}>Back to list</Button>
          </Link>
          <Typography.Title level={4} style={{ margin: 0 }}>
            Create Load Plan
          </Typography.Title>
        </Space>
      </Flex>

      <Card>
        <Form form={form} layout="vertical" onFinish={handleFinish}>
          <Form.Item
            name="cargoManifestId"
            label="Finalized Cargo Manifest"
            rules={[{ required: true, message: 'Please select a finalized manifest' }]}
          >
            <Select
              aria-label="Cargo manifest selector"
              placeholder="Select a finalized manifest"
              loading={manifestsQuery.isLoading}
              options={manifestsQuery.data?.map((m) => ({
                label: `${m.manifestNumber} (${m.freightOrderNumber})`,
                value: m.id,
              }))}
              onChange={(val) => {
                setSelectedManifestId(val);
                setEditedPlacements(undefined);
              }}
            />
          </Form.Item>

          <Form.Item
            name="vehicleId"
            label="Assigned Vehicle"
            rules={[{ required: true, message: 'Please select an active vehicle' }]}
          >
            <Select
              aria-label="Vehicle selector"
              placeholder="Select an active vehicle"
              loading={vehiclesQuery.isLoading}
              options={vehiclesQuery.data?.map((v) => ({
                label: `${v.registrationNumber} - ${v.manufacturer || ''} ${v.model || ''} (${v.capacityKg || 0} kg)`,
                value: v.id,
              }))}
            />
          </Form.Item>

          <Form.Item name="notes" label="Planning Notes">
            <Input.TextArea rows={3} placeholder="Optional physical handling notes or instructions" />
          </Form.Item>

          <Divider orientation="left">Cargo Item Placements</Divider>

          {selectedManifestId && manifestDetailQuery.isLoading && <Typography.Text>Loading manifest cargo items...</Typography.Text>}

          {placements.length > 0 && (
            <Table
              rowKey="manifestItemId"
              dataSource={placements}
              pagination={false}
              columns={[
                {
                  title: 'Manifest Item',
                  render: (_, r) => {
                    const item = manifestDetailQuery.data?.items.find((i) => i.id === r.manifestItemId);
                    return item ? `${item.description} (Qty: ${item.quantity})` : r.manifestItemId;
                  },
                },
                {
                  title: 'Placement Order',
                  render: (_, r, index) => (
                    <InputNumber
                      min={0}
                      value={r.placementOrder}
                      onChange={(val) => handlePlacementChange(index, 'placementOrder', val ?? 0)}
                    />
                  ),
                },
                {
                  title: 'Zone Reference',
                  render: (_, r, index) => (
                    <Input
                      value={r.zoneReference ?? ''}
                      placeholder="e.g. FRONT, REAR"
                      onChange={(e) => handlePlacementChange(index, 'zoneReference', e.target.value)}
                    />
                  ),
                },
                {
                  title: 'Stack Group',
                  render: (_, r, index) => (
                    <Input
                      value={r.stackGroup ?? ''}
                      placeholder="e.g. STACK-A"
                      onChange={(e) => handlePlacementChange(index, 'stackGroup', e.target.value)}
                    />
                  ),
                },
                {
                  title: 'Container / Pallet',
                  render: (_, r, index) => (
                    <Input
                      value={r.containerReference ?? ''}
                      placeholder="e.g. PALLET-1"
                      onChange={(e) => handlePlacementChange(index, 'containerReference', e.target.value)}
                    />
                  ),
                },
                {
                  title: 'Loading Sequence',
                  render: (_, r, index) => (
                    <InputNumber
                      min={1}
                      value={r.loadingSequence}
                      onChange={(val) => handlePlacementChange(index, 'loadingSequence', val ?? 1)}
                    />
                  ),
                },
                {
                  title: 'Special Handling',
                  render: (_, r, index) => (
                    <Input
                      value={r.specialHandlingNotes ?? ''}
                      placeholder="Fragile, Temp, etc."
                      onChange={(e) => handlePlacementChange(index, 'specialHandlingNotes', e.target.value)}
                    />
                  ),
                },
              ]}
            />
          )}

          <Form.Item style={{ marginTop: 24 }}>
            <Button
              type="primary"
              htmlType="submit"
              icon={<SaveOutlined />}
              loading={saveActions.create.isPending}
            >
              Save Load Plan
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </Flex>
  );
}
