package com.smarttrader.broker.price.stresstest.controller;

import com.smarttrader.broker.price.stresstest.service.ConnectionWatcher;
import com.smarttrader.broker.price.stresstest.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("subscribe")
    public void subscribe(@RequestParam(required = false, defaultValue = "false") boolean unsubscribe) {
        subscriptionService.subscribe(unsubscribe);
    }

    @GetMapping("sessions")
    public ConnectionWatcher.SessionsSummary sessions() {
        return subscriptionService.getSessions();
    }

    @GetMapping("sessions/open")
    public Map<String, TreeMap<String, List<ConnectionWatcher.SessionDescriptor>>> sessionsOpen() {
        return subscriptionService.getOpenSessions();
    }
}
