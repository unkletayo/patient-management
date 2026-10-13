package com.pm.apigateway.filter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pm.apigateway.config.AuthServiceProperties;
import com.pm.apigateway.error.GatewayErrorWriter;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Validates the bearer token against auth-service before forwarding. Identity
 * headers are always derived from the validation result; client-supplied
 * values are discarded.
 */
@Component
public class JwtValidationGatewayFilterFactory extends
    AbstractGatewayFilterFactory<Object> {

  static final String USER_EMAIL_HEADER = "X-User-Email";
  static final String USER_ROLE_HEADER = "X-User-Role";

  private static final Logger log =
      LoggerFactory.getLogger(JwtValidationGatewayFilterFactory.class);
  private static final String BEARER_PREFIX = "Bearer ";

  private final WebClient webClient;
  private final AuthServiceProperties properties;
  private final GatewayErrorWriter errorWriter;

  public JwtValidationGatewayFilterFactory(WebClient.Builder webClientBuilder,
      AuthServiceProperties properties, GatewayErrorWriter errorWriter) {
    super(Object.class);
    this.webClient = webClientBuilder.baseUrl(properties.url()).build();
    this.properties = properties;
    this.errorWriter = errorWriter;
  }

  @Override
  public GatewayFilter apply(Object config) {
    return (exchange, chain) -> {
      String authorization =
          exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

      if (!isBearer(authorization)) {
        return errorWriter.write(exchange, HttpStatus.UNAUTHORIZED,
            "Missing or malformed Authorization header");
      }

      return validate(authorization)
          .flatMap(user -> chain.filter(withIdentity(exchange, user)))
          .onErrorResume(AuthFailureException.class,
              ex -> errorWriter.write(exchange, ex.status(), ex.getMessage()));
    };
  }

  private static boolean isBearer(String authorization) {
    return authorization != null
        && authorization.startsWith(BEARER_PREFIX)
        && StringUtils.hasText(authorization.substring(BEARER_PREFIX.length()));
  }

  private Mono<AuthenticatedUser> validate(String authorization) {
    return webClient.get()
        .uri("/validate")
        .header(HttpHeaders.AUTHORIZATION, authorization)
        .exchangeToMono(this::toUser)
        .timeout(properties.timeout())
        .onErrorMap(ex -> !(ex instanceof AuthFailureException), ex -> {
          log.warn("Auth service validation failed: {}", ex.toString());
          return ex instanceof TimeoutException
              ? new AuthFailureException(HttpStatus.SERVICE_UNAVAILABLE,
                  "Authentication service timed out")
              : new AuthFailureException(HttpStatus.SERVICE_UNAVAILABLE,
                  "Authentication service unavailable");
        });
  }

  private Mono<AuthenticatedUser> toUser(ClientResponse response) {
    HttpStatusCode status = response.statusCode();

    if (status.is2xxSuccessful()) {
      return response.bodyToMono(ValidationEnvelope.class)
          .map(ValidationEnvelope::responseBody)
          .filter(user -> user != null
              && StringUtils.hasText(user.email())
              && StringUtils.hasText(user.role()))
          .switchIfEmpty(Mono.error(new AuthFailureException(HttpStatus.BAD_GATEWAY,
              "Invalid response from authentication service")));
    }

    return response.releaseBody().then(Mono.error(
        status.value() == HttpStatus.UNAUTHORIZED.value()
            || status.value() == HttpStatus.FORBIDDEN.value()
            ? new AuthFailureException(HttpStatus.UNAUTHORIZED,
                "Invalid or expired token")
            : new AuthFailureException(HttpStatus.SERVICE_UNAVAILABLE,
                "Authentication service unavailable")));
  }

  private static ServerWebExchange withIdentity(ServerWebExchange exchange,
      AuthenticatedUser user) {
    return exchange.mutate()
        .request(request -> request.headers(headers -> {
          headers.remove(USER_EMAIL_HEADER);
          headers.remove(USER_ROLE_HEADER);
          headers.set(USER_EMAIL_HEADER, user.email());
          headers.set(USER_ROLE_HEADER, user.role());
        }))
        .build();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ValidationEnvelope(AuthenticatedUser responseBody) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record AuthenticatedUser(String email, String role) {
  }

  /** Auth failure that maps directly to an error response. */
  static final class AuthFailureException extends RuntimeException {

    private final transient HttpStatusCode status;

    AuthFailureException(HttpStatusCode status, String message) {
      super(message, null, false, false);
      this.status = status;
    }

    HttpStatusCode status() {
      return status;
    }
  }
}
