package com.descope.units.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating an existing unit.
 *
 * <p>The unit name is required and must not be blank.
 */
public class UpdateUnitRequest {

  @NotBlank(message = "Unit name is required and cannot be blank")
  private String name;

  /** Default constructor for JSON deserialization. */
  public UpdateUnitRequest() {}

  /**
   * Constructs an UpdateUnitRequest with the specified name.
   *
   * @param name the unit name
   */
  public UpdateUnitRequest(String name) {
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
