package com.pm.patientservice;

import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.KafkaProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class PatientServiceApplicationTests {

  @MockitoBean
  BillingServiceGrpcClient billingServiceGrpcClient;

  @MockitoBean
  KafkaProducer kafkaProducer;

  @Test
  void contextLoads() {
  }

}
