package com.pm.analyticsservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.pm.analyticsservice.exception.MalformedPatientEventException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaConsumer {

  private final PatientEventHandler handler;

  public KafkaConsumer(PatientEventHandler handler) {
    this.handler = handler;
  }

  /**
   * Parse failures propagate to the container's error handler, which sends the record to the
   * dead-letter topic.
   */
  @KafkaListener(topics = "${analytics.kafka.topic}")
  public void consumeEvent(byte[] event) {
    handler.handle(parse(event));
  }

  private PatientEvent parse(byte[] event) {
    try {
      return PatientEvent.parseFrom(event);
    } catch (InvalidProtocolBufferException e) {
      throw new MalformedPatientEventException("Unable to parse PatientEvent payload", e);
    }
  }
}
