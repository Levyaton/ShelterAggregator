package cz.levy.pet.shelter.aggregator.fixtures;

import cz.levy.pet.shelter.aggregator.api.DogRequest;
import cz.levy.pet.shelter.aggregator.domain.Sex;
import java.util.Collections;

public class DogRequestFixtures {
  public static final DogRequest TEST_DOG_REQUEST =
      new DogRequest(
          "some external id",
          "some url",
          "some name",
          "some description",
          null,
          Sex.MALE,
          13F,
          10F,
          10F,
          10F,
          null,
          Collections.emptyList());
}
