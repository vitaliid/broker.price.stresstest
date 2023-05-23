package com.smarttrader.broker.price.stresstest.config;


import com.smarttrader.broker.price.stresstest.dto.proto.SubscriptionContract;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
@ConfigurationProperties
@Getter
@Setter
public class SubscriptionsConfig {

    private List<SubscriptionContract> subscriptions;
}
