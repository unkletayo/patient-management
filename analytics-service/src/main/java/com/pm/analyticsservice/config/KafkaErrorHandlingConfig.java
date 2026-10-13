package com.pm.analyticsservice.config;

import com.pm.analyticsservice.exception.MalformedPatientEventException;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaErrorHandlingConfig {

  private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlingConfig.class);

  static final int MAX_RETRIES = 3;
  static final long INITIAL_INTERVAL_MS = 1_000L;
  static final double MULTIPLIER = 2.0;
  static final long MAX_INTERVAL_MS = 10_000L;

  @Bean
  DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
      KafkaOperations<?, ?> kafkaOperations,
      @Value("${analytics.kafka.dead-letter-topic}") String deadLetterTopic) {
    // Partition -1 lets the producer pick one, so the DLT need not match the source partition count.
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
        kafkaOperations, (record, ex) -> new TopicPartition(deadLetterTopic, -1));
    recoverer.setFailIfSendResultIsError(true);
    return recoverer;
  }

  /**
   * Picked up by Spring Boot and applied to the listener container factory.
   * Malformed payloads skip retries; other failures retry with bounded exponential backoff.
   */
  @Bean
  DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
    ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(MAX_RETRIES);
    backOff.setInitialInterval(INITIAL_INTERVAL_MS);
    backOff.setMultiplier(MULTIPLIER);
    backOff.setMaxInterval(MAX_INTERVAL_MS);

    DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
    handler.addNotRetryableExceptions(MalformedPatientEventException.class);
    handler.setRetryListeners((record, ex, attempt) -> log.atWarn()
        .addKeyValue("topic", record.topic())
        .addKeyValue("partition", record.partition())
        .addKeyValue("offset", record.offset())
        .addKeyValue("attempt", attempt)
        .setCause(ex)
        .log("record processing failed, retrying"));
    return handler;
  }
}
