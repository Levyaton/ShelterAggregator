package cz.levy.pet.shelter.aggregator.service;

import cz.levy.pet.shelter.aggregator.dto.DogDto;
import cz.levy.pet.shelter.aggregator.entity.DogEntity;
import cz.levy.pet.shelter.aggregator.entity.ShelterEntity;
import cz.levy.pet.shelter.aggregator.error.RestErrorHandler;
import cz.levy.pet.shelter.aggregator.mapper.DogMapper;
import cz.levy.pet.shelter.aggregator.repository.DogRepository;
import cz.levy.pet.shelter.aggregator.repository.ShelterRepository;
import java.util.NoSuchElementException;
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
   return dogRepository.findById(internalId).orElseThrow(() -> new NoSuchElementException("Dog not found with id: " + internalId));
  }

  private ShelterEntity getShelterById(long shelterId) {
    return shelterRepository
        .findById(shelterId)
            .orElseThrow(() -> new NoSuchElementException("Shelter not found with id: " + shelterId));
  }
}
