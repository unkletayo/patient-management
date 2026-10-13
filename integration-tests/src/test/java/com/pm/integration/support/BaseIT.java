package com.pm.integration.support;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;

/** Base class for all ITs: skips the whole class when the gateway cannot be reached. */
public abstract class BaseIT {

  @BeforeAll
  static void requireGateway() {
    assumeTrue(
        GatewayClient.isReachable(),
        () ->
            "Gateway not reachable at "
                + GatewayClient.baseUrl()
                + "; start the stack or set GATEWAY_URL / -Dgateway.url. Skipping integration tests.");
  }
}
