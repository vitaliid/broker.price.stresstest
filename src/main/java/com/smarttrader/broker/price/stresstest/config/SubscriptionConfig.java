package com.smarttrader.broker.price.stresstest.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties
@Getter
@Setter
public class SubscriptionConfig {

    @Value("${subscription.throttling.accounts}")
    private Integer accountsNumberForThrottling;

    @Value("${subscription.throttling.seconds}")
    private Integer accountsDelayForThrottling;

    @Value("${subscription.hardcode.enabled}")
    private Boolean hardcodeEnabled;

    @Value("${subscription.hardcode.symbol}")
    private String hardcodedSymbol;

    @Value("${subscription.accounts.page.number}")
    private Integer accountsPageNumber;

    @Value("${subscription.accounts.page.size}")
    private Integer accountsPageSize;

    @Value("${subscription.accounts.symbols}")
    private Integer symbolsPerAccount;

    @Value("${subscription.shuffle}")
    private Boolean shuffleEnabled;

    @Value("${subscription.contracts}")
    private Integer contractsNumber;

    @Value("${unsubscribe.delay}")
    private Integer unsubscribeDelay;
}
