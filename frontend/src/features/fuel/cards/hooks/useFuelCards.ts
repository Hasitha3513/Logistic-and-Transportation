import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fuelCardsApi } from '../api/fuelCardsApi';
import type { CreateFuelCard, FuelCard, FuelCardRestriction, FuelCardTransaction } from '../types/fuelCards';

export const fuelCardKeys = { all: ['fuel-cards'] as const, transactions: ['fuel-card-transactions'] as const, imports: ['fuel-card-imports'] as const };
export function useFuelCardDetail(id?: string) {
  return {
    bindings: useQuery({ queryKey: [...fuelCardKeys.all, id, 'bindings'], queryFn: () => fuelCardsApi.bindings(id!), enabled: !!id }),
    history: useQuery({ queryKey: [...fuelCardKeys.all, id, 'history'], queryFn: () => fuelCardsApi.history(id!), enabled: !!id }),
  };
}
export function useFuelCards() {
  const queryClient = useQueryClient();
  return {
    cards: useQuery({ queryKey: fuelCardKeys.all, queryFn: fuelCardsApi.cards }),
    transactions: useQuery({ queryKey: fuelCardKeys.transactions, queryFn: fuelCardsApi.transactions }),
    imports: useQuery({ queryKey: fuelCardKeys.imports, queryFn: fuelCardsApi.imports }),
    create: useMutation({ mutationFn: (value: CreateFuelCard) => fuelCardsApi.create(value),
      onSuccess: () => queryClient.invalidateQueries({ queryKey: fuelCardKeys.all }) }),
    transition: useMutation({ mutationFn: ({ card, action, reason }: { card: FuelCard; action: string; reason: string }) => fuelCardsApi.transition(card, action, reason),
      onSuccess: () => queryClient.invalidateQueries({ queryKey: fuelCardKeys.all }) }),
    bind: useMutation({ mutationFn: ({ card, bindingType, bindingId, reason }: { card: FuelCard; bindingType: string; bindingId: string; reason: string }) => fuelCardsApi.bind(card, bindingType, bindingId, reason),
      onSuccess: () => queryClient.invalidateQueries({ queryKey: fuelCardKeys.all }) }),
    restrict: useMutation({ mutationFn: ({ card, restriction }: { card: FuelCard; restriction: Omit<FuelCardRestriction, 'version'> & { reason: string } }) => fuelCardsApi.restrict(card, restriction),
      onSuccess: () => queryClient.invalidateQueries({ queryKey: fuelCardKeys.all }) }),
    reconcile: useMutation({ mutationFn: ({ transaction, action, purchaseId, reason }: { transaction: FuelCardTransaction; action: string; purchaseId?: string; reason: string }) => fuelCardsApi.reconcile(transaction, action, purchaseId, reason),
      onSuccess: () => queryClient.invalidateQueries({ queryKey: fuelCardKeys.transactions }) }),
    upload: useMutation({ mutationFn: ({ providerId, file }: { providerId: string; file: File }) => fuelCardsApi.upload(providerId, file),
      onSuccess: async () => { await Promise.all([queryClient.invalidateQueries({ queryKey: fuelCardKeys.imports }), queryClient.invalidateQueries({ queryKey: fuelCardKeys.transactions })]); } }),
  };
}
