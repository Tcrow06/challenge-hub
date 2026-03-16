import { Client } from '@stomp/stompjs';
import { useAuthStore } from '../store/authStore';
import { useNotificationStore } from '../store/notificationStore';

export const createWsClient = () => {
  const wsBase = import.meta.env.VITE_WS_BASE_URL ?? 'ws://localhost:8080';
  let client: Client | null = null;

  const connect = () => {
    const token = useAuthStore.getState().accessToken;
    if (!token) {
      return;
    }

    client = new Client({
      brokerURL: `${wsBase}/ws`,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      onConnect: () => {
        client?.subscribe('/user/queue/notifications', () => {
          useNotificationStore.setState((state) => ({ unreadCount: state.unreadCount + 1 }));
        });
      },
    });

    client.activate();
  };

  const disconnect = async () => {
    if (client) {
      await client.deactivate();
      client = null;
    }
  };

  return { connect, disconnect };
};
