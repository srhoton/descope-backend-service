# Unit Management Service

A Quarkus-based REST API service for managing units with AWS DynamoDB as the data store.

## Overview

This service provides CRUD operations for managing units (simple entities with an ID and name). It's designed to run behind an Application Load Balancer (ALB) and uses AWS DynamoDB for persistent storage.

### Features

- RESTful API for unit management (Create, Read, Update, Delete)
- UUIDv7 for unique identifiers (time-ordered for better database performance)
- AWS DynamoDB integration using Enhanced Client
- Comprehensive validation and error handling
- Health check endpoints for ALB
- Docker support for containerized deployment
- Extensive test coverage (unit and integration tests)

## Prerequisites

- Java 17 or higher
- Gradle 8.x (wrapper included)
- Docker (for running tests with Testcontainers)
- AWS credentials (for production deployment)
- DynamoDB table named `units-table` (provisioned externally)

## Project Structure

```
src/
├── main/
│   ├── java/com/descope/units/
│   │   ├── model/           # Domain models (Unit, UnitDao)
│   │   ├── dto/             # Request/Response DTOs
│   │   ├── repository/      # Data access layer
│   │   ├── service/         # Business logic layer
│   │   ├── resource/        # REST API controllers
│   │   └── exception/       # Custom exceptions and handlers
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/descope/units/
        ├── service/         # Service layer unit tests
        ├── repository/      # Repository layer unit tests
        ├── resource/        # REST API unit tests
        └── integration/     # Integration tests with LocalStack
```

## Local Development

### Building the Application

```bash
# Build the application
./gradlew build

# Build without running tests
./gradlew build -x test

# Format code with Spotless
./gradlew spotlessApply
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run only unit tests
./gradlew test --tests '*Test'

# Run only integration tests
./gradlew test --tests '*IntegrationTest'
```

### Running the Application

For local development, you'll need to configure AWS credentials as environment variables:

```bash
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key

# Run in development mode (with live reload)
./gradlew quarkusDev
```

The application will start on `http://localhost:8080`.

## API Documentation

### Base URL

```
http://localhost:8080/api
```

### Endpoints

#### Create Unit

Creates a new unit with an auto-generated UUIDv7.

```http
POST /api/units
Content-Type: application/json

{
  "name": "Example Unit"
}
```

**Response (201 Created):**
```json
{
  "id": "01933b5e-7f00-7000-8000-000000000000",
  "name": "Example Unit"
}
```

#### Get Unit by ID

Retrieves a unit by its identifier.

```http
GET /api/units/{id}
```

**Response (200 OK):**
```json
{
  "id": "01933b5e-7f00-7000-8000-000000000000",
  "name": "Example Unit"
}
```

**Response (404 Not Found):**
```json
{
  "message": "Unit with id '01933b5e-7f00-7000-8000-000000000000' not found",
  "status": 404,
  "timestamp": "2026-01-21T10:00:00.000Z"
}
```

#### Update Unit

Updates an existing unit's name.

```http
PUT /api/units/{id}
Content-Type: application/json

{
  "name": "Updated Unit Name"
}
```

**Response (200 OK):**
```json
{
  "id": "01933b5e-7f00-7000-8000-000000000000",
  "name": "Updated Unit Name"
}
```

**Response (404 Not Found):** Same as Get Unit

#### Delete Unit

Deletes a unit by its identifier.

```http
DELETE /api/units/{id}
```

**Response (204 No Content):** Empty body

**Response (404 Not Found):** Same as Get Unit

### Error Responses

All error responses follow this structure:

```json
{
  "message": "Error description",
  "status": 400,
  "timestamp": "2026-01-21T10:00:00.000Z"
}
```

