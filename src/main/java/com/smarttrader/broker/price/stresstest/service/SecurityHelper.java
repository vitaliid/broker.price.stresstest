package com.smarttrader.broker.price.stresstest.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.smarttrader.broker.price.stresstest.config.SecurityConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityHelper {

    private final SecurityConfig securityConfig;

    public String generateToken(Long userId) {
        Algorithm algorithm = Algorithm.HMAC512(securityConfig.getSecretKey());

        return JWT.create()
                .withIssuer("auth-light.authenticated-trader")
                .withSubject(String.valueOf(userId))
                .withClaim("role", "TRADER")
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(10 * 60 * 60))
                .withJWTId(UUID.randomUUID()
                        .toString())
                .withNotBefore(Instant.now().minusSeconds(10))
                .sign(algorithm);
    }
}
