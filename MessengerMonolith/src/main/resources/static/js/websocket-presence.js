class PresenceWebSocket {
    constructor() {
        this.stompClient = null;
        this.userId = null;
        this.connected = false;
        this.pingInterval = null;
        this.subscriptions = new Map();
    }

    connect(userId) {
        this.userId = userId;
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);

        // Отключаем отладочные логи
        this.stompClient.debug = null;

        this.stompClient.connect({},
            (frame) => this.onConnect(frame),
            (error) => this.onError(error)
        );
    }

    onConnect(frame) {
        console.log('WebSocket подключен для онлайн-статусов');
        this.connected = true;

        // Подписываемся на обновления статусов
        this.subscribeToPresence();

        // Устанавливаем себя онлайн
        this.setOnline(true);

        // Запускаем heartbeat
        this.startHeartbeat();

        // Обновляем UI
        this.updateUiStatus(true);
    }

    onError(error) {
        console.error('WebSocket ошибка:', error);
        this.connected = false;
        this.updateUiStatus(false);

        // Переподключение через 5 секунд
        setTimeout(() => {
            if (this.userId) {
                this.connect(this.userId);
            }
        }, 5000);
    }

    subscribeToPresence() {
        // 1. Подписываемся на свои обновления
        const myPresenceSub = this.stompClient.subscribe(
            `/topic/presence.${this.userId}`,
            (message) => this.handlePresenceUpdate(message)
        );
        this.subscriptions.set('my-presence', myPresenceSub);

        // 2. Подписываемся на общие обновления
        const globalPresenceSub = this.stompClient.subscribe(
            '/topic/presence.updates',
            (message) => this.handlePresenceUpdate(message)
        );
        this.subscriptions.set('global-presence', globalPresenceSub);
    }

    handlePresenceUpdate(message) {
        try {
            const data = JSON.parse(message.body);

            if (data.type === 'USER_PRESENCE_UPDATE') {
                this.updateContactStatus(data);
            }
        } catch (error) {
            console.error('Ошибка обработки обновления статуса:', error);
        }
    }

    updateContactStatus(data) {
        const { userId, isOnline, lastSeenAt, lastSeenFormatted } = data;

        // Обновляем статус во всех местах на странице
        this.updateStatusElements(userId, isOnline, lastSeenFormatted);
    }

    updateStatusElements(userId, isOnline, statusText) {
        // Обновляем в списке чатов
        document.querySelectorAll(`[data-user-id="${userId}"] .chat-status`).forEach(el => {
            el.textContent = statusText;
            el.className = `chat-status ${isOnline ? 'online' : 'offline'}`;
        });

        // Обновляем в списке контактов
        document.querySelectorAll(`[data-user-id="${userId}"] .user-status`).forEach(el => {
            el.textContent = statusText;
            el.className = `user-status ${isOnline ? 'online' : 'offline'}`;
        });

        // Если это текущий чат
        const currentChatUserId = document.querySelector('#currentChatAvatar')?.dataset?.userId;
        if (currentChatUserId == userId) {
            const statusEl = document.getElementById('currentChatStatus');
            if (statusEl) {
                statusEl.innerHTML = `
                    <span class="status-dot ${isOnline ? 'online' : 'offline'}"></span>
                    <span>${statusText}</span>
                `;
            }
        }
    }

    updateUiStatus(isOnline) {
        const myStatusEl = document.getElementById('userStatus');
        if (myStatusEl) {
            myStatusEl.textContent = isOnline ? '● Онлайн' : '● Оффлайн';
            myStatusEl.style.color = isOnline ? '#28a745' : '#dc3545';
        }
    }

    setOnline(isOnline) {
        if (!this.connected) return;

        this.stompClient.send(
            '/app/presence.update',
            {},
            JSON.stringify({ isOnline: isOnline })
        );
    }

    updateActivity() {
        if (!this.connected) return;

        this.stompClient.send(
            '/app/presence.update',
            {},
            JSON.stringify({ isOnline: true })
        );
    }

    requestUserStatus(userId) {
        if (!this.connected) return;

        this.stompClient.send(
            '/app/presence.request',
            {},
            JSON.stringify({ userId: userId })
        );
    }

    startHeartbeat() {
        // Отправляем пинг каждые 30 секунд
        this.pingInterval = setInterval(() => {
            if (this.connected) {
                this.updateActivity();
            }
        }, 30000);
    }

    disconnect() {
        if (this.stompClient && this.connected) {
            // Устанавливаем оффлайн
            this.setOnline(false);

            // Отписываемся от всех подписок
            this.subscriptions.forEach((sub, key) => {
                sub.unsubscribe();
            });
            this.subscriptions.clear();

            // Останавливаем heartbeat
            if (this.pingInterval) {
                clearInterval(this.pingInterval);
            }

            // Отключаемся
            this.stompClient.disconnect();
            this.connected = false;
            console.log('WebSocket отключен');
        }
    }
}

// Глобальный экземпляр
window.presenceSocket = new PresenceWebSocket();