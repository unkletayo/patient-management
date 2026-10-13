package com.pm.apigateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Connection settings for the auth-service token validation endpoint. */
@ConfigurationProperties("auth.service")
public record AuthServiceProperties(
    String url,
    @DefaultValue("3s") Duration timeout) {
}
