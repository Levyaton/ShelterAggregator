package cz.levy.pet.shelter.aggregator.fixtures.builders;

import cz.levy.pet.shelter.aggregator.api.DogRequest;
import cz.levy.pet.shelter.aggregator.api.DogResponse;
import cz.levy.pet.shelter.aggregator.fixtures.CommonFixtures;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(setterPrefix = "with")
public class DogResponseTestFixtureBuilder {
  @Builder.Default private long internalId = CommonFixtures.TEST_ID;

  @Builder.Default
  private DogRequest dogRequest =
      DogRequestTestFixtureBuilder.builder().build().toDogRequest(CommonFixtures.TEST_ID);

  public DogResponse toDogResponse() {
    return new DogResponse(internalId, dogRequest);
  }
}
