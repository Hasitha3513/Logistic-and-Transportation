import axios from 'axios';

export const ACCESS_TOKEN_KEY = 'transport.accessToken';
export const REFRESH_TOKEN_KEY = 'transport.refreshToken';
export const AUTH_SESSION_EXPIRED_EVENT = 'transport:session-expired';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api';

export const api = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
});

const refreshClient = axios.create({ baseURL, headers: { 'Content-Type': 'application/json' } });
let refreshInFlight: Promise<string> | undefined;

api.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type'];
  }
  return config;
});

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
}

function clearSession() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  window.dispatchEvent(new Event(AUTH_SESSION_EXPIRED_EVENT));
}

api.interceptors.response.use(undefined, async (error) => {
  const request = error.config as (typeof error.config & { _retry?: boolean }) | undefined;
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
  const isAuthRequest = request?.url?.includes('/auth/login') || request?.url?.includes('/auth/refresh');

  if (error.response?.status !== 401 || !request || request._retry || isAuthRequest || !refreshToken) {
    return Promise.reject(error);
  }

  request._retry = true;
  refreshInFlight ??= refreshClient
    .post<AuthResponse>('/auth/refresh', { refreshToken })
    .then(({ data }) => {
      localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
      return data.accessToken;
    })
    .catch((refreshError) => {
      clearSession();
      throw refreshError;
    })
    .finally(() => {
      refreshInFlight = undefined;
    });

  const accessToken = await refreshInFlight;
  request.headers.Authorization = `Bearer ${accessToken}`;
  return api(request);
});
