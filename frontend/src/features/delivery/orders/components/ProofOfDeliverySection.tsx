import React, { useEffect, useRef, useState } from 'react';
import {
  ClearOutlined,
  DeleteOutlined,
  EnvironmentOutlined,
  FileImageOutlined,
  ScanOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import {
  Alert,
  App,
  Button,
  Card,
  Checkbox,
  Descriptions,
  Divider,
  Flex,
  Form,
  Input,
  List,
  Modal,
  Space,
  Spin,
  Tag,
  Typography,
  Upload,
} from 'antd';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQueryClient } from '@tanstack/react-query';
import { isAxiosError } from 'axios';
import { useAuth } from '../../../../auth/AuthContext';
import {
  useAddDeliveryEvidence,
  useCreateDeliveryProof,
  useDeleteDeliveryEvidence,
  useDeliveryProof,
  useFinalizeDeliveryProof,
} from '../hooks/useDeliveryOrders';
import type { DeliveryOrder, PodEvidenceType } from '../types/deliveryOrder';
import {
  proofDraftSchema,
  type ProofDraftValues,
  validateEvidenceFile,
  validatePodConsent,
} from '../validation/proofOfDeliverySchema';
import { useOfflineSync } from '../../../offlineSync/OfflineSyncProvider';
import type {
  DeliveryPodOfflineEvidenceItem,
  OfflineOperation,
} from '../../../offlineSync/types';
import { OfflineOperationStatusTag } from '../../../offlineSync/OfflineOperationStatusTag';
import { OfflineOperationActions } from '../../../offlineSync/OfflineOperationActions';

interface Props {
  delivery: DeliveryOrder;
}

interface ErrorBody {
  message?: string;
}

const errorText = (e: unknown) =>
  isAxiosError<ErrorBody>(e)
    ? e.response?.data?.message ?? 'Proof of Delivery operation failed'
    : 'Proof of Delivery operation failed';

