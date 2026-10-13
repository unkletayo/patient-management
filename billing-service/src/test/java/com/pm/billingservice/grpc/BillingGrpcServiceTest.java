package com.pm.billingservice.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import com.pm.billingservice.exception.InvalidBillingRequestException;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.service.BillingAccountService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class BillingGrpcServiceTest {

  private BillingAccountService accountService;
  private Server server;
  private ManagedChannel channel;
  private BillingServiceGrpc.BillingServiceBlockingStub stub;

  @BeforeEach
  void setUp() throws Exception {
    accountService = Mockito.mock(BillingAccountService.class);
    String name = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(name).directExecutor()
        .addService(new BillingGrpcService(accountService)).build().start();
    channel = InProcessChannelBuilder.forName(name).directExecutor().build();
    stub = BillingServiceGrpc.newBlockingStub(channel);
  }

  @AfterEach
  void tearDown() throws Exception {
    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
  }

  private static BillingRequest request() {
    return BillingRequest.newBuilder()
        .setPatientId("p-1").setName("Jane").setEmail("jane@example.com").build();
  }

  @Test
  void returnsGeneratedAccountId() {
    BillingAccount account = new BillingAccount("p-1", "Jane", "jane@example.com");
    UUID id = UUID.randomUUID();
    ReflectionTestUtils.setField(account, "id", id);
    when(accountService.createAccount("p-1", "Jane", "jane@example.com")).thenReturn(account);

    BillingResponse response = stub.createBillingAccount(request());

    assertThat(response.getAccountId()).isEqualTo(id.toString());
    assertThat(response.getStatus()).isEqualTo("ACTIVE");
  }

  @Test
  void mapsValidationErrorToInvalidArgument() {
    when(accountService.createAccount(any(), any(), any()))
        .thenThrow(new InvalidBillingRequestException("name must not be blank"));

    assertThatThrownBy(() -> stub.createBillingAccount(request()))
        .isInstanceOfSatisfying(StatusRuntimeException.class,
            e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
  }

  @Test
  void hidesInternalErrorDetails() {
    when(accountService.createAccount(any(), any(), any()))
        .thenThrow(new IllegalStateException("jdbc:secret-connection-detail"));

    assertThatThrownBy(() -> stub.createBillingAccount(request()))
        .isInstanceOfSatisfying(StatusRuntimeException.class, e -> {
          assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
          assertThat(e.getMessage()).doesNotContain("secret");
        });
  }
}
