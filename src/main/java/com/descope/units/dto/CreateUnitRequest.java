package com.descope.units.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new unit.
 *
 * <p>The unit name is required and must not be blank.
 */
public class CreateUnitRequest {

  @NotBlank(message = "Unit name is required and cannot be blank")
  private String name;

  /** Default constructor for JSON deserialization. */
  public CreateUnitRequest() {}

  /**
   * Constructs a CreateUnitRequest with the specified name.
   *
   * @param name the unit name
   */
  public CreateUnitRequest(String name) {
    this.name = name;
  }

  /**
   * Returns the unit name.
   *
   * @return the unit name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the unit name.
   *
   * @param name the unit name
   */
  public void setName(String name) {
    this.name = name;
  }
}
