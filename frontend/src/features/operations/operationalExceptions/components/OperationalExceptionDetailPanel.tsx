import { zodResolver } from '@hookform/resolvers/zod';
import { Button, Descriptions, Divider, Flex, Form, Input, List, Select, Space, Tag, Timeline, Typography } from 'antd';
import { Controller, useForm } from 'react-hook-form';
import { useAuth } from '../../../../auth/AuthContext';
import { useCorrectiveActionCommand, useOperationalExceptionCommand, useOperationalExceptionHistory } from '../hooks/useOperationalExceptions';
import type { OperationalExceptionDetail } from '../types/operationalException';
import { operationalCommandSchema, type OperationalCommandValues } from '../validation/operationalExceptionSchema';

interface Props { detail: OperationalExceptionDetail }

export function OperationalExceptionDetailPanel({ detail }: Props) {
  const { hasPermission } = useAuth();
  const value = detail.exceptionCase;
  const command = useOperationalExceptionCommand(value.id);
  const actionCommand = useCorrectiveActionCommand(value.id);
  const history = useOperationalExceptionHistory(value.id, hasPermission('OPERATIONAL_EXCEPTION_AUDIT_VIEW'));
  const { control, getValues } = useForm<OperationalCommandValues>({
    resolver: zodResolver(operationalCommandSchema),
    defaultValues: { note: '', category: value.category, severity: value.severity, roleCode: value.assignedRoleCode ?? '' },
  });
  const submit = (action: string, payload: object) => command.mutate({ action, payload });
  const note = () => getValues('note').trim();

  return <Flex vertical gap={12}>
    <Descriptions size="small" column={2} bordered items={[
      { key: 'source', label: 'Source', children: `${value.sourceModule} · ${value.sourceType}` },
      { key: 'sourceId', label: 'Source reference', children: value.sourceId },
      { key: 'category', label: 'Category', children: value.category },
      { key: 'severity', label: 'Severity', children: <Tag color={value.severity === 'CRITICAL' ? 'red' : value.severity === 'HIGH' ? 'orange' : 'blue'}>{value.severity}</Tag> },
      { key: 'status', label: 'Status', children: value.status },
      { key: 'sla', label: 'SLA', children: <Tag color={value.slaStatus === 'BREACHED' ? 'red' : value.slaStatus === 'AT_RISK' ? 'orange' : 'green'}>{value.slaStatus}</Tag> },
      { key: 'response', label: 'Response due', children: new Date(value.responseDueAt).toLocaleString() },
      { key: 'resolution', label: 'Resolution due', children: new Date(value.resolutionDueAt).toLocaleString() },
      { key: 'assignment', label: 'Assignment', children: value.assignedRoleCode ?? value.assignedUserId ?? 'Unassigned' },
      { key: 'level', label: 'Escalation', children: value.escalationLevel },
    ]} />

    {(hasPermission('OPERATIONAL_EXCEPTION_MANAGE') || hasPermission('OPERATIONAL_EXCEPTION_ASSIGN') || hasPermission('OPERATIONAL_EXCEPTION_ESCALATE') || hasPermission('OPERATIONAL_EXCEPTION_RCA') || hasPermission('OPERATIONAL_EXCEPTION_CLOSE')) && <>
      <Divider orientation="left">Case actions</Divider>
      <Form layout="vertical">
        <Flex gap={12} wrap>
          <Form.Item label="Category"><Controller control={control} name="category" render={({ field }) => <Select {...field} style={{ width: 170 }} options={['OPERATIONAL','SAFETY','COMPLIANCE','CUSTOMER','FINANCIAL','TECHNICAL','SECURITY'].map(item => ({ value: item }))} />} /></Form.Item>
          <Form.Item label="Severity"><Controller control={control} name="severity" render={({ field }) => <Select {...field} style={{ width: 140 }} options={['LOW','MEDIUM','HIGH','CRITICAL'].map(item => ({ value: item }))} />} /></Form.Item>
          <Form.Item label="Role queue"><Controller control={control} name="roleCode" render={({ field }) => <Input {...field} style={{ width: 230 }} />} /></Form.Item>
        </Flex>
        <Form.Item label="Reason / note"><Controller control={control} name="note" render={({ field }) => <Input.TextArea {...field} maxLength={2000} rows={2} />} /></Form.Item>
      </Form>
      <Space wrap>
        {hasPermission('OPERATIONAL_EXCEPTION_MANAGE') && value.status === 'OPEN' && <Button onClick={() => submit('acknowledge', { expectedVersion: value.version })}>Acknowledge</Button>}
        {hasPermission('OPERATIONAL_EXCEPTION_MANAGE') && ['OPEN','ACKNOWLEDGED'].includes(value.status) && <Button onClick={() => submit('start', { expectedVersion: value.version })}>Start</Button>}
        {hasPermission('OPERATIONAL_EXCEPTION_MANAGE') && <Button onClick={() => submit('classify', { expectedVersion: value.version, category: getValues('category'), severity: getValues('severity'), reason: note() })}>Apply classification</Button>}
        {hasPermission('OPERATIONAL_EXCEPTION_ASSIGN') && <Button onClick={() => submit('assign', { expectedVersion: value.version, assignmentType: 'ROLE_QUEUE', roleCode: getValues('roleCode'), reason: note() })}>Assign role queue</Button>}
        {hasPermission('OPERATIONAL_EXCEPTION_ESCALATE') && !['RESOLVED','CLOSED'].includes(value.status) && <Button danger onClick={() => submit('escalate', { expectedVersion: value.version, reason: note() })}>Escalate</Button>}
        {hasPermission('OPERATIONAL_EXCEPTION_MANAGE') && value.status === 'IN_PROGRESS' && <Button onClick={() => submit('corrective-actions', { expectedVersion: value.version, type: 'CORRECTIVE', description: note(), ownerType: 'ROLE_QUEUE', ownerRoleCode: value.assignedRoleCode })}>Add corrective action</Button>}
        {hasPermission('OPERATIONAL_EXCEPTION_RCA') && !detail.rca && <Button onClick={() => submit('rca', { expectedVersion: value.version, causeCategory: 'PROCESS', rootCauseCode: 'OPERATOR_RCA', summary: note() })}>Record RCA</Button>}
        {hasPermission('OPERATIONAL_EXCEPTION_RCA') && detail.rca && !detail.rca.approvedAt && <Button onClick={() => submit('rca/approve', { expectedCaseVersion: value.version, expectedRcaVersion: detail.rca!.version })}>Approve RCA</Button>}
        {hasPermission('OPERATIONAL_EXCEPTION_MANAGE') && value.status === 'IN_PROGRESS' && <Button type="primary" onClick={() => submit('resolve', { expectedVersion: value.version, resolutionNote: note() })}>Resolve</Button>}
        {hasPermission('OPERATIONAL_EXCEPTION_CLOSE') && value.status === 'RESOLVED' && <Button type="primary" onClick={() => submit('close', { expectedVersion: value.version })}>Close</Button>}
        {hasPermission('OPERATIONAL_EXCEPTION_CLOSE') && value.status === 'RESOLVED' && <Button onClick={() => submit('reject-resolution', { expectedVersion: value.version, reason: note() })}>Reject resolution</Button>}
        {hasPermission('OPERATIONAL_EXCEPTION_CLOSE') && value.status === 'CLOSED' && <Button onClick={() => submit('reopen', { expectedVersion: value.version, reason: note() })}>Reopen</Button>}
      </Space>
    </>}

    <Divider orientation="left">Corrective actions</Divider>
    <List dataSource={detail.correctiveActions} locale={{ emptyText: 'No corrective actions' }} renderItem={(item) => <List.Item actions={hasPermission('OPERATIONAL_EXCEPTION_MANAGE') ? [
      item.status === 'OPEN' ? <Button key="start" size="small" onClick={() => actionCommand.mutate({ actionId: item.id, action: 'start', version: item.version })}>Start</Button> : null,
      ['OPEN','IN_PROGRESS'].includes(item.status) ? <Button key="complete" size="small" onClick={() => actionCommand.mutate({ actionId: item.id, action: 'complete', version: item.version })}>Complete</Button> : null,
    ].filter(Boolean) : []}><List.Item.Meta title={`${item.type} · ${item.status}`} description={item.description} /></List.Item>} />

    {hasPermission('OPERATIONAL_EXCEPTION_RCA') && <><Divider orientation="left">Root cause analysis</Divider>
      {detail.rca ? <Typography.Paragraph>{detail.rca.rootCauseCode}: {detail.rca.summary} {detail.rca.approvedAt ? '(Approved)' : '(Awaiting approval)'}</Typography.Paragraph> : <Typography.Text type="secondary">No RCA recorded</Typography.Text>}</>}

    {hasPermission('OPERATIONAL_EXCEPTION_AUDIT_VIEW') && <><Divider orientation="left">Timeline</Divider>
      <Timeline items={(history.data?.content ?? []).map(item => ({ children: <><strong>{item.action}</strong> · {new Date(item.occurredAt).toLocaleString()}<br />{item.reason ?? item.afterValue}</> }))} /></>}
  </Flex>;
}
