package cz.levy.pet.shelter.aggregator.utils;

import org.assertj.core.api.Assertions;

public class TestUtils {

  public static <T> void assertThatEqualsRecursive(
      T response, T expected, String... ignoredFields) {
    Assertions.assertThat(response)
        .usingRecursiveComparison()
        .ignoringFields(ignoredFields)
        .isEqualTo(expected);
  }
}
