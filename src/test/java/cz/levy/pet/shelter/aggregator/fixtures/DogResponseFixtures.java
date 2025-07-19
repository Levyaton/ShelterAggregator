package cz.levy.pet.shelter.aggregator.fixtures;

import cz.levy.pet.shelter.aggregator.api.DogResponse;

public class DogResponseFixtures {
  public static final DogResponse TEST_DOG_RESPONSE_RECORD =
      new DogResponse(CommonFixtures.TEST_ID, DogRequestFixtures.TEST_DOG_REQUEST);
}
