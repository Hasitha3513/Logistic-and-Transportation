import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { cargoExceptionApi } from '../api/cargoExceptionApi';
import type {
  CargoExceptionFilter,
  CreateCargoExceptionPayload,
  EscalateExceptionPayload,
  HoldExceptionPayload,
  ReleaseExceptionPayload,
  RejectExceptionPayload,
  ResolveExceptionPayload,
} from '../types';

export const EXCEPTION_KEYS = {
  all: ['cargoExceptions'] as const,
  lists: () => [...EXCEPTION_KEYS.all, 'list'] as const,
  list: (filter: CargoExceptionFilter) => [...EXCEPTION_KEYS.lists(), filter] as const,
  details: () => [...EXCEPTION_KEYS.all, 'detail'] as const,
  detail: (id: string) => [...EXCEPTION_KEYS.details(), id] as const,
};

/** Fetch a paginated / filtered list of cargo exceptions */
export function useCargoExceptions(filter: CargoExceptionFilter = {}) {
  return useQuery({
    queryKey: EXCEPTION_KEYS.list(filter),
    queryFn: () => cargoExceptionApi.list(filter),
    staleTime: 30_000,
  });
}

/** Fetch a single cargo exception by ID */
export function useCargoException(id: string | undefined) {
  return useQuery({
    queryKey: EXCEPTION_KEYS.detail(id!),
    queryFn: () => cargoExceptionApi.getById(id!),
    enabled: Boolean(id),
    staleTime: 30_000,
  });
}

/** Record a new cargo exception */
export function useRecordException() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateCargoExceptionPayload) => cargoExceptionApi.record(payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: EXCEPTION_KEYS.lists() }),
  });
}

/** Hold an exception */
export function useHoldException(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: HoldExceptionPayload) => cargoExceptionApi.hold(id, payload),
    onSuccess: (data) => {
      qc.setQueryData(EXCEPTION_KEYS.detail(id), data);
      qc.invalidateQueries({ queryKey: EXCEPTION_KEYS.lists() });
    },
  });
}

/** Escalate an exception */
export function useEscalateException(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: EscalateExceptionPayload) => cargoExceptionApi.escalate(id, payload),
    onSuccess: (data) => {
      qc.setQueryData(EXCEPTION_KEYS.detail(id), data);
      qc.invalidateQueries({ queryKey: EXCEPTION_KEYS.lists() });
    },
  });
}

/** Release an exception from HELD back to OPEN */
export function useReleaseException(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: ReleaseExceptionPayload) => cargoExceptionApi.release(id, payload),
    onSuccess: (data) => {
      qc.setQueryData(EXCEPTION_KEYS.detail(id), data);
      qc.invalidateQueries({ queryKey: EXCEPTION_KEYS.lists() });
    },
  });
}

/** Reject an exception */
export function useRejectException(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: RejectExceptionPayload) => cargoExceptionApi.reject(id, payload),
    onSuccess: (data) => {
      qc.setQueryData(EXCEPTION_KEYS.detail(id), data);
      qc.invalidateQueries({ queryKey: EXCEPTION_KEYS.lists() });
    },
  });
}

/** Resolve an exception */
export function useResolveException(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: ResolveExceptionPayload) => cargoExceptionApi.resolve(id, payload),
    onSuccess: (data) => {
      qc.setQueryData(EXCEPTION_KEYS.detail(id), data);
      qc.invalidateQueries({ queryKey: EXCEPTION_KEYS.lists() });
    },
  });
}
