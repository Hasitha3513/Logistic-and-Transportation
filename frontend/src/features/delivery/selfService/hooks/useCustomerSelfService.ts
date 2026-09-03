import { useQuery } from '@tanstack/react-query';
import { selfServiceApi } from '../api/selfServiceApi';

export const useCustomerSelfService = (token?: string, revision = 0) => useQuery({
  queryKey: ['delivery-self-service', token, revision],
  queryFn: () => selfServiceApi.track(token!),
  enabled: Boolean(token),
  retry: false,
  staleTime: 0,
  gcTime: 0,
});
