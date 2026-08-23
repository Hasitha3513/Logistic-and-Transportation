export interface OfflineSyncConnectivity {
  isOnline(): boolean;
  subscribe(listener: (online: boolean) => void): () => void;
}

export const browserOfflineSyncConnectivity: OfflineSyncConnectivity = {
  isOnline: () => navigator.onLine,
  subscribe: (listener) => {
    const online = () => listener(true);
    const offline = () => listener(false);
    window.addEventListener('online', online);
    window.addEventListener('offline', offline);
    return () => {
      window.removeEventListener('online', online);
      window.removeEventListener('offline', offline);
    };
  },
};
