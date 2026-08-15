import { createContext, useContext, useEffect, type PropsWithChildren } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, ACCESS_TOKEN_KEY, AUTH_SESSION_EXPIRED_EVENT, REFRESH_TOKEN_KEY } from '../api/client';
import type { CurrentUser } from './types';

interface AuthContextValue {
  user?: CurrentUser;
  isLoading: boolean;
  isError: boolean;
  hasPermission: (permission: string) => boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  isLoggingOut: boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: PropsWithChildren) {
  const queryClient = useQueryClient();
  const userQuery = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: async () => (await api.get<CurrentUser>('/auth/me')).data,
    retry: false,
  });
  const loginMutation = useMutation({
    mutationFn: async ({ username, password }: { username: string; password: string }) => {
      const { data } = await api.post<{ accessToken: string; refreshToken: string }>('/auth/login', { username, password });
      localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
      await queryClient.fetchQuery({
        queryKey: ['auth', 'me'],
        queryFn: async () => (await api.get<CurrentUser>('/auth/me')).data,
      });
    },
  });
  const logoutMutation = useMutation({
    mutationFn: async () => {
      const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
      if (refreshToken) await api.post('/auth/logout', { refreshToken });
    },
    onSettled: () => {
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      queryClient.clear();
    },
  });

  const logout = async () => {
    await logoutMutation.mutateAsync();
    window.location.assign('/login');
  };

  useEffect(() => {
    const sessionExpired = () => {
      queryClient.clear();
      if (window.location.pathname !== '/login') window.location.assign('/login');
    };
    window.addEventListener(AUTH_SESSION_EXPIRED_EVENT, sessionExpired);
    return () => window.removeEventListener(AUTH_SESSION_EXPIRED_EVENT, sessionExpired);
  }, [queryClient]);

  return (
    <AuthContext.Provider
      value={{
        user: userQuery.data,
        isLoading: userQuery.isLoading,
        isError: userQuery.isError,
        hasPermission: (permission) => Boolean(userQuery.data?.permissions.includes(permission)),
        login: async (username, password) => loginMutation.mutateAsync({ username, password }),
        logout,
        isLoggingOut: logoutMutation.isPending,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
