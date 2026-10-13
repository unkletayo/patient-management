package com.pm.apigateway.error;

import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * Converts any unhandled gateway error (unmatched route, upstream failure,
 * timeout) into a JSON envelope. Ordered ahead of Boot's default handler.
 */
@Component
@Order(-2)
public class GatewayErrorHandler implements WebExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

  private final GatewayErrorWriter errorWriter;

  public GatewayErrorHandler(GatewayErrorWriter errorWriter) {
    this.errorWriter = errorWriter;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    if (exchange.getResponse().isCommitted()) {
      return Mono.error(ex);
    }

    HttpStatusCode status = resolveStatus(ex);
    if (status.is5xxServerError()) {
      log.error("Gateway error on {} {}", exchange.getRequest().getMethod(),
          exchange.getRequest().getPath(), ex);
    }
    return errorWriter.write(exchange, status, messageFor(status));
  }

  private static HttpStatusCode resolveStatus(Throwable ex) {
    if (ex instanceof ErrorResponse errorResponse) {
      return errorResponse.getStatusCode();
    }
    if (ex instanceof TimeoutException) {
      return HttpStatus.GATEWAY_TIMEOUT;
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  private static String messageFor(HttpStatusCode status) {
    return switch (status.value()) {
      case 404 -> "No route found for the requested path";
      case 502 -> "Bad response from upstream service";
      case 503 -> "Upstream service unavailable";
      case 504 -> "Upstream service timed out";
      default -> status.is5xxServerError()
          ? "Internal gateway error"
          : "Request could not be processed";
    };
  }
}
