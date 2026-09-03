import { useQuery } from '@tanstack/react-query';
import { deliveryNotificationApi } from '../api/deliveryNotificationApi';

export const useDeliveryNotifications = (deliveryId: string) => useQuery({
  queryKey: ['delivery-notifications', deliveryId],
  queryFn: () => deliveryNotificationApi.timeline(deliveryId),
});
