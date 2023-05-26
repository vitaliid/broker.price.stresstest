package com.smarttrader.broker.price.stresstest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
public class SubscriptionFlow {

    private final WebSocketSession session;
    private Mono<Void> sessionComplete;

    public SubscriptionFlow(WebSocketSession session) {
        this.session = session;
    }

    public SubscriptionFlow subscribe(byte[] message) {
        sessionComplete = session.send(Mono.just(message)
                        .map(bytes -> session.binaryMessage(dataBufferFactory -> dataBufferFactory.wrap(bytes)))
                )
                .thenMany(session.receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .log())
                .doOnError(throwable -> log.error("Error happened", throwable)).then();
        return this;
    }

    public SubscriptionFlow disconnect(long afterSeconds) {
        if (sessionComplete == null) {
            throw new IllegalStateException("It should be subscribed first");
        }

        if (afterSeconds <= 1) {
            throw new IllegalStateException("Parameter should be greater than 0 or ignored");
        }

        sessionComplete = sessionComplete.and(Mono.delay(Duration.ofSeconds(afterSeconds)).then(session.close(CloseStatus.GOING_AWAY))).log();
        return this;
    }

    public Mono<Void> complete() {
        if (sessionComplete == null) {
            throw new IllegalStateException("It should be subscribed first");
        }

        return sessionComplete;
    }

}
