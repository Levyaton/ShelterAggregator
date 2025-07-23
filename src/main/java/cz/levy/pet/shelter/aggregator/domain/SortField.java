package cz.levy.pet.shelter.aggregator.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum SortField {
  ID("id"),
  NAME("name"),
  ESTIMATED_AGE("estimatedAgeInYears"),
  CURRENT_WEIGHT("currentWeight"),
  ESTIMATED_FINAL_WEIGHT_MIN("estimatedFinalWeightMin"),
  ESTIMATED_FINAL_WEIGHT_MAX("estimatedFinalWeightMax"),
  SHELTER_ID("shelterId"),
  ;

  private final String fieldName;

  SortField(String fieldName) {
    this.fieldName = fieldName;
  }

  @JsonValue
  public String json() {
    return name();
  }

  @JsonCreator
  public static SortField from(String input) {
    try {
      return SortField.valueOf(input.toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          "Invalid sort value '"
              + input
              + "', must be one of "
              + java.util.Arrays.toString(values()));
    }
  }
}
