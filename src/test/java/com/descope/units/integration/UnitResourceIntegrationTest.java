package com.descope.units.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for UnitResource using Testcontainers with LocalStack.
 *
 * <p>These tests verify the complete flow from REST API through service layer to DynamoDB.
 */
@QuarkusTest
@QuarkusTestResource(LocalStackDynamoDbResource.class)
class UnitResourceIntegrationTest {

  private static final String BASE_PATH = "/api/units";
  private static final String TEST_NAME = "Integration Test Unit";

  @BeforeEach
  void setUp() {
    // Clean up any existing test data if needed
  }

  @Test
  @DisplayName("createUnit - valid request - should create unit in DynamoDB and return 201")
  void createUnit_validRequest_shouldCreateUnitInDynamoDB() {
    // When/Then
    String unitId =
        given()
            .contentType("application/json")
            .body("{\"name\":\"" + TEST_NAME + "\"}")
            .when()
            .post(BASE_PATH)
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo(TEST_NAME))
            .extract()
            .path("id");

    // Verify we can retrieve the created unit
    given()
        .pathParam("id", unitId)
        .when()
        .get(BASE_PATH + "/{id}")
        .then()
        .statusCode(200)
        .body("id", equalTo(unitId))
        .body("name", equalTo(TEST_NAME));
  }

  @Test
  @DisplayName("getUnit - existing unit - should retrieve unit from DynamoDB")
  void getUnit_existingUnit_shouldRetrieveFromDynamoDB() {
    // Given - Create a unit first
    String unitId =
        given()
            .contentType("application/json")
            .body("{\"name\":\"" + TEST_NAME + "\"}")
            .when()
            .post(BASE_PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    // When/Then - Retrieve the unit
    given()
        .pathParam("id", unitId)
        .when()
        .get(BASE_PATH + "/{id}")
        .then()
        .statusCode(200)
        .body("id", equalTo(unitId))
        .body("name", equalTo(TEST_NAME));
  }

  @Test
  @DisplayName("getUnit - non-existing unit - should return 404")
  void getUnit_nonExistingUnit_shouldReturn404() {
    // Given
    String nonExistingId = "01933b5e-7f00-7000-8000-999999999999";

    // When/Then
    given()
        .pathParam("id", nonExistingId)
        .when()
        .get(BASE_PATH + "/{id}")
        .then()
        .statusCode(404)
        .body("message", notNullValue())
        .body("status", equalTo(404));
  }

  @Test
  @DisplayName("updateUnit - existing unit - should update unit in DynamoDB")
  void updateUnit_existingUnit_shouldUpdateInDynamoDB() {
    // Given - Create a unit first
    String unitId =
        given()
            .contentType("application/json")
            .body("{\"name\":\"" + TEST_NAME + "\"}")
            .when()
            .post(BASE_PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    // When - Update the unit
    String updatedName = "Updated Integration Test Unit";
    given()
        .contentType("application/json")
        .pathParam("id", unitId)
        .body("{\"name\":\"" + updatedName + "\"}")
        .when()
        .put(BASE_PATH + "/{id}")
        .then()
        .statusCode(200)
        .body("id", equalTo(unitId))
        .body("name", equalTo(updatedName));

    // Then - Verify the update persisted
    given()
        .pathParam("id", unitId)
        .when()
        .get(BASE_PATH + "/{id}")
        .then()
        .statusCode(200)
        .body("name", equalTo(updatedName));
  }

  @Test
  @DisplayName("updateUnit - non-existing unit - should return 404")
  void updateUnit_nonExistingUnit_shouldReturn404() {
    // Given
    String nonExistingId = "01933b5e-7f00-7000-8000-999999999999";

    // When/Then
    given()
        .contentType("application/json")
        .pathParam("id", nonExistingId)
        .body("{\"name\":\"Updated Name\"}")
        .when()
        .put(BASE_PATH + "/{id}")
        .then()
        .statusCode(404)
        .body("message", notNullValue())
        .body("status", equalTo(404));
  }

  @Test
  @DisplayName("deleteUnit - existing unit - should delete unit from DynamoDB")
  void deleteUnit_existingUnit_shouldDeleteFromDynamoDB() {
    // Given - Create a unit first
    String unitId =
        given()
            .contentType("application/json")
            .body("{\"name\":\"" + TEST_NAME + "\"}")
            .when()
            .post(BASE_PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    // When - Delete the unit
    given().pathParam("id", unitId).when().delete(BASE_PATH + "/{id}").then().statusCode(204);

    // Then - Verify the unit no longer exists
    given().pathParam("id", unitId).when().get(BASE_PATH + "/{id}").then().statusCode(404);
  }

  @Test
  @DisplayName("deleteUnit - non-existing unit - should return 404")
  void deleteUnit_nonExistingUnit_shouldReturn404() {
    // Given
    String nonExistingId = "01933b5e-7f00-7000-8000-999999999999";

    // When/Then
    given()
        .pathParam("id", nonExistingId)
        .when()
        .delete(BASE_PATH + "/{id}")
        .then()
        .statusCode(404)
        .body("message", notNullValue())
        .body("status", equalTo(404));
  }

  @Test
  @DisplayName("complete CRUD flow - should work end-to-end")
  void completeCrudFlow_shouldWorkEndToEnd() {
    // Create
    String unitId =
        given()
            .contentType("application/json")
            .body("{\"name\":\"CRUD Test Unit\"}")
            .when()
            .post(BASE_PATH)
            .then()
            .statusCode(201)
            .body("name", equalTo("CRUD Test Unit"))
            .extract()
            .path("id");

    // Read
    given()
        .pathParam("id", unitId)
        .when()
        .get(BASE_PATH + "/{id}")
        .then()
        .statusCode(200)
        .body("name", equalTo("CRUD Test Unit"));

    // Update
    given()
        .contentType("application/json")
        .pathParam("id", unitId)
        .body("{\"name\":\"Updated CRUD Test Unit\"}")
        .when()
        .put(BASE_PATH + "/{id}")
        .then()
        .statusCode(200)
        .body("name", equalTo("Updated CRUD Test Unit"));

    // Delete
    given().pathParam("id", unitId).when().delete(BASE_PATH + "/{id}").then().statusCode(204);

    // Verify deletion
    given().pathParam("id", unitId).when().get(BASE_PATH + "/{id}").then().statusCode(404);
  }
}
