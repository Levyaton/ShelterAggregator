package cz.levy.pet.shelter.aggregator.mapper;

import cz.levy.pet.shelter.aggregator.api.DogRequest;
import cz.levy.pet.shelter.aggregator.dto.DogDto;
import cz.levy.pet.shelter.aggregator.entity.DogEntity;
import cz.levy.pet.shelter.aggregator.entity.ShelterEntity;

public class DogMapper {
  public static DogDto requestToDto(DogRequest dogRequest) {
    return DogDto.builder()
        .shelterId(dogRequest.getShelterId())
        .externalId(dogRequest.getExternalId())
        .shelterUrl(dogRequest.getShelterUrl())
        .name(dogRequest.getName())
        .description(dogRequest.getDescription())
        .breedGuess(dogRequest.getBreedGuess())
        .sex(dogRequest.getSex())
        .estimatedAgeInYears(dogRequest.getEstimatedAgeInYears())
        .currentWeight(dogRequest.getCurrentWeight())
        .estimatedFinalWeightMin(dogRequest.getEstimatedFinalWeightMin())
        .estimatedFinalWeightMax(dogRequest.getEstimatedFinalWeightMax())
        .dogAddress(dogRequest.getDogAddress())
        .build();
  }

  public static DogEntity dtoToEntity(DogDto dogDto, ShelterEntity shelter) {
    return DogEntity.builder()
        .externalId(dogDto.getExternalId())
        .shelterUrl(dogDto.getShelterUrl())
        .name(dogDto.getName())
        .description(dogDto.getDescription())
        .breedGuess(dogDto.getBreedGuess())
        .sex(dogDto.getSex())
        .estimatedAgeInYears(dogDto.getEstimatedAgeInYears())
        .currentWeight(dogDto.getCurrentWeight())
        .estimatedFinalWeightMin(dogDto.getEstimatedFinalWeightMin())
        .estimatedFinalWeightMax(dogDto.getEstimatedFinalWeightMax())
        .dogAddress(dogDto.getDogAddress())
        .shelter(shelter)
        .build();
  }
}
