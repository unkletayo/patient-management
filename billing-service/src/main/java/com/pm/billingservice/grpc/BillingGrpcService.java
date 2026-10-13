package com.pm.billingservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc.BillingServiceImplBase;
import com.pm.billingservice.exception.InvalidBillingRequestException;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.service.BillingAccountService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BillingGrpcService extends BillingServiceImplBase {

  private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);

  private final BillingAccountService billingAccountService;

  public BillingGrpcService(BillingAccountService billingAccountService) {
    this.billingAccountService = billingAccountService;
  }

  @Override
  public void createBillingAccount(BillingRequest request,
      StreamObserver<BillingResponse> responseObserver) {
    log.info("createBillingAccount request received for patient {}", request.getPatientId());

    try {
      BillingAccount account = billingAccountService.createAccount(
          request.getPatientId(), request.getName(), request.getEmail());

      responseObserver.onNext(BillingResponse.newBuilder()
          .setAccountId(account.getId().toString())
          .setStatus(account.getStatus().name())
          .build());
      responseObserver.onCompleted();
    } catch (InvalidBillingRequestException e) {
      responseObserver.onError(Status.INVALID_ARGUMENT
          .withDescription(e.getMessage()).asRuntimeException());
    } catch (Exception e) {
      log.error("Failed to create billing account", e);
      responseObserver.onError(Status.INTERNAL
          .withDescription("Internal error").asRuntimeException());
    }
  }
}
