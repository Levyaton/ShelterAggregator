package cz.levy.pet.shelter.aggregator.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.levy.pet.shelter.aggregator.fixtures.CommonFixtures;
import cz.levy.pet.shelter.aggregator.fixtures.DogRequestTestFixtureBuilder;
import cz.levy.pet.shelter.aggregator.fixtures.DogResponseFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DogEntitySheltersControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  public void createDogCreatesANewEntityInDogRepositoryAndReturnsStatusCode200() throws Exception {
    mockMvc
        .perform(
            post("/dogs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        DogRequestTestFixtureBuilder.builder().build().toDogRequest())))
        .andExpect(status().isCreated())
        .andExpect(content().json(CommonFixtures.TEST_ID_STRING));
  }

  @Test
  public void createDogWithInvalidInputReturns400() throws Exception {
    mockMvc
        .perform(
            post("/dogs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        DogRequestTestFixtureBuilder.builder()
                            .withCurrentWeight(-10f)
                            .build()
                            .toDogRequest())))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void updateDogWithInvalidInputReturns400() throws Exception {
    mockMvc
        .perform(
            put("/dogs/" + CommonFixtures.TEST_ID_STRING)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        DogRequestTestFixtureBuilder.builder()
                            .withCurrentWeight(-10f)
                            .build()
                            .toDogRequest())))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void updateDogUpdatesADogEntityInDogRepositoryAndReturnsStatusCode200() throws Exception {
    mockMvc
        .perform(
            put("/dogs/" + CommonFixtures.TEST_ID_STRING)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        DogRequestTestFixtureBuilder.builder().build().toDogRequest())))
        .andExpect(status().isNoContent());
  }

  @Test
  public void deleteDogDeletesADogEntityFromDogRepositoryAndReturnsStatusCode200()
      throws Exception {
    mockMvc
        .perform(
            delete("/dogs/" + CommonFixtures.TEST_ID_STRING)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  public void getOneDogReturnsADogWithTheSpecifiedIdWithStatusCode200() throws Exception {
    mockMvc
        .perform(
            get("/dogs/" + CommonFixtures.TEST_ID_STRING).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    objectMapper.writeValueAsString(DogResponseFixtures.TEST_DOG_RESPONSE_RECORD)));
  }

  @Test
  public void getAllDogsReturnsAllDogsWithStatusCode200() throws Exception {
    mockMvc
        .perform(get("/dogs").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    objectMapper.writeValueAsString(
                        List.of(DogResponseFixtures.TEST_DOG_RESPONSE_RECORD))));
  }
}
