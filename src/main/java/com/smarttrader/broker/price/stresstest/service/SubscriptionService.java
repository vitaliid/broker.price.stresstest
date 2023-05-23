package com.smarttrader.broker.price.stresstest.service;

import com.smarttrader.broker.price.stresstest.config.SecurityConfig;
import com.smarttrader.broker.price.stresstest.config.SubscriptionsConfig;
import com.smarttrader.broker.price.stresstest.dto.proto.SubscriptionStructures;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionsConfig subscriptionsConfig;
    private final SecurityConfig securityConfig;

    @PostConstruct
    private void subscribe() {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(SecurityConfig.WEBSOCKET_HEADER_KEY_AUTH, SecurityConfig.ACCESS_TOKEN_KEY + ", " + securityConfig.getToken());

        List<SubscriptionStructures.Contract> contracts = subscriptionsConfig.getSubscriptions().stream()
                .map(contract -> SubscriptionStructures.Contract.newBuilder()
                        .setAccount(contract.getAccount())
                        .setBrokerId(contract.getBrokerId())
                        .setSymbol(contract.getSymbol())
                        .build())
                .toList();
        SubscriptionStructures.SubscriptionRequest request = SubscriptionStructures.SubscriptionRequest.newBuilder()
                .addAllContract(contracts)
                .build();

        client.execute(
                        URI.create("ws://localhost:8080/subscription"),
                        httpHeaders,
                        session -> session.send(Mono.just(request.toByteArray())
                                        .map(bytes -> session.binaryMessage(dataBufferFactory -> dataBufferFactory.wrap(bytes)))
                                )
                                .thenMany(session.receive()
                                        .map(WebSocketMessage::getPayloadAsText)
                                        .log())
                                .doOnError(throwable -> {
                                    log.error("Error happened", throwable);
                                })
                                .then())
                .subscribe();
    }
}
