import { api } from '../../../../api/client';
import type { CreateFuelCard, FuelCard, FuelCardBinding, FuelCardHistory, FuelCardImportBatch, FuelCardRestriction, FuelCardTransaction } from '../types/fuelCards';

export const fuelCardsApi = {
  cards: async () => (await api.get<FuelCard[]>('/v1/fuel/cards')).data,
  card: async (id: string) => (await api.get<FuelCard>(`/v1/fuel/cards/${id}`)).data,
  create: async (value: CreateFuelCard) => (await api.post<FuelCard>('/v1/fuel/cards', value)).data,
  transition: async (card: FuelCard, action: string, reason: string) =>
    (await api.post<FuelCard>(`/v1/fuel/cards/${card.id}/${action}`, { version: card.version, reason })).data,
  bindings: async (id: string) => (await api.get<FuelCardBinding[]>(`/v1/fuel/cards/${id}/bindings`)).data,
  bind: async (card: FuelCard, bindingType: string, bindingId: string, reason: string) =>
    (await api.post<FuelCardBinding>(`/v1/fuel/cards/${card.id}/bindings`, { bindingType, bindingId, version: card.version, reason })).data,
  history: async (id: string) => (await api.get<FuelCardHistory[]>(`/v1/fuel/cards/${id}/history`)).data,
  restrict: async (card: FuelCard, restriction: Omit<FuelCardRestriction, 'version'> & { reason: string }) =>
    (await api.put<FuelCardRestriction>(`/v1/fuel/cards/${card.id}/restrictions`, { ...restriction, version: card.version })).data,
  transactions: async () => (await api.get<FuelCardTransaction[]>('/v1/fuel/card-transactions')).data,
  imports: async () => (await api.get<FuelCardImportBatch[]>('/v1/fuel/card-imports')).data,
  reconcile: async (transaction: FuelCardTransaction, action: string, purchaseId: string | undefined, reason: string) =>
    (await api.post<FuelCardTransaction>(`/v1/fuel/card-transactions/${transaction.id}/${action}`,
      { purchaseId, version: transaction.version ?? 0, reason })).data,
  upload: async (providerId: string, file: File) => {
    const data = new FormData(); data.append('file', file);
    return (await api.post<FuelCardImportBatch>('/v1/fuel/card-imports', data, { params: { providerId } })).data;
  },
};
