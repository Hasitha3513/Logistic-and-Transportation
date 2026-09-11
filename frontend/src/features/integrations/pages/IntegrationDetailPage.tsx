import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, App as AntApp, Button, Card, Descriptions, Flex, Form, Input, Space, Table, Tag, Typography } from 'antd';
import { isAxiosError } from 'axios';
import { useEffect } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Navigate, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import { IntegrationStatusTags } from '../components/IntegrationStatusTags';
import { useIntegration, useIntegrationAction, useIntegrationExchanges, useSaveIntegration } from '../hooks/useIntegrations';
import type { IntegrationPayload } from '../types/integration';
import { integrationSchema, type IntegrationFormValues } from '../validation/integrationSchema';

const mapping = { mappingKey: 'US73_PLATFORM_PROBE', sourceContract: 'US73_PLATFORM_PROBE', sourceVersion: 1,
  targetSchema: 'US73_FILE_PROBE', targetVersion: 1, rules: [
    { sourceField: 'probeId', targetField: 'probe_id', format: 'UUID' as const, omitIfNull: false, required: true },
    { sourceField: 'probeType', targetField: 'probe_type', format: 'ENUM' as const, omitIfNull: false, required: true },
    { sourceField: 'sequence', targetField: 'sequence', format: 'DECIMAL' as const, omitIfNull: false, required: true },
  ] };
interface ErrorBody { message?: string; fieldErrors?: Array<{ field: string; message: string }> }

