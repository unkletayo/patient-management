package com.pm.analyticsservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.pm.analyticsservice.kafka.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AnalyticsServiceApplicationTests {

  @Autowired
  private KafkaConsumer consumer;

  @Test
  void contextLoads() {
    assertThat(consumer).isNotNull();
  }
}
