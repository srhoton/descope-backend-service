package com.descope.units.repository;

import java.util.Optional;

import com.descope.units.model.Unit;

/**
 * Repository interface for Unit entity persistence operations.
 *
 * <p>This interface defines the contract for unit data access operations.
 */
public interface UnitRepository {

  /**
   * Saves a unit to the data store.
   *
   * @param unit the unit to save
   * @return the saved unit
   */
  Unit save(Unit unit);

  /**
   * Finds a unit by its identifier.
   *
   * @param id the unit identifier
   * @return an Optional containing the unit if found, or empty if not found
   */
  Optional<Unit> findById(String id);

  /**
   * Updates an existing unit in the data store.
   *
   * @param unit the unit to update
   * @return the updated unit
   */
  Unit update(Unit unit);

  /**
   * Deletes a unit from the data store by its identifier.
   *
   * @param id the unit identifier
   */
  void deleteById(String id);

  /**
   * Checks if a unit exists with the specified identifier.
   *
   * @param id the unit identifier
   * @return true if the unit exists, false otherwise
   */
  boolean existsById(String id);
}
