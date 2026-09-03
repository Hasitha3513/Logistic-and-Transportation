import { useQuery } from '@tanstack/react-query';
import { freightReportApi } from '../api/freightReportApi';
import type { FreightReportFilter } from '../types';

export const freightReportKeys = {
  all: ['freight', 'reports'] as const,
  summary: (filter: FreightReportFilter) => [...freightReportKeys.all, 'summary', filter] as const,
  shipments: (filter: FreightReportFilter, page: number, size: number) =>
    [...freightReportKeys.all, 'shipments', filter, page, size] as const,
};

export function useFreightReportSummary(filter: FreightReportFilter) {
  return useQuery({ queryKey: freightReportKeys.summary(filter), queryFn: () => freightReportApi.summary(filter) });
}

export function useFreightReportShipments(filter: FreightReportFilter, page: number, size: number) {
  return useQuery({
    queryKey: freightReportKeys.shipments(filter, page, size),
    queryFn: () => freightReportApi.shipments(filter, page, size),
  });
}
