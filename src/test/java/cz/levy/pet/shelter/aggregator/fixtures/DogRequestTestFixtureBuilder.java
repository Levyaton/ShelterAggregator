package cz.levy.pet.shelter.aggregator.fixtures;

import cz.levy.pet.shelter.aggregator.api.DogRequest;
import cz.levy.pet.shelter.aggregator.domain.Sex;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(setterPrefix = "with")
public class DogRequestTestFixtureBuilder {
  @Builder.Default private Long shelterId = 1L;
  @Builder.Default private String externalId = "some external id";
  @Builder.Default private String shelterUrl = "some url";
  @Builder.Default private String name = "some name";
  @Builder.Default private String description = "some description";
  @Builder.Default private String breedGuess = null;
  @Builder.Default private Sex sex = Sex.MALE;
  @Builder.Default private Float estimatedAgeInYears = 13F;
  @Builder.Default private Float currentWeight = 10F;
  @Builder.Default private Float estimatedFinalWeightMin = 10F;
  @Builder.Default private Float estimatedFinalWeightMax = 10F;
  @Builder.Default private String dogAddress = null;
  @Builder.Default private List<byte[]> images = Collections.emptyList();

  public DogRequest toDogRequest() {
    return new DogRequest(
        shelterId,
        externalId,
        shelterUrl,
        name,
        description,
        breedGuess,
        sex,
        estimatedAgeInYears,
        currentWeight,
        estimatedFinalWeightMin,
        estimatedFinalWeightMax,
        dogAddress,
        images);
  }
}
