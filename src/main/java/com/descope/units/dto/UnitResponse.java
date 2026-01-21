package com.descope.units.dto;

import com.descope.units.model.Unit;

/**
 * Response DTO for unit operations.
 *
 * <p>This DTO represents the external API representation of a unit.
 */
public class UnitResponse {

  private String id;
  private String name;

  /** Default constructor for JSON serialization. */
  public UnitResponse() {}

  /**
   * Constructs a UnitResponse with the specified id and name.
   *
   * @param id the unit identifier
   * @param name the unit name
   */
  public UnitResponse(String id, String name) {
    this.id = id;
    this.name = name;
  }

  /**
   * Creates a UnitResponse from a domain Unit object.
   *
   * @param unit the domain Unit
   * @return the UnitResponse
   */
  public static UnitResponse fromDomain(Unit unit) {
    return new UnitResponse(unit.getId(), unit.getName());
  }

  /**
   * Returns the unit identifier.
   *
   * @return the unit id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the unit identifier.
   *
   * @param id the unit id
   */
  public void setId(String id) {
    this.id = id;
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
