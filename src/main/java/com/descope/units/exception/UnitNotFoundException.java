package com.descope.units.exception;

/**
 * Exception thrown when a requested unit is not found in the data store.
 *
 * <p>This exception is typically thrown when attempting to retrieve, update, or delete a unit that
 * does not exist.
 */
public class UnitNotFoundException extends RuntimeException {

  private final String unitId;

  /**
   * Constructs a new UnitNotFoundException with the specified unit id.
   *
   * @param unitId the id of the unit that was not found
   */
  public UnitNotFoundException(String unitId) {
    super(String.format("Unit with id '%s' not found", unitId));
    this.unitId = unitId;
  }

  /**
   * Returns the id of the unit that was not found.
   *
   * @return the unit id
   */
  public String getUnitId() {
    return unitId;
  }
}
