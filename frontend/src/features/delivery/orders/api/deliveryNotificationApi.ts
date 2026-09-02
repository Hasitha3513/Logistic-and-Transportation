import { api } from '../../../../api/client';
import type { DeliveryNotificationTimelineItem } from '../types/deliveryNotification';

export const deliveryNotificationApi = {
  timeline: async (deliveryId: string) => (await api.get<DeliveryNotificationTimelineItem[]>(
    '/v1/notification-deliveries',
    { params: { aggregateType: 'DELIVERY_ORDER', aggregateId: deliveryId, limit: 200 } },
  )).data,
};
