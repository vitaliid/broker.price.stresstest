package com.smarttrader.broker.price.stresstest.service;

import com.smarttrader.broker.price.stresstest.dto.proto.SubscriptionStructures;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Slf4j
public class SubscriptionFlow {

    private final WebSocketSession session;
    private Mono<Void> sessionComplete;
    private final ConnectionWatcher connectionWatcher;

    private final Flux<WebSocketMessage> income;

    private final List<SubscriptionStructures.SubscriptionRequest> requests = new ArrayList<>();

    public SubscriptionFlow(WebSocketSession session, Flux<WebSocketMessage> income, ConnectionWatcher connectionWatcher) {
        this.session = session;
        this.connectionWatcher = connectionWatcher;
        this.income = income;
    }

    public SubscriptionFlow subscribe(SubscriptionStructures.SubscriptionRequest request) {
        requests.add(request);

        sessionComplete = session
                .send(Mono.just(request)
                        .doOnNext(sameRequest -> {
                            log.info("Sending subscription request: {}", sameRequest);
                            connectionWatcher.addConnection(session, null, sameRequest);
                        })
                        .doOnError(throwable -> {
                            log.error("[During sending] Error happened with session {}", session.getId(), throwable);
                            connectionWatcher.brokeConnection(session.getId());
                        })
                        .doOnCancel(() -> {
                            log.info("[During sending] Session {} unexpectedly cancelled", session.getId());
                            connectionWatcher.brokeConnection(session.getId());
                        })
                        .doOnTerminate(() -> {
                            log.info("[During sending] Session {} unexpectedly terminated", session.getId());
                            connectionWatcher.brokeConnection(session.getId());
                        })
                        .map(sameRequest ->
                                session.binaryMessage(dataBufferFactory -> dataBufferFactory.wrap(sameRequest.toByteArray())))
                )
                .doOnError(throwable -> {
                    log.error("[After request] Error happened with session {}", session.getId(), throwable);
                    connectionWatcher.brokeConnection(session.getId());
                })
                .doOnCancel(() -> {
                    log.info("[After request] Session {} unexpectedly cancelled", session.getId());
                    connectionWatcher.brokeConnection(session.getId());
                })
                .doOnTerminate(() -> {
                    log.info("[After request] Session {} unexpectedly terminated", session.getId());
                    connectionWatcher.brokeConnection(session.getId());
                })
                .and(income
                        .filter(webSocketMessage -> webSocketMessage.getType() == WebSocketMessage.Type.BINARY)
                        .map(WebSocketMessage::getPayload)
                        .log(log.getName(), Level.ALL)
                        .doOnNext(message -> {

                            byte[] bytes = new byte[message.readableByteCount()];
                            message.read(bytes);

                            log.info("Received message: {}", new String(bytes));

                            connectionWatcher.updateConnection(session.getId());
                        })
                        .doOnError(throwable -> {
                            log.error("[During receiving] Error happened with session {}", session.getId(), throwable);
                            connectionWatcher.brokeConnection(session.getId());
                        })
                        .doOnCancel(() -> {
                            log.info("[During receiving] Session {} unexpectedly cancelled", session.getId());
                            connectionWatcher.brokeConnection(session.getId());
                        })
                        .doOnComplete(() -> {
                            log.info("[During receiving] Session {} unexpectedly completed, request was: {}", session.getId(), request);
                            connectionWatcher.brokeConnection(session.getId());
                        })
                        .doOnTerminate(() -> {
                            log.info("[During receiving] Session {} unexpectedly terminated, request was: {}", session.getId(), request);
                            connectionWatcher.brokeConnection(session.getId());
                        })
                )
                .then()
                .doOnError(throwable -> {
                    log.error("[After all] Error happened with session {}", session.getId(), throwable);
                    connectionWatcher.brokeConnection(session.getId());
                })
                .doOnCancel(() -> {
                    log.info("[After all] Session {} unexpectedly cancelled, request was: {}", session.getId(), request);
                    connectionWatcher.brokeConnection(session.getId());
                })
                .doOnTerminate(() -> {
                    log.info("[After all] Session {} unexpectedly terminated, request was: {}", session.getId(), request);
                    connectionWatcher.brokeConnection(session.getId());
                });
        return this;
    }

    public SubscriptionFlow unsubscribe(int afterSeconds) {
        if (sessionComplete == null) {
            throw new IllegalStateException("It should be subscribed first");
        }

        if (afterSeconds <= 1) {
            throw new IllegalStateException("Parameter should be greater than 0 or this method can be ignored");
        }

        if (!requests.isEmpty()) {
            SubscriptionStructures.SubscriptionRequest lastRequest = requests.get(requests.size() - 1);
            ArrayList<SubscriptionStructures.Contract> contracts = new ArrayList<>(lastRequest.getContractList());

            List<SubscriptionStructures.SubscriptionRequest> requestsWithUnsubscriptions = new ArrayList<>();
            for (int i = 0; i < lastRequest.getContractCount(); i++) {
                contracts.remove(lastRequest.getContractCount() - 1 - i);
                SubscriptionStructures.SubscriptionRequest requestWithUnsubscription = SubscriptionStructures.SubscriptionRequest
                        .newBuilder(lastRequest)
                        .clearContract()
                        .addAllContract(contracts)
                        .build();
                requestsWithUnsubscriptions.add(requestWithUnsubscription);
            }
            sessionComplete = sessionComplete
                    .and(Mono
                            .delay(Duration.ofSeconds(afterSeconds))
                            //unsubscribe
                            .then(session.send(
                                            Flux.fromIterable(requestsWithUnsubscriptions)
                                                    .delayElements(Duration.ofSeconds(1))
                                                    .doOnNext(sameRequest -> {
                                                        requests.add(sameRequest);
                                                        log.info("Unsubscribed with request {}", sameRequest.toString());
                                                    })
                                                    .map(sameRequest ->
                                                            session.binaryMessage(dataBufferFactory -> dataBufferFactory.wrap(sameRequest.toByteArray())))
                                    )
                            )
                            //resubscribe
                            .then(session.send(
                                    Mono.just(lastRequest)
                                            .doOnNext(sameRequest ->
                                                    log.info("Resubscribed with request {}", sameRequest.toString()))
                                            .map(sameRequest ->
                                                    session.binaryMessage(dataBufferFactory -> dataBufferFactory.wrap(sameRequest.toByteArray())))))
                    );
        }
        return this;
    }

    public Mono<Void> complete() {
        if (sessionComplete == null) {
            throw new IllegalStateException("It should be subscribed first");
        }

        return sessionComplete;
    }
}
