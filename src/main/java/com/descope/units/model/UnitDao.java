package com.descope.units.model;

import java.util.Objects;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Data Access Object (DAO) for Unit entity mapped to DynamoDB.
 *
 * <p>This class represents the DynamoDB table structure for units with appropriate annotations for
 * the DynamoDB Enhanced Client.
 */
@DynamoDbBean
public class UnitDao {

  private String id;
  private String name;

  /** Default constructor required by DynamoDB Enhanced Client. */
  public UnitDao() {}

  /**
   * Constructs a UnitDao with the specified id and name.
   *
   * @param id the unit identifier
   * @param name the unit name
   */
  public UnitDao(String id, String name) {
    this.id = id;
    this.name = name;
  }

  /**
   * Returns the partition key (id) for this unit.
   *
   * @return the unit id
   */
  @DynamoDbPartitionKey
  @DynamoDbAttribute("id")
  public String getId() {
    return id;
  }

  /**
   * Sets the partition key (id) for this unit.
   *
   * @param id the unit id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Returns the name of this unit.
   *
   * @return the unit name
   */
  @DynamoDbAttribute("name")
  public String getName() {
    return name;
  }

  /**
   * Sets the name of this unit.
   *
   * @param name the unit name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Converts this DAO to a domain Unit object.
   *
   * @return the domain Unit
   */
  public Unit toDomain() {
    return new Unit(this.id, this.name);
  }

  /**
   * Creates a UnitDao from a domain Unit object.
   *
   * @param unit the domain Unit
   * @return the UnitDao
   */
  public static UnitDao fromDomain(Unit unit) {
    return new UnitDao(unit.getId(), unit.getName());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UnitDao unitDao = (UnitDao) o;
    return Objects.equals(id, unitDao.id) && Objects.equals(name, unitDao.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    return "UnitDao{" + "id='" + id + '\'' + ", name='" + name + '\'' + '}';
  }
}
