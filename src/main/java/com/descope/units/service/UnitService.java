package com.descope.units.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.descope.units.exception.UnitNotFoundException;
import com.descope.units.model.Unit;
import com.descope.units.repository.UnitRepository;
import com.fasterxml.uuid.Generators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class for unit business logic.
 *
 * <p>This service handles the business logic for unit operations including creation, retrieval,
 * update, and deletion. It uses UUIDv7 for generating unique identifiers.
 */
@ApplicationScoped
public class UnitService {

  private static final Logger logger = LoggerFactory.getLogger(UnitService.class);

  private final UnitRepository unitRepository;

  /**
   * Constructs a UnitService with the specified repository.
   *
   * @param unitRepository the unit repository
   */
  @Inject
  public UnitService(UnitRepository unitRepository) {
    this.unitRepository = unitRepository;
  }

  /**
   * Creates a new unit with the specified name.
   *
   * <p>A UUIDv7 is automatically generated as the unit identifier.
   *
   * @param name the name of the unit
   * @return the created unit
   * @throws IllegalArgumentException if the name is null or empty
   */
  public Unit createUnit(String name) {
    logger.debug("Creating new unit with name: {}", name);
    String id = generateUuidV7();
    Unit unit = new Unit(id, name);
    Unit savedUnit = unitRepository.save(unit);
    logger.info("Created unit with id: {}", savedUnit.getId());
    return savedUnit;
  }

  /**
   * Retrieves a unit by its identifier.
   *
   * @param id the unit identifier
   * @return the unit
   * @throws UnitNotFoundException if the unit is not found
   */
  public Unit getUnitById(String id) {
    logger.debug("Retrieving unit with id: {}", id);
    return unitRepository
        .findById(id)
        .orElseThrow(
            () -> {
              logger.warn("Unit not found with id: {}", id);
              return new UnitNotFoundException(id);
            });
  }

  /**
   * Updates an existing unit with the specified name.
   *
   * @param id the unit identifier
   * @param name the new name for the unit
   * @return the updated unit
   * @throws UnitNotFoundException if the unit is not found
   * @throws IllegalArgumentException if the name is null or empty
   */
  public Unit updateUnit(String id, String name) {
    logger.debug("Updating unit with id: {}", id);

    // Verify the unit exists
    if (!unitRepository.existsById(id)) {
      logger.warn("Cannot update - unit not found with id: {}", id);
      throw new UnitNotFoundException(id);
    }

    Unit unit = new Unit(id, name);
    Unit updatedUnit = unitRepository.update(unit);
    logger.info("Updated unit with id: {}", updatedUnit.getId());
    return updatedUnit;
  }

  /**
   * Deletes a unit by its identifier.
   *
   * @param id the unit identifier
   * @throws UnitNotFoundException if the unit is not found
   */
  public void deleteUnit(String id) {
    logger.debug("Deleting unit with id: {}", id);

    // Verify the unit exists before deleting
    if (!unitRepository.existsById(id)) {
      logger.warn("Cannot delete - unit not found with id: {}", id);
      throw new UnitNotFoundException(id);
    }

    unitRepository.deleteById(id);
    logger.info("Deleted unit with id: {}", id);
  }

  /**
   * Generates a UUIDv7 string.
   *
   * <p>UUIDv7 is time-ordered and provides better database performance compared to UUIDv4.
   *
   * @return a UUIDv7 string
   */
  private String generateUuidV7() {
    return Generators.timeBasedEpochGenerator().generate().toString();
  }
}