export default function IntegrationDetailPage() {
  const { id } = useParams(); const creating = !id; const navigate = useNavigate(); const { hasPermission } = useAuth();
  const { message } = AntApp.useApp(); const detail = useIntegration(id); const editable = creating || detail.data?.lifecycle !== 'ACTIVE';
  const save = useSaveIntegration(id); const test = useIntegrationAction(id ?? '', 'test');
  const enable = useIntegrationAction(id ?? '', 'enable'); const disable = useIntegrationAction(id ?? '', 'disable');
  const history = useIntegrationExchanges(id, hasPermission('INTEGRATION_AUDIT_VIEW'));
  const form = useForm<IntegrationFormValues>({ resolver: zodResolver(integrationSchema), defaultValues: {
    name: '', endpointAlias: 'CONTROLLED_SANDBOX', credentialReference: '',
  } });
  useEffect(() => { if (detail.data) form.reset({ name: detail.data.name, endpointAlias: 'CONTROLLED_SANDBOX', credentialReference: '' }); }, [detail.data, form]);
  if (!hasPermission('INTEGRATION_VIEW') || (creating && !hasPermission('INTEGRATION_MANAGE'))) return <Navigate to="/integrations" replace />;
  const submit = form.handleSubmit(async (values) => {
    const payload: IntegrationPayload = { ...values, credentialReference: values.credentialReference || undefined, mapping,
      ...(creating ? { type: 'FILE_EXCHANGE', protocol: 'FILE_JSON_V1', direction: 'OUTBOUND', dataClassification: 'INTERNAL_OPERATIONAL_NON_SENSITIVE' } : { version: detail.data!.version }) };
    try { const saved = await save.mutateAsync(payload); void message.success(creating ? 'Integration created' : 'Integration updated'); navigate(`/integrations/${saved.id}`); }
    catch (error) { if (isAxiosError<ErrorBody>(error)) { error.response?.data?.fieldErrors?.forEach((item) => form.setError(item.field as keyof IntegrationFormValues, { message: item.message })); form.setError('root', { message: error.response?.data?.message ?? 'Integration could not be saved' }); } }
  });
  const act = async (kind: 'test' | 'enable' | 'disable') => { const mutation = kind === 'test' ? test : kind === 'enable' ? enable : disable; try { await mutation.mutateAsync(detail.data!.version); void message.success(kind === 'test' ? 'Connection test completed' : `Integration ${kind}d`); } catch { void message.error(`Integration ${kind} failed`); } };
  const item = detail.data;
  return <Flex vertical gap={18}>{form.formState.errors.root?.message && <Alert type="error" message={form.formState.errors.root.message} />}
    {!creating && item && <Card><Flex justify="space-between" wrap gap={12}><IntegrationStatusTags lifecycle={item.lifecycle} health={item.health} /><Space>
      {hasPermission('INTEGRATION_TEST') && item.lifecycle !== 'ACTIVE' && <Button onClick={() => void act('test')} loading={test.isPending}>Test connection</Button>}
      {hasPermission('INTEGRATION_ACTIVATE') && item.lifecycle !== 'ACTIVE' && <Button type="primary" onClick={() => void act('enable')} loading={enable.isPending}>Enable</Button>}
      {hasPermission('INTEGRATION_ACTIVATE') && item.lifecycle === 'ACTIVE' && <Button danger onClick={() => void act('disable')} loading={disable.isPending}>Disable</Button>}
    </Space></Flex></Card>}
    <Card title={creating ? 'Configuration' : 'Configuration and mapping'} loading={!creating && detail.isLoading}><Form layout="vertical" onFinish={() => void submit()}>
      <Flex gap={16} wrap><Form.Item label="Name" required validateStatus={form.formState.errors.name ? 'error' : undefined} help={form.formState.errors.name?.message}><Controller name="name" control={form.control} render={({ field }) => <Input {...field} aria-label="Name" disabled={!editable} style={{ width: 320 }} />} /></Form.Item>
      <Form.Item label="Endpoint alias"><Controller name="endpointAlias" control={form.control} render={({ field }) => <Input {...field} aria-label="Endpoint alias" disabled style={{ width: 240 }} />} /></Form.Item>
      <Form.Item label="Credential reference" extra={item?.credentialConfigured ? 'A credential is configured. Leave blank to retain it.' : 'Optional environment-backed opaque reference.'}><Controller name="credentialReference" control={form.control} render={({ field }) => <Input.Password {...field} aria-label="Credential reference" disabled={!editable} autoComplete="new-password" style={{ width: 320 }} />} /></Form.Item></Flex>
      <Descriptions bordered size="small" column={2} items={[{ key: 'type', label: 'Capability', children: 'FILE_EXCHANGE / FILE_JSON_V1 / OUTBOUND' }, { key: 'classification', label: 'Classification', children: 'INTERNAL_OPERATIONAL_NON_SENSITIVE' }, { key: 'source', label: 'Source contract', children: 'US73_PLATFORM_PROBE v1' }, { key: 'target', label: 'Target schema', children: 'US73_FILE_PROBE v1' }, { key: 'rules', label: 'Mapping', span: 2, children: 'probeId → probe_id · probeType → probe_type · sequence → sequence' }]} />
      {editable && hasPermission('INTEGRATION_MANAGE') && <Space style={{ marginTop: 18 }}><Button type="primary" htmlType="submit" loading={save.isPending}>Save configuration</Button><Button onClick={() => navigate('/integrations')}>Cancel</Button></Space>}
    </Form></Card>
    {!creating && item && hasPermission('INTEGRATION_AUDIT_VIEW') && <Card title="Exchange history"><Table rowKey="id" loading={history.isLoading} dataSource={history.data?.content ?? []} pagination={false} expandable={{ expandedRowRender: (row) => <Table rowKey="attemptNumber" size="small" pagination={false} dataSource={row.attempts} columns={[{ title: 'Attempt', dataIndex: 'attemptNumber' }, { title: 'Outcome', dataIndex: 'outcome' }, { title: 'Latency (ms)', dataIndex: 'latencyMillis' }, { title: 'Safe error', dataIndex: 'errorCode', render: (value?: string) => value ?? '—' }]} /> }} columns={[{ title: 'Status', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag> }, { title: 'Attempts', dataIndex: 'attemptCount' }, { title: 'Mapping hash', dataIndex: 'mappingDefinitionHash', render: (value: string) => value.slice(0, 12) }, { title: 'Target filename', dataIndex: 'targetFilename', render: (value?: string) => value ?? '—' }, { title: 'Safe error', dataIndex: 'lastErrorCode', render: (value?: string) => value ?? '—' }, { title: 'Created', dataIndex: 'createdAt', render: (value: string) => new Date(value).toLocaleString() }]} /></Card>}
    {!creating && item && <Typography.Text type="secondary">Credential: {item.credentialConfigured ? item.credentialReferenceLabel ?? 'Configured' : 'Not configured'} · Mapping v{item.mapping.mappingVersion} · {item.mapping.definitionHash.slice(0, 12)}</Typography.Text>}
  </Flex>;
}
