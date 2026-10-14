package com.pm.billingservice.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores the status as its name in a plain varchar column. Unlike {@code @Enumerated}, this does
 * not make Hibernate emit a CHECK constraint listing the enum values, which {@code ddl-auto=update}
 * never refreshes when a value is added later.
 */
@Converter
public class BillingAccountStatusConverter
    implements AttributeConverter<BillingAccountStatus, String> {

  @Override
  public String convertToDatabaseColumn(BillingAccountStatus status) {
    return status == null ? null : status.name();
  }

  @Override
  public BillingAccountStatus convertToEntityAttribute(String value) {
    return value == null ? null : BillingAccountStatus.valueOf(value);
  }
}
