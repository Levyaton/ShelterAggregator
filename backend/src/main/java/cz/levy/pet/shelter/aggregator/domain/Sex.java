package cz.levy.pet.shelter.aggregator.domain;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Sex {
  MALE,
  FEMALE,
  @JsonEnumDefaultValue
  UNKNOWN
}
