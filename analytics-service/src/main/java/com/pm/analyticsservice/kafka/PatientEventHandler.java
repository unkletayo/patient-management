package com.pm.analyticsservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import patient.events.PatientEvent;

@Component
public class PatientEventHandler {

  private static final Logger log = LoggerFactory.getLogger(PatientEventHandler.class);

  public void handle(PatientEvent event) {
    // ... perform any business related to analytics here

    // Name and email are personal data, so only identifiers are logged.
    log.atInfo()
        .addKeyValue("patientId", event.getPatientId())
        .addKeyValue("eventType", event.getEventType())
        .log("patient event processed");
  }
}
