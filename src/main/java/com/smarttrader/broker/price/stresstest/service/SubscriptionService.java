package com.smarttrader.broker.price.stresstest.service;

import com.smarttrader.broker.price.stresstest.config.SubscriptionsConfig;
import com.smarttrader.broker.price.stresstest.dto.proto.SubscriptionStructures;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionsConfig subscriptionsConfig;
    private final WebSocketClientService webSocketClientService;

    @PostConstruct
    private void subscribe() {
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

        webSocketClientService.execute(session -> new SubscriptionFlow(session)
                        .subscribe(request.toByteArray())
                        .disconnect(10L)
                        .complete())
                .subscribe();
    }
}
