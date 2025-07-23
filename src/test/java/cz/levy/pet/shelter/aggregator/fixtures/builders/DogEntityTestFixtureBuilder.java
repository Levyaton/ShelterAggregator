package cz.levy.pet.shelter.aggregator.fixtures.builders;

import cz.levy.pet.shelter.aggregator.domain.Sex;
import cz.levy.pet.shelter.aggregator.entity.DogEntity;
import cz.levy.pet.shelter.aggregator.entity.ShelterEntity;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(setterPrefix = "with")
public class DogEntityTestFixtureBuilder {
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
  @Builder.Default private List<String> imageUrls = List.of("some-image.jpg");

  public DogEntity toDogEntity(ShelterEntity shelter) {
    return DogEntity.builder()
        .externalId(externalId)
        .shelterUrl(shelterUrl)
        .name(name)
        .description(description)
        .breedGuess(breedGuess)
        .sex(sex)
        .estimatedAgeInYears(estimatedAgeInYears)
        .currentWeight(currentWeight)
        .estimatedFinalWeightMin(estimatedFinalWeightMin)
        .estimatedFinalWeightMax(estimatedFinalWeightMax)
        .dogAddress(dogAddress)
        .shelter(shelter)
        .imageUrls(imageUrls)
        .build();
  }
}
