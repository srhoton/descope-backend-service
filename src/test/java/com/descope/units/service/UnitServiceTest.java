package com.descope.units.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.descope.units.exception.UnitNotFoundException;
import com.descope.units.model.Unit;
import com.descope.units.repository.UnitRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

  @Mock private UnitRepository unitRepository;

  @InjectMocks private UnitService unitService;

  private static final String TEST_ID = "01933b5e-7f00-7000-8000-000000000000";
  private static final String TEST_NAME = "Test Unit";

  @BeforeEach
  void setUp() {
    // Common setup if needed
  }

  @Test
  @DisplayName("createUnit - valid name provided - should create unit with generated UUID")
  void createUnit_validName_shouldCreateUnitWithGeneratedUuid() {
    // Given
    when(unitRepository.save(any(Unit.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // When
    Unit result = unitService.createUnit(TEST_NAME);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isNotNull().isNotEmpty();
    assertThat(result.getName()).isEqualTo(TEST_NAME);
    verify(unitRepository).save(any(Unit.class));
  }

  @Test
  @DisplayName("createUnit - empty name provided - should throw IllegalArgumentException")
  void createUnit_emptyName_shouldThrowException() {
    // When/Then
    assertThatThrownBy(() -> unitService.createUnit(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name cannot be null or empty");
  }

  @Test
  @DisplayName("createUnit - null name provided - should throw IllegalArgumentException")
  void createUnit_nullName_shouldThrowException() {
    // When/Then
    assertThatThrownBy(() -> unitService.createUnit(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name cannot be null or empty");
  }

  @Test
  @DisplayName("getUnitById - existing unit id - should return unit")
  void getUnitById_existingId_shouldReturnUnit() {
    // Given
    Unit expectedUnit = new Unit(TEST_ID, TEST_NAME);
    when(unitRepository.findById(TEST_ID)).thenReturn(Optional.of(expectedUnit));

    // When
    Unit result = unitService.getUnitById(TEST_ID);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(TEST_ID);
    assertThat(result.getName()).isEqualTo(TEST_NAME);
    verify(unitRepository).findById(TEST_ID);
  }

  @Test
  @DisplayName("getUnitById - non-existing unit id - should throw UnitNotFoundException")
  void getUnitById_nonExistingId_shouldThrowException() {
    // Given
    when(unitRepository.findById(TEST_ID)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> unitService.getUnitById(TEST_ID))
        .isInstanceOf(UnitNotFoundException.class)
        .hasMessageContaining(TEST_ID);
    verify(unitRepository).findById(TEST_ID);
  }

  @Test
  @DisplayName("updateUnit - existing unit with valid name - should update unit")
  void updateUnit_existingUnitValidName_shouldUpdateUnit() {
    // Given
    String updatedName = "Updated Name";
    when(unitRepository.existsById(TEST_ID)).thenReturn(true);
    when(unitRepository.update(any(Unit.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    Unit result = unitService.updateUnit(TEST_ID, updatedName);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(TEST_ID);
    assertThat(result.getName()).isEqualTo(updatedName);
    verify(unitRepository).existsById(TEST_ID);
    verify(unitRepository).update(any(Unit.class));
  }

  @Test
  @DisplayName("updateUnit - non-existing unit - should throw UnitNotFoundException")
  void updateUnit_nonExistingUnit_shouldThrowException() {
    // Given
    when(unitRepository.existsById(TEST_ID)).thenReturn(false);

    // When/Then
    assertThatThrownBy(() -> unitService.updateUnit(TEST_ID, "New Name"))
        .isInstanceOf(UnitNotFoundException.class)
        .hasMessageContaining(TEST_ID);
    verify(unitRepository).existsById(TEST_ID);
  }

  @Test
  @DisplayName("updateUnit - empty name provided - should throw IllegalArgumentException")
  void updateUnit_emptyName_shouldThrowException() {
    // Given
    when(unitRepository.existsById(TEST_ID)).thenReturn(true);

    // When/Then
    assertThatThrownBy(() -> unitService.updateUnit(TEST_ID, ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name cannot be null or empty");
  }

  @Test
  @DisplayName("deleteUnit - existing unit - should delete unit")
  void deleteUnit_existingUnit_shouldDeleteUnit() {
    // Given
    when(unitRepository.existsById(TEST_ID)).thenReturn(true);

    // When
    unitService.deleteUnit(TEST_ID);

    // Then
    verify(unitRepository).existsById(TEST_ID);
    verify(unitRepository).deleteById(TEST_ID);
  }

  @Test
  @DisplayName("deleteUnit - non-existing unit - should throw UnitNotFoundException")
  void deleteUnit_nonExistingUnit_shouldThrowException() {
    // Given
    when(unitRepository.existsById(TEST_ID)).thenReturn(false);

    // When/Then
    assertThatThrownBy(() -> unitService.deleteUnit(TEST_ID))
        .isInstanceOf(UnitNotFoundException.class)
        .hasMessageContaining(TEST_ID);
    verify(unitRepository).existsById(TEST_ID);
  }
}
