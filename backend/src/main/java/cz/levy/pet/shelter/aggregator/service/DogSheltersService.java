package cz.levy.pet.shelter.aggregator.service;

import cz.levy.pet.shelter.aggregator.api.DogResponse;
import cz.levy.pet.shelter.aggregator.domain.DogSize;
import cz.levy.pet.shelter.aggregator.domain.Sex;
import cz.levy.pet.shelter.aggregator.dto.DogDto;
import cz.levy.pet.shelter.aggregator.entity.DogEntity;
import cz.levy.pet.shelter.aggregator.entity.ShelterEntity;
import cz.levy.pet.shelter.aggregator.error.RestErrorHandler;
import cz.levy.pet.shelter.aggregator.mapper.DogMapper;
import cz.levy.pet.shelter.aggregator.repository.DogRepository;
import cz.levy.pet.shelter.aggregator.repository.ShelterRepository;
import cz.levy.pet.shelter.aggregator.spec.DogSpec;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class DogSheltersService {
  private final DogRepository dogRepository;
  private final ShelterRepository shelterRepository;

  public DogSheltersService(DogRepository dogRepository, ShelterRepository shelterRepository) {
    this.dogRepository = dogRepository;
    this.shelterRepository = shelterRepository;
  }

  private boolean dogExists(String externalId, long shelterId) {
    return dogRepository.existsByExternalIdAndShelterId(externalId, shelterId);
  }

  public DogEntity saveDog(DogDto dogDto) {
    assert dogDto != null : "Request body cannot be null";
    validateDogDoesNotExist(dogDto);
    ShelterEntity shelter = getShelterById(dogDto.getShelterId());
    DogEntity dogEntity = DogMapper.dtoToEntity(dogDto, shelter);
    return dogRepository.save(dogEntity);
  }

  public void updateDog(long internalId, DogDto dogDto) {
    assert dogDto != null : "Request body cannot be null";

    var dogEntity = getDogByInternalId(internalId);
    var updatedDogEntity = DogMapper.dtoToEntity(dogDto, dogEntity.getShelter());
    updatedDogEntity.setId(internalId);
    dogRepository.save(updatedDogEntity);
  }

  public void deleteDog(long internalId) {
    DogEntity dogEntity = getDogByInternalId(internalId);
    dogRepository.delete(dogEntity);
  }

  public DogDto getDogDto(long internalId) {
    DogEntity dogEntity = getDogByInternalId(internalId);
    return DogMapper.entityToDto(dogEntity);
  }

  public List<DogResponse> getAllDogs(
      Pageable pageable, Float ageMin, Float ageMax, Sex sex, DogSize size) {
    Page<DogEntity> dogEntities = paginateAndFilterDogs(pageable, ageMin, ageMax, sex, size);

    return dogEntitiesToResponses(dogEntities.toList());
  }

  Page<DogEntity> paginateAndFilterDogs(
      Pageable pageable, Float ageMin, Float ageMax, Sex sex, DogSize size) {
    var spec = buildDogSpecification(ageMin, ageMax, sex, size);
    return dogRepository.findAll(spec, pageable);
  }

  public List<DogResponse> getRandomDogs(int listSize) {
    var randomDogs = RandomnessWeight.getRandomWeightedSelection(dogRepository.findAll(), listSize);
    return dogEntitiesToResponses(randomDogs);
  }

  private List<DogResponse> dogEntitiesToResponses(List<DogEntity> dogEntities) {
    return dogEntities.stream()
        .map(
            dogEntity -> {
              var dogDto = DogMapper.entityToDto(dogEntity);
              return DogMapper.dtoToResponse(dogDto, dogEntity.getId());
            })
        .toList();
  }

  private Specification<DogEntity> buildDogSpecification(
      Float ageMin, Float ageMax, Sex sex, DogSize size) {

    Specification<DogEntity> spec = (_, _, cb) -> cb.conjunction();
    if (ageMin != null) {
      spec = spec.and(DogSpec.ageGte(ageMin));
    }
    if (ageMax != null) {
      spec = spec.and(DogSpec.ageLte(ageMax));
    }
    if (sex != null) {
      spec = spec.and(DogSpec.hasSex(sex));
    }
    if (size != null) {
      spec = spec.and(DogSpec.hasSize(size));
    }
    return spec;
  }

  private void validateDogDoesNotExist(DogDto dogDto) {
    if (dogExists(dogDto.getExternalId(), dogDto.getShelterId())) {
      throw new RestErrorHandler.DuplicateResourceException(
          "Dog already exists with externalId: "
              + dogDto.getExternalId()
              + " and shelterId: "
              + dogDto.getShelterId());
    }
  }

  private DogEntity getDogByInternalId(long internalId) {
    return dogRepository
        .findById(internalId)
        .orElseThrow(() -> new NoSuchElementException("Dog not found with id: " + internalId));
  }

  private ShelterEntity getShelterById(long shelterId) {
    return shelterRepository
        .findById(shelterId)
        .orElseThrow(() -> new NoSuchElementException("Shelter not found with id: " + shelterId));
  }

  static class RandomnessWeight {
    private static final double MIN_WEIGHT = 0.01;
    private static final double MAX_WEIGHT = 0.02;
    private static final int MAX_POINTS = 24;

    public static List<DogEntity> getRandomWeightedSelection(List<DogEntity> dogs, int size) {
      var weightedDogs =
          dogs.stream().map(d -> Pair.create(d, computeWeight(d))).collect(Collectors.toList());
      EnumeratedDistribution<DogEntity> dist = new EnumeratedDistribution<>(weightedDogs);

      Object[] raw = dist.sample(size);

      return Arrays.stream(raw).map(o -> (DogEntity) o).toList();
    }

    private static double computeWeight(DogEntity dog) {
      int points = 0;
      if (dog.getDescription() == null) points += 5;
      if (dog.getBreedGuess() == null) points += 6;
      if (dog.getEstimatedAgeInYears() == null) points += 4;
      if (dog.getCurrentWeight() == null) points += 2;
      if (dog.getEstimatedFinalWeightMin() == null) points += 1;
      if (dog.getEstimatedFinalWeightMax() == null) points += 2;
      if (dog.getDogAddress() == null) points += 1;
      if (dog.getImageUrls() == null || dog.getImageUrls().size() <= 2) points += 3;

      double frac = points / (double) MAX_POINTS;
      return MIN_WEIGHT + frac * (MAX_WEIGHT - MIN_WEIGHT);
    }
  }
}
