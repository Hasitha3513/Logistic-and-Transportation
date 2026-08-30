import { DeleteOutlined, EnvironmentOutlined, FileImageOutlined, ScanOutlined } from '@ant-design/icons';
import { Alert, App, Button, Card, Descriptions, Flex, Form, Input, List, Space, Spin, Tag, Typography, Upload } from 'antd';
import { Controller, useForm } from 'react-hook-form'; import { zodResolver } from '@hookform/resolvers/zod'; import { isAxiosError } from 'axios';
import { useAuth } from '../../../../auth/AuthContext';
import { useAddDeliveryEvidence, useCreateDeliveryProof, useDeleteDeliveryEvidence, useDeliveryProof, useFinalizeDeliveryProof } from '../hooks/useDeliveryOrders';
import type { DeliveryOrder, PodEvidenceType } from '../types/deliveryOrder'; import { proofDraftSchema, type ProofDraftValues, validateEvidenceFile } from '../validation/proofOfDeliverySchema';

interface Props { delivery: DeliveryOrder }
interface ErrorBody { message?: string }
const errorText = (e: unknown) => isAxiosError<ErrorBody>(e) ? e.response?.data?.message ?? 'Proof of Delivery operation failed' : 'Proof of Delivery operation failed';
export function ProofOfDeliverySection({ delivery }: Props) {
  const { hasPermission } = useAuth(); const { message } = App.useApp();
  const canCapture = hasPermission('DELIVERY_POD_CAPTURE') && delivery.status === 'READY_FOR_ASSIGNMENT'; const canView = hasPermission('DELIVERY_POD_VIEW');
  const proof = useDeliveryProof(delivery.id, canCapture || canView); const create = useCreateDeliveryProof(); const add = useAddDeliveryEvidence(); const remove = useDeleteDeliveryEvidence(); const finalize = useFinalizeDeliveryProof();
  const { control, handleSubmit, setValue, formState: { errors } } = useForm<ProofDraftValues>({ resolver: zodResolver(proofDraftSchema), defaultValues: {} });
  const current = proof.data;
  const createDraft = handleSubmit(async values => { try { await create.mutateAsync({ id: delivery.id, payload: { deliveryVersion: delivery.version, deviceCapturedAt: new Date().toISOString(), ...values } }); void message.success('POD draft created'); } catch (e) { void message.error(errorText(e)); } });
  const locate = () => navigator.geolocation?.getCurrentPosition(p => { setValue('latitude', p.coords.latitude); setValue('longitude', p.coords.longitude); if (p.coords.accuracy > 0) setValue('accuracyMeters', p.coords.accuracy); void message.success('Location captured'); }, () => void message.warning('Location unavailable; you can continue without it'));
  const upload = async (type: PodEvidenceType, file?: File, barcodeValue?: string) => { if (!current) return; if (file && type !== 'BARCODE') { const issue = validateEvidenceFile(type, file); if (issue) { void message.error(issue); return; } } try { await add.mutateAsync({ id: delivery.id, podVersion: current.version, type, file, barcodeValue, captureSource: type === 'BARCODE' ? 'MANUAL' : 'FILE' }); void message.success(`${type.toLowerCase()} evidence added`); } catch (e) { void message.error(errorText(e)); } };
  const doRemove = async (evidenceId: string) => { if (!current) return; try { await remove.mutateAsync({ id: delivery.id, evidenceId, podVersion: current.version }); void message.success('Evidence removed'); } catch (e) { void message.error(errorText(e)); } };
  const doFinalize = async () => { if (!current) return; try { await finalize.mutateAsync({ id: delivery.id, deliveryVersion: delivery.version, podVersion: current.version }); void message.success('Proof of Delivery finalized'); } catch (e) { void message.error(errorText(e)); } };
  if (!canView && !canCapture) return null;
  if (proof.isLoading) return <Card title="Proof of Delivery"><Spin /></Card>;
  if (!current) return <Card title="Proof of Delivery">{canCapture ? <Form layout="vertical" onFinish={() => void createDraft()}>
    <Controller name="signerName" control={control} render={({ field }) => <Form.Item label="Signer name" validateStatus={errors.signerName ? 'error' : ''} help={errors.signerName?.message}><Input {...field} /></Form.Item>} />
    <Controller name="signerRelationship" control={control} render={({ field }) => <Form.Item label="Signer relationship"><Input {...field} /></Form.Item>} />
    <Space><Button icon={<EnvironmentOutlined />} onClick={locate}>Capture location</Button><Button type="primary" htmlType="submit" loading={create.isPending}>Start POD</Button></Space>
  </Form> : <Alert type="info" showIcon message="No Proof of Delivery has been captured" />}</Card>;
  return <Card title={<Space>Proof of Delivery <Tag color={current.status === 'FINALIZED' ? 'green' : 'blue'}>{current.status}</Tag></Space>}>
    <Flex vertical gap={16}><Descriptions size="small" column={{ xs: 1, md: 2 }} items={[{ key: 'accepted', label: 'Server accepted', children: current.acceptedAt ? new Date(current.acceptedAt).toLocaleString() : 'Not finalized' }, { key: 'location', label: 'Location', children: current.latitude == null ? 'Not provided' : `${current.latitude}, ${current.longitude}` }]} />
    <List size="small" bordered dataSource={current.evidence} locale={{ emptyText: 'No signature, photo or barcode evidence yet' }} renderItem={item => <List.Item actions={canCapture && current.status === 'DRAFT' ? [<Button key="delete" type="text" danger icon={<DeleteOutlined />} onClick={() => void doRemove(item.id)} />] : []}><Space><Tag>{item.type}</Tag><span>{item.barcodeValue ?? item.originalFilename ?? item.checksum}</span></Space></List.Item>} />
    {canCapture && current.status === 'DRAFT' && <Space wrap>
      <Upload accept="image/png,image/jpeg" showUploadList={false} beforeUpload={file => { void upload('SIGNATURE', file); return false; }}><Button icon={<FileImageOutlined />}>Add signature image</Button></Upload>
      <Upload accept="image/png,image/jpeg" showUploadList={false} beforeUpload={file => { void upload('PHOTO', file); return false; }}><Button icon={<FileImageOutlined />}>Add photo</Button></Upload>
      <Input.Search aria-label="Delivery barcode" placeholder={delivery.deliveryNumber} enterButton={<><ScanOutlined /> Add barcode</>} onSearch={value => void upload('BARCODE', undefined, value)} style={{ width: 360 }} />
      <Button type="primary" onClick={() => void doFinalize()} loading={finalize.isPending}>Finalize POD</Button>
    </Space>}
    {current.status === 'DRAFT' && <Typography.Text type="secondary">At least one signature, photo, or matching barcode is required. Location is optional.</Typography.Text>}
    </Flex></Card>;
}
