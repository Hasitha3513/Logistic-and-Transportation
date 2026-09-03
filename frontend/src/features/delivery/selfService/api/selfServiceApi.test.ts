import { afterEach, describe, expect, it, vi } from 'vitest';
import { publicApi } from '../../../../api/client';
import { selfServiceApi } from './selfServiceApi';

describe('selfServiceApi', () => {
  afterEach(() => vi.restoreAllMocks());

  it('uses the shared public Axios transport with only in-memory DeliveryAccess authorization', async () => {
    const request = vi.spyOn(publicApi, 'request').mockResolvedValue({ data: { deliveryNumber: 'DEL-1' } });
    const localRead = vi.spyOn(Storage.prototype, 'getItem');
    const localWrite = vi.spyOn(Storage.prototype, 'setItem');

    await selfServiceApi.track('opaque-memory-token');

    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/public/v1/delivery-self-service',
      method: 'GET',
      headers: { Authorization: 'DeliveryAccess opaque-memory-token' },
    }));
    expect(localRead).not.toHaveBeenCalled();
    expect(localWrite).not.toHaveBeenCalled();
  });
});
