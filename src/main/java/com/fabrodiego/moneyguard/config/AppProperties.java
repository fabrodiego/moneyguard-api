package com.fabrodiego.moneyguard.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Crypto crypto, Cors cors) {

    /** secret: base64, minimo 32 bytes (HS256). */
    public record Jwt(String secret, Duration ttl, String issuer) {
    }

    /** key: base64 de exatamente 32 bytes (AES-256). */
    public record Crypto(String key) {
    }

    public record Cors(List<String> origins) {
    }
}
