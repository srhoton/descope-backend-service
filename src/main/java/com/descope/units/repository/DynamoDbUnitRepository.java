package com.descope.units.repository;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.descope.units.model.Unit;
import com.descope.units.model.UnitDao;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * DynamoDB implementation of the UnitRepository interface.
 *
 * <p>This implementation uses the AWS SDK v2 Enhanced Client for DynamoDB operations.
 */
@ApplicationScoped
public class DynamoDbUnitRepository implements UnitRepository {

  private static final Logger logger = LoggerFactory.getLogger(DynamoDbUnitRepository.class);

  private final DynamoDbTable<UnitDao> table;

  /**
   * Constructs a DynamoDbUnitRepository with the specified DynamoDB client and table name.
   *
   * @param dynamoDbClient the DynamoDB client
   * @param tableName the name of the DynamoDB table
   */
  @Inject
  public DynamoDbUnitRepository(
      DynamoDbClient dynamoDbClient,
      @ConfigProperty(name = "dynamodb.table.units") String tableName) {
    DynamoDbEnhancedClient enhancedClient =
        DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();

    this.table = enhancedClient.table(tableName, TableSchema.fromBean(UnitDao.class));
    logger.info("Initialized DynamoDbUnitRepository with table: {}", tableName);
  }

  @Override
  public Unit save(Unit unit) {
    logger.debug("Saving unit with id: {}", unit.getId());
    UnitDao dao = UnitDao.fromDomain(unit);
    table.putItem(dao);
    logger.info("Successfully saved unit with id: {}", unit.getId());
    return unit;
  }

  @Override
  public Optional<Unit> findById(String id) {
    logger.debug("Finding unit by id: {}", id);
    Key key = Key.builder().partitionValue(id).build();
    UnitDao dao = table.getItem(key);

    if (dao == null) {
      logger.debug("Unit not found with id: {}", id);
      return Optional.empty();
    }

    logger.debug("Found unit with id: {}", id);
    return Optional.of(dao.toDomain());
  }

  @Override
  public Unit update(Unit unit) {
    logger.debug("Updating unit with id: {}", unit.getId());
    UnitDao dao = UnitDao.fromDomain(unit);
    table.putItem(dao);
    logger.info("Successfully updated unit with id: {}", unit.getId());
    return unit;
  }

  @Override
  public void deleteById(String id) {
    logger.debug("Deleting unit with id: {}", id);
    Key key = Key.builder().partitionValue(id).build();
    table.deleteItem(key);
    logger.info("Successfully deleted unit with id: {}", id);
  }

  @Override
  public boolean existsById(String id) {
    logger.debug("Checking if unit exists with id: {}", id);
    return findById(id).isPresent();
  }
}