**Status Codes:**
- `200` - Success (GET, PUT)
- `201` - Created (POST)
- `204` - No Content (DELETE)
- `400` - Bad Request (validation errors)
- `404` - Not Found (unit doesn't exist)
- `500` - Internal Server Error (unexpected errors)

### Health Check

The service exposes health check endpoints for ALB monitoring:

```http
GET /q/health
GET /q/health/live
GET /q/health/ready
```

## Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `AWS_REGION` | AWS region for DynamoDB | `us-east-1` | Yes |
| `AWS_ACCESS_KEY_ID` | AWS access key | - | Yes (production) |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key | - | Yes (production) |
| `dynamodb.table.units` | DynamoDB table name | `units-table` | No |

## Docker

### Building the Docker Image

```bash
# Build native executable (requires GraalVM)
./gradlew build -Dquarkus.package.type=native

# Build JVM-based Docker image
docker build -f src/main/docker/Dockerfile.jvm -t unit-management-service:latest .

# Build native Docker image
docker build -f src/main/docker/Dockerfile.native -t unit-management-service:native .
```

### Running with Docker

```bash
docker run -p 8080:8080 \
  -e AWS_REGION=us-east-1 \
  -e AWS_ACCESS_KEY_ID=your-access-key \
  -e AWS_SECRET_ACCESS_KEY=your-secret-key \
  unit-management-service:latest
```

## DynamoDB Table Schema

The service expects a DynamoDB table with the following schema:

**Table Name:** `units-table`

**Primary Key:**
- Partition Key: `id` (String)

**Attributes:**
- `id`: String (UUIDv7 format)
- `name`: String

**Billing Mode:** On-demand (recommended) or Provisioned

### Creating the Table (AWS CLI)

```bash
aws dynamodb create-table \
  --table-name units-table \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

## Testing

### Unit Tests

Unit tests use Mockito to mock dependencies and test business logic in isolation.

```bash
./gradlew test --tests 'com.descope.units.service.*'
./gradlew test --tests 'com.descope.units.resource.*'
```

### Integration Tests

Integration tests use Testcontainers with LocalStack to provide a real DynamoDB instance for testing.

```bash
./gradlew test --tests 'com.descope.units.integration.*'
```

**Note:** Integration tests require Docker to be running.

### Test Coverage

View test coverage report after running tests:

```bash
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## Code Quality

### Formatting

The project uses Spotless with Google Java Format:

```bash
# Check formatting
./gradlew spotlessCheck

# Apply formatting
./gradlew spotlessApply
```

### Linting

Follow Google Java Style Guide. The build is configured to automatically format code before compilation.

## CI/CD

### GitHub Actions Example

```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build and Test
        run: ./gradlew build
      - name: Upload Coverage
        uses: codecov/codecov-action@v3
```

## Deployment

### AWS Deployment Considerations

1. **IAM Permissions:** The service requires DynamoDB read/write permissions
2. **VPC:** Can run in public or private subnets (private recommended)
3. **ALB Configuration:**
   - Health check path: `/q/health`
   - Health check interval: 30 seconds
   - Healthy threshold: 2
   - Unhealthy threshold: 3
4. **Security Groups:** Allow inbound HTTP/HTTPS from ALB
5. **Environment Variables:** Configure via ECS task definition or EC2 user data

### IAM Policy Example

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem",
        "dynamodb:Query",
        "dynamodb:Scan"
      ],
      "Resource": "arn:aws:dynamodb:us-east-1:*:table/units-table"
    }
  ]
}
```

## Troubleshooting

### Common Issues

**DynamoDB Connection Errors:**
- Verify AWS credentials are correctly configured
- Check that the DynamoDB table exists in the specified region
- Ensure IAM permissions are properly configured

**Tests Failing:**
- Ensure Docker is running (required for Testcontainers)
- Check that no other process is using port 8080

**Build Errors:**
- Run `./gradlew clean build` to clean build artifacts
- Ensure Java 17+ is installed and set as JAVA_HOME

## Contributing

1. Follow the Google Java Style Guide
2. Run `./gradlew spotlessApply` before committing
3. Ensure all tests pass: `./gradlew test`
4. Maintain test coverage above 80%

## License

Copyright (c) 2026 Descope. All rights reserved.
