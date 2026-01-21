package com.descope.units.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.descope.units.exception.UnitNotFoundException;
import com.descope.units.model.Unit;
import com.descope.units.service.UnitService;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UnitResourceTest {

  @InjectMock UnitService unitService;

  private static final String TEST_ID = "01933b5e-7f00-7000-8000-000000000000";
  private static final String TEST_NAME = "Test Unit";
  private static final String BASE_PATH = "/api/units";

  @Test
  @DisplayName("createUnit - valid request - should return 201 with created unit")
  void createUnit_validRequest_shouldReturn201WithCreatedUnit() {
    // Given
    Unit unit = new Unit(TEST_ID, TEST_NAME);
    when(unitService.createUnit(TEST_NAME)).thenReturn(unit);

    // When/Then
    given()
        .contentType("application/json")
        .body("{\"name\":\"" + TEST_NAME + "\"}")
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(201)
        .body("id", equalTo(TEST_ID))
        .body("name", equalTo(TEST_NAME));

    verify(unitService).createUnit(TEST_NAME);
  }

  @Test
  @DisplayName("createUnit - empty name - should return 400 with validation error")
  void createUnit_emptyName_shouldReturn400WithValidationError() {
    // When/Then
    given()
        .contentType("application/json")
        .body("{\"name\":\"\"}")
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(400);
  }

  @Test
  @DisplayName("createUnit - missing name - should return 400 with validation error")
  void createUnit_missingName_shouldReturn400WithValidationError() {
    // When/Then
    given()
        .contentType("application/json")
        .body("{}")
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(400);
  }

  @Test
  @DisplayName("getUnit - existing unit - should return 200 with unit")
  void getUnit_existingUnit_shouldReturn200WithUnit() {
    // Given
    Unit unit = new Unit(TEST_ID, TEST_NAME);
    when(unitService.getUnitById(TEST_ID)).thenReturn(unit);

    // When/Then
    given()
        .pathParam("id", TEST_ID)
        .when()
        .get(BASE_PATH + "/{id}")
        .then()
        .statusCode(200)
        .body("id", equalTo(TEST_ID))
        .body("name", equalTo(TEST_NAME));

    verify(unitService).getUnitById(TEST_ID);
  }

  @Test
  @DisplayName("getUnit - non-existing unit - should return 404 with error")
  void getUnit_nonExistingUnit_shouldReturn404WithError() {
    // Given
    when(unitService.getUnitById(TEST_ID)).thenThrow(new UnitNotFoundException(TEST_ID));

    // When/Then
    given()
        .pathParam("id", TEST_ID)
        .when()
        .get(BASE_PATH + "/{id}")
        .then()
        .statusCode(404)
        .body("message", notNullValue())
        .body("status", equalTo(404));

    verify(unitService).getUnitById(TEST_ID);
  }

  @Test
  @DisplayName("updateUnit - existing unit with valid name - should return 200 with updated unit")
  void updateUnit_existingUnitValidName_shouldReturn200WithUpdatedUnit() {
    // Given
    String updatedName = "Updated Unit";
    Unit unit = new Unit(TEST_ID, updatedName);
    when(unitService.updateUnit(TEST_ID, updatedName)).thenReturn(unit);

    // When/Then
    given()
        .contentType("application/json")
        .pathParam("id", TEST_ID)
        .body("{\"name\":\"" + updatedName + "\"}")
        .when()
        .put(BASE_PATH + "/{id}")
        .then()
        .statusCode(200)
        .body("id", equalTo(TEST_ID))
        .body("name", equalTo(updatedName));

    verify(unitService).updateUnit(TEST_ID, updatedName);
  }

  @Test
  @DisplayName("updateUnit - non-existing unit - should return 404 with error")
  void updateUnit_nonExistingUnit_shouldReturn404WithError() {
    // Given
    String updatedName = "Updated Unit";
    when(unitService.updateUnit(TEST_ID, updatedName))
        .thenThrow(new UnitNotFoundException(TEST_ID));

    // When/Then
    given()
        .contentType("application/json")
        .pathParam("id", TEST_ID)
        .body("{\"name\":\"" + updatedName + "\"}")
        .when()
        .put(BASE_PATH + "/{id}")
        .then()
        .statusCode(404)
        .body("message", notNullValue())
        .body("status", equalTo(404));

    verify(unitService).updateUnit(TEST_ID, updatedName);
  }

  @Test
  @DisplayName("updateUnit - empty name - should return 400 with validation error")
  void updateUnit_emptyName_shouldReturn400WithValidationError() {
    // When/Then
    given()
        .contentType("application/json")
        .pathParam("id", TEST_ID)
        .body("{\"name\":\"\"}")
        .when()
        .put(BASE_PATH + "/{id}")
        .then()
        .statusCode(400);
  }

  @Test
  @DisplayName("deleteUnit - existing unit - should return 204")
  void deleteUnit_existingUnit_shouldReturn204() {
    // When/Then
    given().pathParam("id", TEST_ID).when().delete(BASE_PATH + "/{id}").then().statusCode(204);

    verify(unitService).deleteUnit(TEST_ID);
  }

  @Test
  @DisplayName("deleteUnit - non-existing unit - should return 404 with error")
  void deleteUnit_nonExistingUnit_shouldReturn404WithError() {
    // Given
    doThrow(new UnitNotFoundException(TEST_ID)).when(unitService).deleteUnit(TEST_ID);

    // When/Then
    given()
        .pathParam("id", TEST_ID)
        .when()
        .delete(BASE_PATH + "/{id}")
        .then()
        .statusCode(404)
        .body("message", notNullValue())
        .body("status", equalTo(404));

    verify(unitService).deleteUnit(TEST_ID);
  }
}
