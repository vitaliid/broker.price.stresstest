package com.smarttrader.broker.price.stresstest.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AppConfig {


    @Value("${app.target}")
    private String target;

    @Value("${app.silence-timeout-seconds}")
    private Long silenceTimeoutSeconds;
}
