package com.descope.units.integration;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Quarkus test resource for LocalStack DynamoDB integration testing.
 *
 * <p>This resource starts a LocalStack container with DynamoDB and creates the necessary table for
 * testing.
 */
public class LocalStackDynamoDbResource implements QuarkusTestResourceLifecycleManager {

  private static final String TABLE_NAME = "units-table-test";
  private LocalStackContainer localstack;

  @Override
  public Map<String, String> start() {
    // Start LocalStack container with DynamoDB
    localstack =
        new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(LocalStackContainer.Service.DYNAMODB);
    localstack.start();

    // Create DynamoDB client
    DynamoDbClient dynamoDbClient =
        DynamoDbClient.builder()
            .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        localstack.getAccessKey(), localstack.getSecretKey())))
            .region(Region.of(localstack.getRegion()))
            .build();

    // Create the units table
    createUnitsTable(dynamoDbClient);

    // Return configuration properties for Quarkus
    return Map.of(
        "quarkus.dynamodb.endpoint-override",
        localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString(),
        "quarkus.dynamodb.aws.region",
        localstack.getRegion(),
        "quarkus.dynamodb.aws.credentials.type",
        "static",
        "quarkus.dynamodb.aws.credentials.static-provider.access-key-id",
        localstack.getAccessKey(),
        "quarkus.dynamodb.aws.credentials.static-provider.secret-access-key",
        localstack.getSecretKey(),
        "dynamodb.table.units",
        TABLE_NAME);
  }

  @Override
  public void stop() {
    if (localstack != null) {
      localstack.stop();
    }
  }

  private void createUnitsTable(DynamoDbClient dynamoDbClient) {
    CreateTableRequest createTableRequest =
        CreateTableRequest.builder()
            .tableName(TABLE_NAME)
            .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName("id")
                    .attributeType(ScalarAttributeType.S)
                    .build())
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .build();

    dynamoDbClient.createTable(createTableRequest);
  }
}
