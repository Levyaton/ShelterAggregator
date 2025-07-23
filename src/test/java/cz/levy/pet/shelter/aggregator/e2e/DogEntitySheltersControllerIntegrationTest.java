package cz.levy.pet.shelter.aggregator.e2e;

import static cz.levy.pet.shelter.aggregator.utils.ResponseExtensions.performGetRequest;
import static cz.levy.pet.shelter.aggregator.utils.ResponseExtensions.performRequest;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.levy.pet.shelter.aggregator.api.DogResponse;
import cz.levy.pet.shelter.aggregator.config.TestContainerConfig;
import cz.levy.pet.shelter.aggregator.entity.DogEntity;
import cz.levy.pet.shelter.aggregator.entity.ShelterEntity;
import cz.levy.pet.shelter.aggregator.error.RestErrorHandler;
import cz.levy.pet.shelter.aggregator.fixtures.CommonFixtures;
import cz.levy.pet.shelter.aggregator.fixtures.builders.DogEntityTestFixtureBuilder;
import cz.levy.pet.shelter.aggregator.fixtures.builders.DogRequestTestFixtureBuilder;
import cz.levy.pet.shelter.aggregator.fixtures.builders.DogResponseTestFixtureBuilder;
import cz.levy.pet.shelter.aggregator.fixtures.builders.ShelterEntityTestFixtureBuilder;
import cz.levy.pet.shelter.aggregator.repository.DogRepository;
import cz.levy.pet.shelter.aggregator.repository.ShelterRepository;
import cz.levy.pet.shelter.aggregator.utils.ResponseExtensions;
import io.restassured.http.Method;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.util.List;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfig.class)
@AutoConfigureMockMvc
@ExtensionMethod(ResponseExtensions.class)
@ActiveProfiles("test")
public class DogEntitySheltersControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ShelterRepository shelterRepository;

  @Autowired private DogRepository dogRepository;

  @BeforeEach
  void setup() {
    RestAssuredMockMvc.mockMvc(mockMvc);
    dogRepository.deleteAll();
    shelterRepository.deleteAll();
  }

  @Test
  public void createDogCreatesANewEntityInDogRepositoryAndReturnsStatusCode201() {
    var savedShelter = prepareSavedShelterEntity();

    var validRequest =
        DogRequestTestFixtureBuilder.builder().build().toDogRequest(savedShelter.getId());

    performRequest(validRequest, HttpStatus.CREATED, Method.POST, "/dogs");

    assertThat(dogRepository.findAll().size()).isEqualTo(1);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidCreateDogCases")
  public void createDogWithInvalidInputReturnsAppropriateError(InvalidCreateDogCase caseData) {
    ShelterEntity savedShelter = null;
    if (caseData.useSavedShelter) {
      savedShelter = prepareSavedShelterEntity();
    }

    if (caseData.useSavedDog) {
      prepareSavedDogEntity(savedShelter);
    }

    var shelterId = savedShelter != null ? savedShelter.getId() : CommonFixtures.TEST_ID;
    var invalidDogRequest = caseData.dogRequestBuilder.toDogRequest(shelterId);
    var expectedErrorMessage =
        caseData
            .expectedErrorMessage
            .replace("{externalId}", String.valueOf(invalidDogRequest.getExternalId()))
            .replace("{shelterId}", String.valueOf(shelterId));
    performRequest(invalidDogRequest, caseData.expectedStatus, Method.POST, "/dogs")
        .assertThatResponseEqualsRecursive(
            new RestErrorHandler.ErrorResponse(
                caseData.expectedStatus.name(), expectedErrorMessage));
  }

  @Test
  public void updateDogUpdatesADogEntityInDogRepositoryAndReturnsStatusCode200() {
    var savedShelter = prepareSavedShelterEntity();
    var savedDog = prepareSavedDogEntity(savedShelter);
    var validRequest =
        DogRequestTestFixtureBuilder.builder().build().toDogRequest(savedShelter.getId());

    performRequest(
        validRequest, HttpStatus.NO_CONTENT, Method.PUT, "/dogs/{internalId}", savedDog.getId());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidUpdateDogCases")
  public void updateDogWithInvalidInputReturnsAppropriateError(InvalidCreateDogCase caseData) {
    ShelterEntity savedShelter = prepareSavedShelterEntity();
    DogEntity savedDog = null;

    if (caseData.useSavedDog) {
      savedDog = prepareSavedDogEntity(savedShelter);
    }

    var shelterId = savedShelter.getId();
    var internalDogId = savedDog != null ? savedDog.getId() : CommonFixtures.TEST_ID;

    var expectedErrorMessage =
        caseData
            .expectedErrorMessage
            .replace("{internalId}", String.valueOf(internalDogId))
            .replace("{shelterId}", String.valueOf(shelterId));

    performRequest(
            caseData.dogRequestBuilder.toDogRequest(shelterId),
            caseData.expectedStatus,
            Method.PUT,
            "/dogs/{internalId}",
            internalDogId)
        .assertThatResponseEqualsRecursive(
            new RestErrorHandler.ErrorResponse(
                caseData.expectedStatus.name(), expectedErrorMessage));
  }

  @Test
  public void deleteDogDeletesADogEntityFromDogRepositoryAndReturnsStatusCode200() {
    var savedShelter = prepareSavedShelterEntity();
    var savedDog = prepareSavedDogEntity(savedShelter);

    performRequest(
        null, HttpStatus.NO_CONTENT, Method.DELETE, "/dogs/{internalId}", savedDog.getId());

    assertThat(dogRepository.findById(savedDog.getId())).isEmpty();
  }

  @Test
  public void getOneDogReturnsADogWithTheSpecifiedIdWithStatusCode200() {
    var savedShelter = prepareSavedShelterEntity();
    var savedDog = prepareSavedDogEntity(savedShelter);

    DogResponse expectedResponse =
        DogResponseTestFixtureBuilder.builder()
            .withInternalId(savedDog.getId())
            .withDogRequest(
                DogRequestTestFixtureBuilder.builder().build().toDogRequest(savedShelter.getId()))
            .build()
            .toDogResponse();

    performGetRequest(HttpStatus.OK, "/dogs/{internalId}", savedDog.getId())
        .assertThatResponseEqualsRecursive(expectedResponse);
  }

  @Test
  public void getAllDogsReturnsAllDogsWithStatusCode200() {
    var savedShelter = prepareSavedShelterEntity();
    var savedDog1 = prepareSavedDogEntity(savedShelter);
    var savedDog2 =
        prepareSavedDogEntity(
            savedShelter,
            DogEntityTestFixtureBuilder.builder()
                .withExternalId("some other external id")
                .build()
                .toDogEntity(savedShelter));

    var expectedResponse =
        List.of(
            DogResponseTestFixtureBuilder.builder()
                .withInternalId(savedDog1.getId())
                .withDogRequest(
                    DogRequestTestFixtureBuilder.builder()
                        .build()
                        .toDogRequest(savedShelter.getId()))
                .build()
                .toDogResponse(),
            DogResponseTestFixtureBuilder.builder()
                .withInternalId(savedDog2.getId())
                .withDogRequest(
                    DogRequestTestFixtureBuilder.builder()
                        .withExternalId("some other external id")
                        .build()
                        .toDogRequest(savedShelter.getId()))
                .build()
                .toDogResponse());

    performRequest(null, HttpStatus.OK, Method.GET, "/dogs")
        .assertThatResponseEqualsRecursive(expectedResponse);
  }

  private ShelterEntity prepareSavedShelterEntity() {
    var shelter = ShelterEntityTestFixtureBuilder.builder().build().toShelterEntity();
    return shelterRepository.save(shelter);
  }

  private DogEntity prepareSavedDogEntity(ShelterEntity savedShelter) {
    return prepareSavedDogEntity(
        savedShelter, DogEntityTestFixtureBuilder.builder().build().toDogEntity(savedShelter));
  }

  private DogEntity prepareSavedDogEntity(ShelterEntity savedShelter, DogEntity dogEntity) {
    return dogRepository.save(dogEntity);
  }

  private static Stream<InvalidCreateDogCase> invalidCreateDogCases() {
    return Stream.of(
        InvalidCreateDogCase.builder()
            .caseName("Invalid request body")
            .dogRequestBuilder(
                DogRequestTestFixtureBuilder.builder().withCurrentWeight(-10F).build())
            .expectedErrorMessage(
                "Invalid request parameters: dog.currentWeight must be greater than 0")
            .expectedStatus(HttpStatus.BAD_REQUEST)
            .useSavedDog(false)
            .build(),
        InvalidCreateDogCase.builder()
            .caseName("Unknown shelter id")
            .dogRequestBuilder(DogRequestTestFixtureBuilder.builder().build())
            .expectedStatus(HttpStatus.NOT_FOUND)
            .expectedErrorMessage("Shelter not found with id: {shelterId}")
            .useSavedShelter(false)
            .useSavedDog(false)
            .build(),
        InvalidCreateDogCase.builder()
            .caseName("Duplicate dog entered")
            .dogRequestBuilder(DogRequestTestFixtureBuilder.builder().build())
            .expectedStatus(HttpStatus.CONFLICT)
            .expectedErrorMessage(
                "Dog already exists with externalId: {externalId} and shelterId: {shelterId}")
            .useSavedDog(true)
            .build());
  }

  private static Stream<InvalidCreateDogCase> invalidUpdateDogCases() {
    return Stream.of(
        InvalidCreateDogCase.builder()
            .caseName("Invalid request body")
            .dogRequestBuilder(DogRequestTestFixtureBuilder.builder().withSex(null).build())
            .expectedErrorMessage("Invalid request parameters: dog.sex must not be null")
            .expectedStatus(HttpStatus.BAD_REQUEST)
            .useSavedDog(true)
            .build(),
        InvalidCreateDogCase.builder()
            .caseName("Dog Not Found")
            .dogRequestBuilder(DogRequestTestFixtureBuilder.builder().build())
            .expectedStatus(HttpStatus.NOT_FOUND)
            .expectedErrorMessage("Dog not found with id: {internalId}")
            .useSavedDog(false)
            .build());
  }

  @Data
  @Builder
  public static class InvalidCreateDogCase {
    private final String caseName;
    private final DogRequestTestFixtureBuilder dogRequestBuilder;
    private final HttpStatus expectedStatus;
    private final String expectedErrorMessage;
    @Builder.Default private final boolean useSavedShelter = true;
    private final boolean useSavedDog;

    @Override
    public String toString() {
      return caseName;
    }
  }
}
