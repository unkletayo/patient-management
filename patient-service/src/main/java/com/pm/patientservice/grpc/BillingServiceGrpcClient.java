package com.pm.patientservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import com.pm.patientservice.exception.BillingServiceException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceGrpcClient {

  private static final Logger log = LoggerFactory.getLogger(
      BillingServiceGrpcClient.class);
  private static final String CHANNEL_NAME = "billing";

  private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;
  private final Duration deadline;

  public BillingServiceGrpcClient(GrpcChannelFactory channelFactory,
      @Value("${billing.grpc.deadline:5s}") Duration deadline) {
    this.blockingStub = BillingServiceGrpc.newBlockingStub(
        channelFactory.createChannel(CHANNEL_NAME));
    this.deadline = deadline;
  }

  public BillingResponse createBillingAccount(String patientId, String name,
      String email) {

    BillingRequest request = BillingRequest.newBuilder().setPatientId(patientId)
        .setName(name).setEmail(email).build();

    try {
      BillingResponse response = blockingStub
          .withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS)
          .createBillingAccount(request);
      log.info("Received response from billing service via GRPC: {}",
          response);
      return response;
    } catch (StatusRuntimeException e) {
      Status.Code code = e.getStatus().getCode();
      boolean unavailable = code == Status.Code.UNAVAILABLE
          || code == Status.Code.DEADLINE_EXCEEDED;
      log.error("Billing service call failed with status {}", code, e);
      throw new BillingServiceException(
          "Billing service call failed: " + code, unavailable, e);
    }
  }
}
