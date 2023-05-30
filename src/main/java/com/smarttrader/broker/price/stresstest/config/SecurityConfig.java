package com.smarttrader.broker.price.stresstest.config;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
@Getter
public class SecurityConfig {

    public static final String WEBSOCKET_HEADER_KEY_AUTH = "Sec-WebSocket-Protocol";
    public static final String ACCESS_TOKEN_KEY = "AccessToken";

    @Value("${app.security.token}")
    private String token;

    @Value("${app.security.secret.key}")
    private String secretKey;

    @Value("${app.security.roles}")
    private List<String> allowedRoles;
}
