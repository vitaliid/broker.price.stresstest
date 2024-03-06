package com.smarttrader.broker.price.stresstest.service;

import com.smarttrader.broker.price.stresstest.config.AppConfig;
import com.smarttrader.broker.price.stresstest.config.SecurityConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.net.URI;

@Service
public class WebSocketClientService {

    private final WebSocketClient client;
    private final HttpHeaders httpHeaders;
    private final AppConfig appConfig;
    private final SecurityHelper securityHelper;

    public WebSocketClientService(AppConfig appConfig, SecurityHelper securityHelper) {
        this.appConfig = appConfig;
        ConnectionProvider webFluxConnectionProvider = ConnectionProvider.create("webflux", 5000);
        HttpClient httpClient = HttpClient.create(webFluxConnectionProvider);
        this.client = new ReactorNettyWebSocketClient(httpClient);
        this.httpHeaders = new HttpHeaders();
        this.securityHelper = securityHelper;
    }

    public Mono<Void> execute(WebSocketHandler handler, Long userId) {
        httpHeaders.set(SecurityConfig.WEBSOCKET_HEADER_KEY_AUTH, SecurityConfig.ACCESS_TOKEN_KEY + ", " + securityHelper.generateToken(userId));

        return client.execute(
                URI.create("ws://" + appConfig.getTarget() + "/subscription"),
                httpHeaders,
                handler);
    }
}
