package com.pm.analyticsservice.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.google.protobuf.InvalidProtocolBufferException;
import com.pm.analyticsservice.exception.MalformedPatientEventException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import patient.events.PatientEvent;

class KafkaConsumerTest {

  private final PatientEventHandler handler = mock(PatientEventHandler.class);
  private final KafkaConsumer consumer = new KafkaConsumer(handler);

  @Test
  void delegatesValidEventToHandler() {
    PatientEvent event = PatientEvent.newBuilder()
        .setPatientId("p-1")
        .setName("Jane Doe")
        .setEmail("jane@example.com")
        .setEventType("PATIENT_CREATED")
        .build();

    consumer.consumeEvent(event.toByteArray());

    ArgumentCaptor<PatientEvent> captor = ArgumentCaptor.forClass(PatientEvent.class);
    verify(handler).handle(captor.capture());
    assertThat(captor.getValue()).isEqualTo(event);
  }

  @Test
  void rejectsCorruptBytesWithoutCallingHandler() {
    byte[] corrupt = {0x0A, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x7F};

    assertThatThrownBy(() -> consumer.consumeEvent(corrupt))
        .isInstanceOf(MalformedPatientEventException.class)
        .hasCauseInstanceOf(InvalidProtocolBufferException.class);
    verifyNoInteractions(handler);
  }
}
