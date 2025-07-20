package cz.levy.pet.shelter.aggregator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.levy.pet.shelter.aggregator.api.DogRequest;
import cz.levy.pet.shelter.aggregator.api.DogResponse;
import cz.levy.pet.shelter.aggregator.domain.Sex;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dogs")
public class DogSheltersController {
  private static final DogResponse stubbedResponse =
      new DogResponse(
          0,
          new DogRequest(
              1L,
              "some external id",
              "some url",
              "some name",
              "some description",
              null,
              Sex.MALE,
              13F,
              10F,
              10F,
              10F,
              null,
              Collections.emptyList()));

  @PostMapping()
  public ResponseEntity<Long> createDog(@Valid @RequestBody DogRequest dog) {
    return ResponseEntity.status(HttpStatus.CREATED).body(stubbedResponse.internalId());
  }

  @PutMapping("/{internalId}")
  public ResponseEntity<Void> updateDog(
      @PathVariable long internalId, @Valid @RequestBody DogRequest dog) {
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{internalId}")
  public ResponseEntity<String> deleteDog(@PathVariable long internalId) {
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{internalId}")
  public ResponseEntity<DogResponse> getOneDog(@PathVariable long internalId)
      throws JsonProcessingException {
    return ResponseEntity.ok(stubbedResponse);
  }

  @GetMapping()
  public ResponseEntity<List<DogResponse>> getAllDogs() throws JsonProcessingException {
    return ResponseEntity.ok(List.of(stubbedResponse));
  }
}
