package cz.levy.pet.shelter.aggregator.fixtures.builders;

import cz.levy.pet.shelter.aggregator.entity.DogEntity;
import cz.levy.pet.shelter.aggregator.entity.ShelterEntity;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(setterPrefix = "with")
public class ShelterEntityTestFixtureBuilder {
  @Builder.Default private String name = "some shelter name";
  @Builder.Default private String address = "some address";
  @Builder.Default private String phoneNumber = "123456789";
  @Builder.Default private String url = "some url";
  @Builder.Default private String email = "some email";
  @Builder.Default private boolean isNonProfit = true;
  @Builder.Default private String bankAccountNumber = "1234567890/1234";
  @Builder.Default private Set<DogEntity> dogs = Set.of();

  public ShelterEntity toShelterEntity() {
    return ShelterEntity.builder()
        .name(name)
        .address(address)
        .phoneNumber(phoneNumber)
        .url(url)
        .email(email)
        .isNonProfit(isNonProfit)
        .bankAccountNumber(bankAccountNumber)
        .dogs(dogs != null ? Set.copyOf(dogs) : Set.of())
        .build();
  }
}
