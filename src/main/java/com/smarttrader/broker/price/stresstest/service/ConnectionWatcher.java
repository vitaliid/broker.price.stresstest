package com.smarttrader.broker.price.stresstest.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smarttrader.broker.price.stresstest.config.AppConfig;
import com.smarttrader.broker.price.stresstest.config.SubscriptionConfig;
import com.smarttrader.broker.price.stresstest.dto.proto.SubscriptionStructures;
import lombok.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ConnectionWatcher {

    private final AppConfig appConfig;

    private final SubscriptionConfig subscriptionConfig;

    private final ConcurrentMap<String, SessionDescriptor> sessions = new ConcurrentHashMap<>();

    enum SessionState {
        ALIVE,
        BROKEN,
        SILENT,//no prices for now
        OPEN//it has never received prices
    }

    @Builder
    @Getter
    public static class SessionDescriptor {
        private String sessionId;

        @JsonIgnore
        private WebSocketSession session;

        @Setter
        private SessionState connectionState;

        private String userId;
        private String account;
        private String brokerId;
        private String symbol;

        @Setter
        private BigDecimal messagesAmount;

        @Setter
        private Instant lastIncomingMessageTime;
    }

    public void addConnection(WebSocketSession session, String userId, SubscriptionStructures.SubscriptionRequest request) {
        //TODO fix getting contract !!!!!!!!!!!!!!!!!!!!!!!!!!
        SubscriptionStructures.Contract singleContract = request.getContract(0);
        SessionDescriptor sessionDescriptor = SessionDescriptor.builder()
                .sessionId(session.getId())
                .session(session)
                .userId(userId)
                .account(String.valueOf(singleContract.getAccount()))
                .brokerId(String.valueOf(singleContract.getBrokerId()))
                .symbol(singleContract.getSymbol())
                .messagesAmount(BigDecimal.ZERO)
                .connectionState(SessionState.OPEN)
                .build();

        sessions.put(sessionDescriptor.getSessionId(), sessionDescriptor);
    }

    public void brokeConnection(String sessionId) {
        sessions.get(sessionId).setConnectionState(SessionState.BROKEN);
    }

    public void updateConnection(String sessionId) {
        sessions.get(sessionId).setLastIncomingMessageTime(Instant.now());
    }

    @Data
    public static class SessionsSummary {
        private String duration;
        private long configuredAccounts = 0;
        private long total = 0;
        private Map<SessionState, Long> summary = new EnumMap<>(SessionState.class);
    }

    public SessionsSummary getSessions() {
        SessionsSummary sessionsSummary = new SessionsSummary();
        sessionsSummary.setTotal(sessions.size());

        Instant nowWithoutTimeout = Instant.now().minusSeconds(appConfig.getSilenceTimeoutSeconds());
        Map<SessionState, Long> summary = sessions.values()
                .stream()
                .collect(Collectors.groupingBy(descriptor ->
                        {
                            WebSocketSession session = descriptor.getSession();
                            if (session.isOpen()) {
                                if (descriptor.getLastIncomingMessageTime() == null) {
                                    return SessionState.OPEN;
                                } else if (descriptor.getLastIncomingMessageTime().isAfter(nowWithoutTimeout)) {
                                    return SessionState.ALIVE;
                                } else return SessionState.SILENT;
                            } else return SessionState.BROKEN;
                        },
                        TreeMap::new,
                        Collectors.counting()));
        sessionsSummary.setSummary(summary);
        sessionsSummary.setConfiguredAccounts(subscriptionConfig.getAccountsPageSize());
        return sessionsSummary;
    }

    public Map<String, TreeMap<String, List<SessionDescriptor>>> getOpenSessions() {
        return sessions.values()
                .stream()
                .filter(descriptor -> {
                    WebSocketSession session = descriptor.getSession();
                    if (session.isOpen() && descriptor.getLastIncomingMessageTime() == null) {
                        descriptor.setConnectionState(SessionState.OPEN);
                        return true;
                    }
                    return false;
                }).collect(Collectors.groupingBy(SessionDescriptor::getBrokerId,
                        Collectors.groupingBy(
                                SessionDescriptor::getAccount,
                                TreeMap::new,
                                Collectors.toList())));
    }
}
