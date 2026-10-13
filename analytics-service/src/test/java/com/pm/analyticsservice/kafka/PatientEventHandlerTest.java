package com.pm.analyticsservice.kafka;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import patient.events.PatientEvent;

class PatientEventHandlerTest {

  private final PatientEventHandler handler = new PatientEventHandler();

  @Test
  void handlesValidEvent() {
    PatientEvent event = PatientEvent.newBuilder()
        .setPatientId("p-1")
        .setName("Jane Doe")
        .setEmail("jane@example.com")
        .setEventType("PATIENT_CREATED")
        .build();

    assertThatCode(() -> handler.handle(event)).doesNotThrowAnyException();
  }

  @Test
  void handlesEmptyEvent() {
    assertThatCode(() -> handler.handle(PatientEvent.getDefaultInstance()))
        .doesNotThrowAnyException();
  }
}
