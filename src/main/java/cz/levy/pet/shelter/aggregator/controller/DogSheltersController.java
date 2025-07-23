package cz.levy.pet.shelter.aggregator.controller;

import static cz.levy.pet.shelter.aggregator.mapper.DogMapper.requestToDto;

import cz.levy.pet.shelter.aggregator.api.DogRequest;
import cz.levy.pet.shelter.aggregator.api.DogResponse;
import cz.levy.pet.shelter.aggregator.mapper.DogMapper;
import cz.levy.pet.shelter.aggregator.service.DogSheltersService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dogs")
public class DogSheltersController {

  private final DogSheltersService dogSheltersService;

  public DogSheltersController(DogSheltersService dogSheltersService) {
    this.dogSheltersService = dogSheltersService;
  }

  @PostMapping()
  public ResponseEntity<Long> createDog(@Valid @RequestBody DogRequest dog) {
    var dogEntity = dogSheltersService.saveDog(requestToDto(dog));
    return ResponseEntity.status(HttpStatus.CREATED).body(dogEntity.getId());
  }

  @PutMapping("/{internalId}")
  public ResponseEntity<Void> updateDog(
      @PathVariable long internalId, @Valid @RequestBody DogRequest dog) {
    dogSheltersService.updateDog(internalId, requestToDto(dog));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{internalId}")
  public ResponseEntity<Void> deleteDog(@PathVariable long internalId) {
    dogSheltersService.deleteDog(internalId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{internalId}")
  public ResponseEntity<DogResponse> getOneDog(@PathVariable long internalId) {
    var dogDto = dogSheltersService.getDogDto(internalId);
    return ResponseEntity.ok(DogMapper.dtoToResponse(dogDto, internalId));
  }

  @GetMapping()
  public ResponseEntity<List<DogResponse>> getAllDogs(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
    var dogResponses = dogSheltersService.getAllDogs(PageRequest.of(page, size, Sort.by("id")));
    return ResponseEntity.ok(dogResponses.stream().toList());
  }
}
