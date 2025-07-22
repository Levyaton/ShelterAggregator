package cz.levy.pet.shelter.aggregator.utils;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.Method;
import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class ResponseExtensions {
  public static <T> ValidatableMockMvcResponse performRequest(
      T requestBody, HttpStatus expectedStatus, Method method, String url, Object... pathParams) {

    var requestSpec = given().contentType(MediaType.APPLICATION_JSON_VALUE);

    if (requestBody != null) {
      requestSpec = requestSpec.body(requestBody);
    }

    return requestSpec
        .when()
        .request(method, url, pathParams)
        .then()
        .statusCode(expectedStatus.value());
  }

  public static <T> ValidatableMockMvcResponse assertThatResponseEqualsRecursive(
      ValidatableMockMvcResponse resp, Object expected, String... ignoredFields) {

    @SuppressWarnings("unchecked")
    var response = (T) resp.extract().as(expected.getClass());

    TestUtils.assertThatEqualsRecursive(response, expected, ignoredFields);
    return resp;
  }

  @SuppressWarnings("unchecked")
  public static <T> ValidatableMockMvcResponse assertThatResponseEqualsRecursive(
      ValidatableMockMvcResponse resp, List<T> expected, String... ignoredFields) {
    var response = resp.extract().as(new TypeRef<List<T>>() {});
    if (!expected.isEmpty()) {
      var typeClass = expected.getFirst().getClass();
      response = (List<T>) resp.extract().jsonPath().getList(".", typeClass);
    }
    TestUtils.assertThatEqualsRecursive(response, expected, ignoredFields);
    return resp;
  }
}
