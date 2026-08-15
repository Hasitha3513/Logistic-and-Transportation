import { HttpResponse, http } from 'msw';
import { api, ACCESS_TOKEN_KEY, AUTH_SESSION_EXPIRED_EVENT, REFRESH_TOKEN_KEY } from './client';
import { server } from '../test/server';

describe('API authentication recovery', () => {
  it('shares one refresh across concurrent 401 responses and retries each request once', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'expired-access');
    localStorage.setItem(REFRESH_TOKEN_KEY, 'valid-refresh');
    let refreshCalls = 0;

    server.use(
      http.get('*/refresh-probe', ({ request }) => request.headers.get('Authorization') === 'Bearer renewed-access'
        ? HttpResponse.json({ ok: true })
        : HttpResponse.json({ code: 'INVALID_TOKEN' }, { status: 401 })),
      http.post('*/auth/refresh', async () => {
        refreshCalls += 1;
        return HttpResponse.json({ accessToken: 'renewed-access', refreshToken: 'rotated-refresh' });
      }),
    );

    const responses = await Promise.all([api.get('/refresh-probe'), api.get('/refresh-probe')]);

    expect(responses.every(({ data }) => data.ok)).toBe(true);
    expect(refreshCalls).toBe(1);
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBe('renewed-access');
    expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBe('rotated-refresh');
  });

  it('clears the session and emits one expiry event when refresh fails', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'expired-access');
    localStorage.setItem(REFRESH_TOKEN_KEY, 'expired-refresh');
    const expired = vi.fn();
    window.addEventListener(AUTH_SESSION_EXPIRED_EVENT, expired);
    server.use(
      http.get('*/refresh-failure', () => HttpResponse.json({}, { status: 401 })),
      http.post('*/auth/refresh', () => HttpResponse.json({}, { status: 401 })),
    );

    await expect(api.get('/refresh-failure')).rejects.toBeDefined();

    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
    expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
    expect(expired).toHaveBeenCalledOnce();
    window.removeEventListener(AUTH_SESSION_EXPIRED_EVENT, expired);
  });
});
