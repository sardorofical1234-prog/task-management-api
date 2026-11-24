package uz.portfolio.taskmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String issuer,
        String secretBase64,
        Duration accessTokenTtl
) {
}