export function ProofOfDeliverySection({ delivery }: Props) {
  const { user, hasPermission } = useAuth();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const { onlineHint, backendReachable, enqueueOperation, getOperationsForAggregate, syncNow, registerPostApply } =
    useOfflineSync();
  const isOnline = onlineHint && backendReachable !== false;

  useEffect(() => {
    return registerPostApply('DELIVERY_POD_OFFLINE_SYNC', async (operation) => {
      if (operation.aggregateId === delivery.id) {
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: ['delivery-proof', delivery.id] }),
          queryClient.invalidateQueries({ queryKey: ['deliveries'] }),
          queryClient.invalidateQueries({ queryKey: ['delivery-orders'] }),
        ]);
      }
    });
  }, [queryClient, registerPostApply, delivery.id]);

  const canCapture =
    hasPermission('DELIVERY_POD_CAPTURE') && delivery.status === 'READY_FOR_ASSIGNMENT';
  const canView = hasPermission('DELIVERY_POD_VIEW');

  const proof = useDeliveryProof(delivery.id, canCapture || canView);
  const create = useCreateDeliveryProof();
  const add = useAddDeliveryEvidence();
  const remove = useDeleteDeliveryEvidence();
  const finalize = useFinalizeDeliveryProof();

  const {
    control,
    getValues,
    setValue,
    formState: { errors },
  } = useForm<ProofDraftValues>({
    resolver: zodResolver(proofDraftSchema),
    defaultValues: {},
  });

  // Consent & Offline state
  const [consentGiven, setConsentGiven] = useState(false);
  const [offlineOperations, setOfflineOperations] = useState<OfflineOperation[]>([]);
  const [isSavingOffline, setIsSavingOffline] = useState(false);

  // Offline staged evidence
  const [stagedEvidence, setStagedEvidence] = useState<DeliveryPodOfflineEvidenceItem[]>([]);
  const [isSignatureModalOpen, setIsSignatureModalOpen] = useState(false);
  const [isDrawing, setIsDrawing] = useState(false);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  // Refresh offline queue for this delivery
  useEffect(() => {
    let mounted = true;
    const loadOffline = async () => {
      if (!user?.id) return;
      try {
        const ops = await getOperationsForAggregate('DELIVERY', delivery.id);
        if (mounted) {
          setOfflineOperations(ops);
        }
      } catch (err) {
        console.error('Failed to load offline operations for delivery', err);
      }
    };
    void loadOffline();
    const interval = setInterval(() => void loadOffline(), 3000);
    return () => {
      mounted = false;
      clearInterval(interval);
    };
  }, [user?.id, delivery.id, getOperationsForAggregate]);

  const current = proof.data;

  // Canvas drawing handlers
  const startDrawing = (e: React.MouseEvent<HTMLCanvasElement> | React.TouchEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const rect = canvas.getBoundingClientRect();
    const x = 'touches' in e ? e.touches[0].clientX - rect.left : e.clientX - rect.left;
    const y = 'touches' in e ? e.touches[0].clientY - rect.top : e.clientY - rect.top;
    ctx.beginPath();
    ctx.moveTo(x, y);
    setIsDrawing(true);
  };

  const draw = (e: React.MouseEvent<HTMLCanvasElement> | React.TouchEvent<HTMLCanvasElement>) => {
    if (!isDrawing) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const rect = canvas.getBoundingClientRect();
    const x = 'touches' in e ? e.touches[0].clientX - rect.left : e.clientX - rect.left;
    const y = 'touches' in e ? e.touches[0].clientY - rect.top : e.clientY - rect.top;
    ctx.lineTo(x, y);
    ctx.stroke();
  };

  const stopDrawing = () => {
    setIsDrawing(false);
  };

  const clearCanvas = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  };

  const saveCanvasSignature = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const dataUrl = canvas.toDataURL('image/png');
    const base64Content = dataUrl.split(',')[1];
    if (!base64Content) {
      void message.error('Please provide a signature before saving');
      return;
    }
    // Remove existing signature and add new one
    setStagedEvidence((prev) => [
      ...prev.filter((e) => e.evidenceType !== 'SIGNATURE'),
      {
        evidenceType: 'SIGNATURE',
        binaryContent: base64Content,
        captureSource: 'MANUAL',
        originalFilename: 'drawn_signature.png',
      },
    ]);
    setIsSignatureModalOpen(false);
    void message.success('Signature captured');
  };

  const locate = () =>
    navigator.geolocation?.getCurrentPosition(
      (p) => {
        setValue('latitude', p.coords.latitude);
        setValue('longitude', p.coords.longitude);
        if (p.coords.accuracy > 0) setValue('accuracyMeters', p.coords.accuracy);
        void message.success('Location captured');
      },
      () => void message.warning('Location unavailable; you can continue without it'),
    );

  const fileToBase64 = (file: File): Promise<string> =>
    new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const res = reader.result as string;
        resolve(res.split(',')[1] ?? '');
      };
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });

  const handleAddStagedPhoto = async (file: File) => {
    const issue = validateEvidenceFile('PHOTO', file);
    if (issue) {
      void message.error(issue);
      return;
    }
    const currentPhotoCount = stagedEvidence.filter((e) => e.evidenceType === 'PHOTO').length;
    if (currentPhotoCount >= 3) {
      void message.error('Maximum 3 photos allowed per Proof of Delivery');
      return;
    }
    try {
      const b64 = await fileToBase64(file);
      setStagedEvidence((prev) => [
        ...prev,
        {
          evidenceType: 'PHOTO',
          binaryContent: b64,
          captureSource: 'CAMERA',
          originalFilename: file.name,
        },
      ]);
      void message.success('Photo added');
    } catch {
      void message.error('Failed to read photo file');
    }
  };

  const handleAddBarcode = async (val: string) => {
    const trimmed = val.trim().toUpperCase();
    if (!trimmed) return;
    if (current && current.status === 'DRAFT') {
      await upload('BARCODE', undefined, trimmed);
      return;
    }
    if (trimmed !== delivery.deliveryNumber) {
      void message.error(`Barcode must match delivery number ${delivery.deliveryNumber}`);
      return;
    }
    setStagedEvidence((prev) => [
      ...prev.filter((e) => e.evidenceType !== 'BARCODE'),
      {
        evidenceType: 'BARCODE',
        barcodeValue: trimmed,
        captureSource: 'SCANNER',
      },
    ]);
    void message.success('Barcode added');
  };

  const handleSaveOfflineDraft = async () => {
    if (!user?.id) {
      void message.error('User context unavailable');
      return;
    }
    if (stagedEvidence.length === 0) {
      void message.error('Please add at least one signature, photo, or barcode evidence');
      return;
    }
    const hasSigOrPhoto = stagedEvidence.some(
      (e) => e.evidenceType === 'SIGNATURE' || e.evidenceType === 'PHOTO',
    );
    const consentErr = validatePodConsent(consentGiven, hasSigOrPhoto);
    if (consentErr) {
      void message.error(consentErr);
      return;
    }
    const formValues = getValues();
    if (stagedEvidence.some((e) => e.evidenceType === 'SIGNATURE') && !formValues.signerName) {
      void message.error('Signer name is required when signature is present');
      return;
    }

    setIsSavingOffline(true);
    try {
      await enqueueOperation({
        ownerUserId: user.id,
        operationType: 'DELIVERY_POD_OFFLINE_SYNC',
        aggregateType: 'DELIVERY',
        aggregateId: delivery.id,
        payload: {
          deliveryId: delivery.id,
          deliveryVersion: delivery.version,
          signerName: formValues.signerName?.trim() || undefined,
          signerRelationship: formValues.signerRelationship?.trim() || undefined,
          consentGiven,
          consentVersion: consentGiven ? 'POD-CONSENT-V1' : undefined,
          consentTimestamp: consentGiven ? new Date().toISOString() : undefined,
          deviceCapturedAt: new Date().toISOString(),
          latitude: formValues.latitude,
          longitude: formValues.longitude,
          accuracyMeters: formValues.accuracyMeters,
          finalizeIntent: true,
          evidenceList: stagedEvidence,
        },
      });
      void message.success('Proof of Delivery saved offline and queued for sync');
      setStagedEvidence([]);
      if (isOnline) {
        void syncNow();
      }
    } catch (err) {
      void message.error('Failed to save offline POD');
      console.error(err);
    } finally {
      setIsSavingOffline(false);
    }
  };

  const handleCreateOnlineDraft = async () => {
    const values = getValues();
    try {
      await create.mutateAsync({
        id: delivery.id,
        payload: {
          deliveryVersion: delivery.version,
          deviceCapturedAt: new Date().toISOString(),
          ...values,
        },
      });
      void message.success('POD draft created');
    } catch (e) {
      void message.error(errorText(e));
    }
  };

  const upload = async (type: PodEvidenceType, file?: File, barcodeValue?: string) => {
    if (!current) return;
    if (file && type !== 'BARCODE') {
      const issue = validateEvidenceFile(type, file);
      if (issue) {
        void message.error(issue);
        return;
      }
    }
    try {
      await add.mutateAsync({
        id: delivery.id,
        podVersion: current.version,
        type,
        file,
        barcodeValue,
        captureSource: type === 'BARCODE' ? 'MANUAL' : 'FILE',
      });
      void message.success(`${type.toLowerCase()} evidence added`);
    } catch (e) {
      void message.error(errorText(e));
    }
  };

  const doRemove = async (evidenceId: string) => {
    if (!current) return;
    try {
      await remove.mutateAsync({
        id: delivery.id,
        evidenceId,
        podVersion: current.version,
      });
      void message.success('Evidence removed');
    } catch (e) {
      void message.error(errorText(e));
    }
  };

  const doFinalize = async () => {
    if (!current) return;
    try {
      await finalize.mutateAsync({
        id: delivery.id,
        deliveryVersion: delivery.version,
        podVersion: current.version,
      });
      void message.success('Proof of Delivery finalized');
    } catch (e) {
      void message.error(errorText(e));
    }
  };

  if (!canView && !canCapture) return null;
  if (proof.isLoading) {
    return (
      <Card title="Proof of Delivery">
        <Spin />
      </Card>
    );
  }

  const latestOfflineOp = offlineOperations[offlineOperations.length - 1];

  return (
    <Card
      title={
        <Space>
          <span>Proof of Delivery</span>
          {current && (
            <Tag color={current.status === 'FINALIZED' ? 'green' : 'blue'}>
              {current.status}
            </Tag>
          )}
          {latestOfflineOp && (
            <OfflineOperationStatusTag status={latestOfflineOp.status} />
          )}
          {!isOnline && <Tag color="volcano">Offline Mode</Tag>}
        </Space>
      }
    >
      <Flex vertical gap={16}>
        {latestOfflineOp && (
          <Alert
            type={
              latestOfflineOp.status === 'SYNCED'
                ? 'success'
                : latestOfflineOp.status === 'CONFLICT'
                  ? 'warning'
                  : latestOfflineOp.status === 'FAILED'
                    ? 'error'
                    : 'info'
            }
            showIcon
            message={
              <Space>
                <span>
                  {latestOfflineOp.status === 'SYNCED'
                    ? 'Offline POD synced successfully'
                    : latestOfflineOp.status === 'PENDING'
                      ? 'Offline POD queued in local outbox — will sync automatically when online'
                      : latestOfflineOp.status === 'CONFLICT'
                        ? `Sync conflict: ${latestOfflineOp.lastErrorMessage ?? 'POD conflict'}`
                        : `Sync failed: ${latestOfflineOp.lastErrorMessage ?? 'Operation failed'}`}
                </span>
                <OfflineOperationActions operation={latestOfflineOp} />
              </Space>
            }
          />
        )}

        {current && (
          <Descriptions
            size="small"
            column={{ xs: 1, md: 2 }}
            items={[
              {
                key: 'accepted',
                label: 'Server accepted',
                children: current.acceptedAt
                  ? new Date(current.acceptedAt).toLocaleString()
                  : 'Not finalized',
              },
              {
                key: 'location',
                label: 'Location',
                children:
                  current.latitude == null
                    ? 'Not provided'
                    : `${current.latitude}, ${current.longitude}`,
              },
            ]}
          />
        )}

        {current && (
          <List
            size="small"
            bordered
            dataSource={current.evidence}
            locale={{ emptyText: 'No signature, photo or barcode evidence yet' }}
            renderItem={(item) => (
              <List.Item
                actions={
                  canCapture && current.status === 'DRAFT'
                    ? [
                        <Button
                          key="delete"
                          type="text"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={() => void doRemove(item.id)}
                        />,
                      ]
                    : []
                }
              >
                <Space>
                  <Tag>{item.type}</Tag>
                  <span>
                    {item.barcodeValue ?? item.originalFilename ?? item.checksum}
                  </span>
                </Space>
              </List.Item>
            )}
          />
        )}

        {/* Offline Capture / Staged Evidence Area */}
        {canCapture && (!current || current.status === 'DRAFT') && (
          <div>
            <Divider orientation="left" plain>
              Offline / Staged Capture
            </Divider>
            <Form layout="vertical">
              <Controller
                name="signerName"
                control={control}
                render={({ field }) => (
                  <Form.Item
                    label="Signer name"
                    htmlFor="signerName"
                    validateStatus={errors.signerName ? 'error' : ''}
                    help={errors.signerName?.message}
                  >
                    <Input id="signerName" aria-label="Signer name" {...field} placeholder="Full name of recipient" />
                  </Form.Item>
                )}
              />
              <Controller
                name="signerRelationship"
                control={control}
                render={({ field }) => (
                  <Form.Item label="Signer relationship" htmlFor="signerRelationship">
                    <Input id="signerRelationship" aria-label="Signer relationship" {...field} placeholder="e.g. Recipient, Warehouse Manager" />
                  </Form.Item>
                )}
              />

              <Form.Item>
                <Checkbox
                  id="customerConsent"
                  checked={consentGiven}
                  onChange={(e) => setConsentGiven(e.target.checked)}
                >
                  <strong>Customer Consent (POD-CONSENT-V1):</strong> Recipient confirms delivery acceptance and agrees to electronic signature/photo capture.
                </Checkbox>
              </Form.Item>

              {stagedEvidence.length > 0 && (
                <List
                  size="small"
                  bordered
                  style={{ marginBottom: 16 }}
                  header={<strong>Staged Evidence ({stagedEvidence.length})</strong>}
                  dataSource={stagedEvidence}
                  renderItem={(item, index) => (
                    <List.Item
                      actions={[
                        <Button
                          key="remove"
                          type="text"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={() =>
                            setStagedEvidence((prev) => prev.filter((_, i) => i !== index))
                          }
                        >
                          Remove / Retake
                        </Button>,
                      ]}
                    >
                      <Space>
                        <Tag color="cyan">{item.evidenceType}</Tag>
                        <span>
                          {item.barcodeValue ?? item.originalFilename ?? 'Captured binary'}
                        </span>
                      </Space>
                    </List.Item>
                  )}
                />
              )}

              <Space wrap>
                <Button
                  icon={<FileImageOutlined />}
                  onClick={() => setIsSignatureModalOpen(true)}
                >
                  Draw Signature
                </Button>
                <Upload
                  accept="image/png,image/jpeg"
                  showUploadList={false}
                  beforeUpload={(file) => {
                    void handleAddStagedPhoto(file);
                    return false;
                  }}
                >
                  <Button icon={<FileImageOutlined />}>Capture / Upload Photo</Button>
                </Upload>
                <Input.Search
                  id="deliveryBarcode"
                  aria-label="Delivery barcode"
                  placeholder={delivery.deliveryNumber}
                  enterButton={
                    <>
                      <ScanOutlined /> Add Barcode
                    </>
                  }
                  onSearch={(val) => void handleAddBarcode(val)}
                  style={{ width: 320 }}
                />
                <Button icon={<EnvironmentOutlined />} onClick={locate}>
                  Capture Location
                </Button>
                <Button
                  type="primary"
                  icon={<SyncOutlined />}
                  onClick={() => void handleSaveOfflineDraft()}
                  loading={isSavingOffline}
                >
                  Save & Queue Offline
                </Button>
                {!current && isOnline && (
                  <Button onClick={() => void handleCreateOnlineDraft()} loading={create.isPending}>
                    Start POD
                  </Button>
                )}
              </Space>
            </Form>
          </div>
        )}

        {/* Online Actions if Online & Draft exists */}
        {canCapture && current && current.status === 'DRAFT' && isOnline && (
          <Space wrap style={{ marginTop: 8 }}>
            <Upload
              accept="image/png,image/jpeg"
              showUploadList={false}
              beforeUpload={(file) => {
                void upload('SIGNATURE', file);
                return false;
              }}
            >
              <Button icon={<FileImageOutlined />}>Upload Online Signature</Button>
            </Upload>
            <Upload
              accept="image/png,image/jpeg"
              showUploadList={false}
              beforeUpload={(file) => {
                void upload('PHOTO', file);
                return false;
              }}
            >
              <Button icon={<FileImageOutlined />}>Upload Online Photo</Button>
            </Upload>
            <Button
              type="primary"
              onClick={() => void doFinalize()}
              loading={finalize.isPending}
            >
              Finalize POD Online
            </Button>
          </Space>
        )}

        {(!current || current.status === 'DRAFT') && (
          <Typography.Text type="secondary">
            Customer consent is mandatory for signature and photo capture. Barcode evidence must match the delivery order number ({delivery.deliveryNumber}).
          </Typography.Text>
        )}
      </Flex>

      {/* Signature Canvas Modal */}
      <Modal
        title="Draw Signature"
        open={isSignatureModalOpen}
        onOk={saveCanvasSignature}
        onCancel={() => setIsSignatureModalOpen(false)}
        okText="Accept Signature"
        maskClosable={false}
        destroyOnClose
      >
        <div style={{ textAlign: 'center' }}>
          <canvas
            ref={canvasRef}
            width={400}
            height={200}
            style={{
              border: '2px dashed #d9d9d9',
              borderRadius: 8,
              backgroundColor: '#fafafa',
              touchAction: 'none',
              cursor: 'crosshair',
            }}
            onMouseDown={startDrawing}
            onMouseMove={draw}
            onMouseUp={stopDrawing}
            onMouseLeave={stopDrawing}
            onTouchStart={startDrawing}
            onTouchMove={draw}
            onTouchEnd={stopDrawing}
          />
          <div style={{ marginTop: 8 }}>
            <Button icon={<ClearOutlined />} onClick={clearCanvas}>
              Clear / Retake
            </Button>
          </div>
        </div>
      </Modal>
    </Card>
  );
}
