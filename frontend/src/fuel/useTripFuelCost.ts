import { useQuery } from '@tanstack/react-query';
import { api } from '../api/client';

export type TripFuelCostCalculationStatus = 'COMPLETE' | 'PARTIAL' | 'UNAVAILABLE';
export type PricingSource = 'EXPLICIT_ISSUE_PRICE' | 'PRICE_CATALOGUE' | 'UNPRICED';
export type TripDistanceStatus = 'CALCULATED' | 'PENDING_START' | 'PENDING_END' | 'MISMATCH' | 'UNAVAILABLE';

export interface TripFuelCostLineResponse {
  fuelIssueId: string;
  voucherNumber: string;
  issuedAt: string;
  quantityLiters: number;
  unitPrice: number | null;
  lineCost: number | null;
  pricingSource: PricingSource;
  currencyCode: string;
  stationId: string | null;
  fuelType: string;
}

export interface TripFuelCostResponse {
  tripId: string;
  vehicleId: string | null;
  totalFuelQuantityLiters: number;
  currencyCode: string;
  totalFuelCost: number;
  tripDistanceKm: number | null;
  costPerKm: number | null;
  litersPer100Km: number | null;
  fuelIssueCount: number;
  unpricedIssueCount: number;
  distanceStatus: TripDistanceStatus;
  calculationStatus: TripFuelCostCalculationStatus;
  lines: TripFuelCostLineResponse[];
  calculatedAt: string;
}

export function useTripFuelCost(tripId?: string) {
  return useQuery({
    queryKey: ['trip-fuel-cost', tripId],
    queryFn: async () => {
      if (!tripId) throw new Error('Trip ID is required');
      const res = await api.get<TripFuelCostResponse>(`/trips/${tripId}/fuel-cost`);
      return res.data;
    },
    enabled: Boolean(tripId),
  });
}