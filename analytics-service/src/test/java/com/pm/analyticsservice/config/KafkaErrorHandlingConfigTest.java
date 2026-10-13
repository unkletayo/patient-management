package com.pm.analyticsservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm.analyticsservice.exception.MalformedPatientEventException;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;

class KafkaErrorHandlingConfigTest {

  private static final String DLT = "patient.DLT";

  @SuppressWarnings("unchecked")
  private final KafkaTemplate<Object, Object> template = mock(KafkaTemplate.class);
  private final KafkaErrorHandlingConfig config = new KafkaErrorHandlingConfig();
  private final DefaultErrorHandler errorHandler =
      config.kafkaErrorHandler(config.deadLetterPublishingRecoverer(template, DLT));

  @Test
  @SuppressWarnings("unchecked")
  void malformedRecordGoesStraightToDeadLetterTopic() {
    when(template.send(any(ProducerRecord.class)))
        .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
    byte[] payload = {1, 2, 3};
    ConsumerRecord<String, byte[]> record = new ConsumerRecord<>("patient", 0, 5L, "k", payload);

    errorHandler.handleOne(
        new MalformedPatientEventException("bad", new RuntimeException("cause")),
        record, mock(Consumer.class), mock(MessageListenerContainer.class));

    ArgumentCaptor<ProducerRecord<Object, Object>> sent =
        ArgumentCaptor.forClass(ProducerRecord.class);
    verify(template).send(sent.capture());
    assertThat(sent.getValue().topic()).isEqualTo(DLT);
    assertThat(sent.getValue().value()).isEqualTo(payload);
  }
}
