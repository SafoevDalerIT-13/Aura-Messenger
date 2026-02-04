package ru.messenger.config_service;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Включаем брокер сообщений в памяти
        registry.enableSimpleBroker(
                "/topic",  // Для всех в чате
                "/queue",  // Для приватных сообщений
                "/user"    // Для сообщений конкретному пользователю
        );

        // Префикс для отправки сообщений на сервер
        registry.setApplicationDestinationPrefixes("/app");

        // Префикс для личных сообщений
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Регистрация WebSocket endpoint'а
        registry.addEndpoint("/ws")  // http://localhost:8080/ws
                .setAllowedOriginPatterns(
                        "http://localhost:8080",  // Ваш сервер
                        "http://127.0.0.1:8080",  // Альтернативный localhost
                        "http://localhost"         // Без порта
                )
                .withSockJS();  // Fallback для старых браузеров
    }
}