package com.descope.units.model;

import java.util.Objects;

/**
 * Domain model representing a Unit entity.
 *
 * <p>A Unit is a simple entity with an identifier and a name. The identifier is a UUIDv7 string and
 * the name is a required non-empty string.
 */
public class Unit {

  private final String id;
  private final String name;

  /**
   * Constructs a new Unit with the specified id and name.
   *
   * @param id the unique identifier (UUIDv7 format)
   * @param name the name of the unit
   * @throws IllegalArgumentException if name is null or empty
   */
  public Unit(String id, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Unit name cannot be null or empty");
    }
    this.id = id;
    this.name = name;
  }

  /**
   * Returns the unique identifier of this unit.
   *
   * @return the unit id
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the name of this unit.
   *
   * @return the unit name
   */
  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Unit unit = (Unit) o;
    return Objects.equals(id, unit.id) && Objects.equals(name, unit.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    return "Unit{" + "id='" + id + '\'' + ", name='" + name + '\'' + '}';
  }
}
