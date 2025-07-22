package cz.levy.pet.shelter.aggregator.e2e;

import static cz.levy.pet.shelter.aggregator.utils.ResponseExtensions.performRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.levy.pet.shelter.aggregator.error.RestErrorHandler;
import cz.levy.pet.shelter.aggregator.fixtures.CommonFixtures;
import cz.levy.pet.shelter.aggregator.fixtures.DogRequestTestFixtureBuilder;
import cz.levy.pet.shelter.aggregator.fixtures.DogResponseFixtures;
import cz.levy.pet.shelter.aggregator.utils.ResponseExtensions;
import io.restassured.http.Method;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.util.List;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ExtensionMethod(ResponseExtensions.class)
@ActiveProfiles("test")
public class DogEntitySheltersControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void setup() {
    RestAssuredMockMvc.mockMvc(mockMvc);
  }

  @Test
  public void createDogCreatesANewEntityInDogRepositoryAndReturnsStatusCode200() {
    var validRequest = DogRequestTestFixtureBuilder.builder().build().toDogRequest();

    performRequest(validRequest, HttpStatus.CREATED, Method.POST, "/dogs")
        .assertThatResponseEqualsRecursive(CommonFixtures.TEST_ID_STRING);
  }

  @Test
  public void createDogWithInvalidInputReturns400() {
    var badRequest =
        DogRequestTestFixtureBuilder.builder().withCurrentWeight(-10f).build().toDogRequest();

    performRequest(badRequest, HttpStatus.BAD_REQUEST, Method.POST, "/dogs")
        .assertThatResponseEqualsRecursive(
            new RestErrorHandler.ErrorResponse(
                HttpStatus.BAD_REQUEST.name(),
                "Invalid request parameters: dog.currentWeight must be greater than 0"));
  }

  @Test
  public void updateDogWithInvalidInputReturns400() {
    var badRequest =
        DogRequestTestFixtureBuilder.builder().withSex(null).build().toDogRequest();

    performRequest(
        badRequest,
        HttpStatus.BAD_REQUEST,
        Method.PUT,
        "/dogs/{internalId}",
        CommonFixtures.TEST_ID_STRING).assertThatResponseEqualsRecursive(
            new RestErrorHandler.ErrorResponse(
                    HttpStatus.BAD_REQUEST.name(),
                    "Invalid request parameters: dog.sex must not be null"));
  }

  @Test
  public void updateDogUpdatesADogEntityInDogRepositoryAndReturnsStatusCode200() {
    var validRequest = DogRequestTestFixtureBuilder.builder().build().toDogRequest();

    performRequest(
        validRequest,
        HttpStatus.NO_CONTENT,
        Method.PUT,
        "/dogs/{internalId}",
        CommonFixtures.TEST_ID_STRING);
  }

  @Test
  public void deleteDogDeletesADogEntityFromDogRepositoryAndReturnsStatusCode200() {
    performRequest(
        null,
        HttpStatus.NO_CONTENT,
        Method.DELETE,
        "/dogs/{internalId}",
        CommonFixtures.TEST_ID_STRING);
  }

  @Test
  public void getOneDogReturnsADogWithTheSpecifiedIdWithStatusCode200() {
    performRequest(
            null, HttpStatus.OK, Method.GET, "/dogs/{internalId}", CommonFixtures.TEST_ID_STRING)
        .assertThatResponseEqualsRecursive(DogResponseFixtures.TEST_DOG_RESPONSE_RECORD);
  }

  @Test
  public void getAllDogsReturnsAllDogsWithStatusCode200() {
    performRequest(null, HttpStatus.OK, Method.GET, "/dogs")
        .assertThatResponseEqualsRecursive(List.of(DogResponseFixtures.TEST_DOG_RESPONSE_RECORD));
  }
}
