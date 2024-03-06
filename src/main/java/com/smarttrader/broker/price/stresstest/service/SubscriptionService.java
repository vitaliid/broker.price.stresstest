package com.smarttrader.broker.price.stresstest.service;

import com.google.common.collect.Lists;
import com.smarttrader.broker.price.stresstest.config.SubscriptionConfig;
import com.smarttrader.broker.price.stresstest.dto.proto.SubscriptionStructures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String HEARTBEAT_REQUEST = "{\"heartbeat\":\"ping\"}";
    public static final String HEARTBEAT_RESPONSE = "{\"heartbeat\":\"pong\"}";
    private static final int FLUX_FLAT_MAP_CONCURRENCY = 5000;

    private final SubscriptionConfig subscriptionsConfig;
    private final WebSocketClientService webSocketClientService;
    private final BrokerAccountService brokerAccountService;
    private final ConnectionWatcher connectionWatcher;
    private boolean subscriptionStarted = false;
    private Instant startedAt;

    private final Random random = new Random();

    public void subscribe(boolean unsubscribe) {
        if (subscriptionStarted) {
            return;
        }

        startedAt = Instant.now();

        List<SubscriptionStructures.SubscriptionRequest> requests = getHardcodedOne();//it is possible also getFromFile and getHardcodedOne

        if (requests.isEmpty()) {
            log.error("There is no contracts to request");
            return;
        }

        AtomicLong sentRequests = new AtomicLong();
        Flux.fromIterable(requests)
                .window(subscriptionsConfig.getAccountsNumberForThrottling())
                .zipWith(Flux.interval(Duration.ZERO, Duration.ofSeconds(subscriptionsConfig.getAccountsDelayForThrottling())))
                .flatMap(Tuple2::getT1, FLUX_FLAT_MAP_CONCURRENCY)
                .flatMap(request -> {
                            log.info("Sending request: {}/{}", sentRequests.incrementAndGet(), requests.size());
                            return webSocketClientService
                                    .execute(session ->
                                            {

                                                Flux<WebSocketMessage> income = session.receive().share();

                                                SubscriptionFlow flow = new SubscriptionFlow(session, income, connectionWatcher)
                                                        .subscribe(request);

                                                if (unsubscribe) {
                                                    flow = flow.unsubscribe(subscriptionsConfig.getUnsubscribeDelay());
                                                }

                                                return Flux.merge(flow.complete(), createHeartbeatSender(session),
                                                        income
                                                                .filter(webSocketMessage -> webSocketMessage.getType() == WebSocketMessage.Type.TEXT)
                                                                .filter(webSocketMessage -> webSocketMessage.getPayloadAsText().equals(HEARTBEAT_RESPONSE))
                                                                //.doOnNext(webSocketMessage -> log.info("Received heartbeat message: {}", webSocketMessage.getPayloadAsText()))
                                                ).then();
                                            },
                                            sentRequests.get());//USER ID
                        }, FLUX_FLAT_MAP_CONCURRENCY
                )
                .subscribe();

        subscriptionStarted = true;
    }


    public ConnectionWatcher.SessionsSummary getSessions() {
        ConnectionWatcher.SessionsSummary sessions = connectionWatcher.getSessions();
        Instant now = Instant.now();
        sessions.setDuration(Duration.between(Optional.ofNullable(startedAt)
                                .orElse(now),
                        now).toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase());
        return sessions;
    }

    public Map<String, TreeMap<String, List<ConnectionWatcher.SessionDescriptor>>> getOpenSessions() {
        return connectionWatcher.getOpenSessions();
    }

    private List<SubscriptionStructures.SubscriptionRequest> getFromRemoteDb() {
        log.info("Reading accounts page: number = {} and size = {}", subscriptionsConfig.getAccountsPageNumber(), subscriptionsConfig.getAccountsPageSize());

        List<Integer> brokerIdsOfTimeoutServers = List.of(5, 23, 59, 63, 71, 79, 201, 254, 279, 305, 508, 549, 602, 686, 725, 728, 738, 759, 805, 47);
        List<Integer> brokerIdsOfBrokenServers = List.of(22, 103, 133, 137, 139, 143, 188, 187, 310, 422, 429, 559, 571, 635, 650, 656, 601, 688, 722, 723, 726, 727, 839, 840);
        List<String> invalidAccounts = List.of("925200", "70685032", "888121021");
        List<SubscriptionStructures.Contract> contractList = brokerAccountService.get(
                        subscriptionsConfig.getAccountsPageNumber(),
                        subscriptionsConfig.getAccountsPageSize(),
                        Stream.concat(brokerIdsOfTimeoutServers.stream(), brokerIdsOfBrokenServers.stream()).toList(),
                        invalidAccounts)
                .stream()
                .filter(account -> {
                    boolean noForexSymbols = account.getSymbols().isEmpty();
                    if (noForexSymbols) {
                        log.info("This account will be ignored: {}", account);
                    }
                    return !noForexSymbols;
                })
                .flatMap(account -> {
                    if (Boolean.TRUE.equals(subscriptionsConfig.getHardcodeEnabled())) {
                        return Stream.of(
                                SubscriptionStructures.Contract.newBuilder()
                                        .setAccount(account.getAccount())
                                        .setBrokerId(account.getBrokerId())
                                        .setSymbol(subscriptionsConfig.getHardcodedSymbol())
                                        .build());
                    }

                    log.info("Symbols for account {}: {}", account, account.getSymbols());

                    List<String> preparedSymbols = new ArrayList<>(account.getSymbols());

                    if (subscriptionsConfig.getShuffleEnabled()) {
                        random.setSeed(Instant.now().toEpochMilli());
                        Collections.shuffle(preparedSymbols, random);
                        log.info("Shuffled symbols for account {}: {}", account, preparedSymbols);
                    }

                    Collections.sort(preparedSymbols);
                    List<String> symbols = preparedSymbols
                            .subList(0, Math.min(preparedSymbols.size(), subscriptionsConfig.getSymbolsPerAccount()));
                    log.info("Requested symbols for account {}: {}", account, symbols);

                    return symbols.stream().map(randomSymbol -> SubscriptionStructures.Contract.newBuilder()
                            .setAccount(account.getAccount())
                            .setBrokerId(account.getBrokerId())
                            .setSymbol(randomSymbol)
                            .build());
                })
                .toList();
        log.info("Loaded contracts: {}", contractList.size());

        List<SubscriptionStructures.SubscriptionRequest> requestsWithMultipleContracts = Lists.partition(contractList,
                        subscriptionsConfig.getContractsNumber()).stream()
                .map(contracts ->
                        SubscriptionStructures.SubscriptionRequest
                                .newBuilder()
                                .addAllContract(contracts)
                                .build())
                .toList();

        log.info("Built subscription requests: {}", requestsWithMultipleContracts.size());

        return requestsWithMultipleContracts;
    }

    private List<SubscriptionStructures.SubscriptionRequest> getFromLocalDB() {
        SubscriptionStructures.SubscriptionRequest deserializedRequest = SubscriptionStructures.SubscriptionRequest.newBuilder().build();
        try (FileInputStream fis = new FileInputStream("requests.db");
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            while (ois.available() > 0) {
                deserializedRequest = SubscriptionStructures.SubscriptionRequest.parseFrom(ois);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return Lists.partition(deserializedRequest.getContractList(),
                        subscriptionsConfig.getContractsNumber()).stream()
                .map(contracts ->
                        SubscriptionStructures.SubscriptionRequest
                                .newBuilder()
                                .addAllContract(contracts)
                                .build())
                .limit(subscriptionsConfig.getAccountsPageSize())
                .toList();
    }

    private List<SubscriptionStructures.SubscriptionRequest> getHardcodedOne() {
        SubscriptionStructures.SubscriptionRequest singleRequest = SubscriptionStructures.SubscriptionRequest
                .newBuilder()
                .addContract(SubscriptionStructures.Contract.newBuilder()
                        .setAccount(2424723)
                        .setBrokerId(12345)
                        .setSymbol("EURCHF")
                        .build())
                .build();
        return IntStream.range(0, subscriptionsConfig.getAccountsPageSize())
                .mapToObj(operand -> singleRequest)
                .toList();
    }

    private Mono<Void> createHeartbeatSender(WebSocketSession session) {
        return session.send(Mono.just(HEARTBEAT_REQUEST)
                .map(session::textMessage)
                .repeat()
                .delayElements(Duration.ofSeconds(30))
                .subscribeOn(Schedulers.immediate())
                .publishOn(Schedulers.immediate()));
    }
}
