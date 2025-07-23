package cz.levy.pet.shelter.aggregator.dto;

import cz.levy.pet.shelter.aggregator.domain.Sex;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class DogDto {
  Long shelterId;
  String externalId;
  String shelterUrl;
  String name;
  String description;
  String breedGuess;
  Sex sex;
  Float estimatedAgeInYears;
  Float currentWeight;
  Float estimatedFinalWeightMin;
  Float estimatedFinalWeightMax;
  String dogAddress;
  List<String> imageUrls;
}
