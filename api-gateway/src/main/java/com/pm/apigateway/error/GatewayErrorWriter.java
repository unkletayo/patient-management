package com.pm.apigateway.error;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/** Writes failure envelopes as JSON directly to the response. */
@Component
public class GatewayErrorWriter {

  private final ObjectMapper objectMapper;

  public GatewayErrorWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Mono<Void> write(ServerWebExchange exchange, HttpStatusCode status,
      String message) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    byte[] body = objectMapper.writeValueAsBytes(
        ApiEnvelope.failure(status.value(), message));
    DataBuffer buffer = response.bufferFactory().wrap(body);
    return response.writeWith(Mono.just(buffer));
  }
}
