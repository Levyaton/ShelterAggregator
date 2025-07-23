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
import java.util.NoSuchElementException;
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

  public Page<DogResponse> getAllDogs(
      Pageable pageable, Float ageMin, Float ageMax, Sex sex, DogSize size) {
    Page<DogEntity> dogEntities = paginateAndFilterDogs(pageable, ageMin, ageMax, sex, size);

    return dogEntities.map(
        (dogEntity) -> {
          var dogDto = DogMapper.entityToDto(dogEntity);
          return DogMapper.dtoToResponse(dogDto, dogEntity.getId());
        });
  }

  Page<DogEntity> paginateAndFilterDogs(
      Pageable pageable, Float ageMin, Float ageMax, Sex sex, DogSize size) {
    var spec = buildDogSpecification(ageMin, ageMax, sex, size);
    return dogRepository.findAll(spec, pageable);
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
}
