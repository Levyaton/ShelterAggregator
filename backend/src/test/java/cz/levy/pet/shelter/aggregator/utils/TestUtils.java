package cz.levy.pet.shelter.aggregator.utils;

import org.assertj.core.api.Assertions;

public class TestUtils {

  public static <T> void assertThatEqualsRecursive(T actual, T expected, String... ignoredFields) {
    Assertions.assertThat(actual)
        .usingRecursiveComparison()
        .ignoringFields(ignoredFields)
        .isEqualTo(expected);
  }
}
