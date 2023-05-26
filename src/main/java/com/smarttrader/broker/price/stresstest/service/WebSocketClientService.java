package com.smarttrader.broker.price.stresstest.service;

import com.smarttrader.broker.price.stresstest.config.SecurityConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;

import java.net.URI;

@Service
public class WebSocketClientService {

    private final WebSocketClient client;
    private final HttpHeaders httpHeaders;

    public WebSocketClientService(SecurityConfig securityConfig) {
        this.client = new ReactorNettyWebSocketClient();
        this.httpHeaders = new HttpHeaders();

        httpHeaders.add(SecurityConfig.WEBSOCKET_HEADER_KEY_AUTH, SecurityConfig.ACCESS_TOKEN_KEY + ", " + securityConfig.getToken());
    }

    public Mono<Void> execute(WebSocketHandler handler) {
        return client.execute(
                URI.create("ws://localhost:8080/subscription"),
                httpHeaders,
                handler);
    }
}
