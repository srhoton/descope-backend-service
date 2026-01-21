# Unit Management Service

A Quarkus-based REST API service for managing units with AWS DynamoDB as the data store, deployed as an AWS Lambda function behind an Application Load Balancer.

## Overview

This service provides CRUD operations for managing units (simple entities with an ID and name). It's deployed as an AWS Lambda function using a ZIP package, sitting behind an Application Load Balancer (ALB), and uses AWS DynamoDB for persistent storage.

### Features

- RESTful API for unit management (Create, Read, Update, Delete)
- UUIDv7 for unique identifiers (time-ordered for better database performance)
- AWS DynamoDB integration using Enhanced Client
- Comprehensive validation and error handling
- Health check endpoints for ALB
- Lambda deployment using Quarkus Lambda extension with ZIP packaging
- No Docker required for deployment
- Auto-scaling Lambda function with provisioned concurrency support
- Extensive test coverage (unit and integration tests)

## Prerequisites

- Java 17 or higher
- Gradle 8.x (wrapper included)
- Docker (only for running tests with Testcontainers)
- AWS CLI configured with credentials
- Terraform (for infrastructure deployment)
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

## Lambda ZIP Package

### Building the Lambda Package

The service uses a ZIP package deployment which is faster to build and doesn't require Docker:

```bash
# Build the application (creates an uber-jar)
./gradlew clean build

# The deployment package is automatically created at build/function.zip
```

The Quarkus build creates an uber-jar that contains all dependencies, which is then packaged into a ZIP file for Lambda deployment.

### Testing Locally

For local testing without Lambda, you can run in development mode:

```bash
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key

# Run in development mode with Lambda support
./gradlew quarkusDev
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

### Infrastructure as Code (Terraform)

This project includes comprehensive Terraform configurations for deploying to AWS with production-ready Lambda infrastructure.

**Quick Start:**

```bash
cd terraform
terraform init
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your configuration
terraform plan
terraform apply
```

**What's Included:**
- Multi-AZ VPC with public and private subnets
- Application Load Balancer with health checks
- AWS Lambda function with ZIP package deployment
- Lambda auto-scaling with provisioned concurrency (optional)
- DynamoDB table with encryption and backups
- IAM roles with least-privilege permissions
- CloudWatch monitoring and alarms
- X-Ray tracing for distributed debugging
- Security groups with proper isolation

**Key Lambda Configuration Variables:**
- `lambda_memory_size` - Memory allocation (default: 512 MB)
- `lambda_timeout` - Function timeout (default: 30 seconds)
- `lambda_provisioned_concurrent_executions` - Warm instances (default: 0, disabled)
- `lambda_enable_function_url` - Direct Lambda URL (default: false, use ALB)

### Automated Deployment

Use the included deployment script to build and deploy:

```bash
./terraform/deploy.sh
```

This script will:
1. Build the Quarkus application as an uber-jar
2. Create the Lambda deployment ZIP package
3. Deploy the infrastructure and function via Terraform

**Note:** The build process typically takes 1-2 minutes.

### Manual Deployment

If you prefer manual deployment or need to deploy to a different environment:

1. **Build the application:**
   ```bash
   ./gradlew clean build
   ```

2. **Create the ZIP package:**
   ```bash
   cd build/quarkus-app
   zip -r ../function.zip . -x "*.original"
   cd ../..
   ```

3. **Deploy with Terraform:**
   ```bash
   cd terraform
   terraform apply
   ```

Alternatively, update the Lambda function directly with AWS CLI:

```bash
aws lambda update-function-code \
  --function-name <function-name> \
  --zip-file fileb://build/function.zip \
  --region us-east-1
```

### Environment Configuration

The application requires these environment variables:

| Variable | Description | Required |
|----------|-------------|----------|
| `AWS_REGION` | AWS region for DynamoDB | Yes |
| `AWS_ACCESS_KEY_ID` | AWS credentials | Yes (unless using IAM roles) |
| `AWS_SECRET_ACCESS_KEY` | AWS credentials | Yes (unless using IAM roles) |
| `dynamodb.table.units` | DynamoDB table name | No (defaults to `units-table`) |

**Best Practice:** Use IAM roles (Lambda Execution Role) instead of access keys for production deployments. The Terraform configuration automatically sets up the necessary IAM permissions.

## Lambda Deployment Architecture

### Why Lambda?

This service uses AWS Lambda for several advantages:
- **Cost Efficiency**: Pay only for actual request processing time
- **Auto-Scaling**: Automatic scaling from zero to thousands of requests
- **No Server Management**: AWS manages the runtime environment
- **Integration**: Native integration with ALB and other AWS services
- **Simple Deployment**: ZIP packages are quick to build and deploy

### Cold Start Optimization

The service uses several strategies to minimize cold starts:
1. **Uber-JAR Packaging**: Single JAR with all dependencies reduces initialization time
2. **Provisioned Concurrency**: Can be enabled for guaranteed warm instances
3. **Memory Allocation**: Properly sized memory (512MB default) for optimal performance
4. **VPC Configuration**: Lambda runs in VPC for DynamoDB access

### Lambda Configuration

**Memory Allocation**: Default 512 MB
- More memory = more CPU power
- Adjust based on your workload
- Monitor CloudWatch metrics to optimize

**Timeout**: Default 30 seconds
- Sufficient for most CRUD operations
- Increase if you have complex queries
- ALB has a 60-second timeout limit

**Provisioned Concurrency**: Default 0 (disabled)
- Enable for consistent low latency
- Costs more but eliminates cold starts
- Recommended for production traffic

### Monitoring Lambda Performance

Key CloudWatch metrics to monitor:
- **Duration**: Execution time per invocation
- **Throttles**: Requests that couldn't execute due to concurrency limits
- **Errors**: Failed invocations
- **ConcurrentExecutions**: Number of simultaneous executions
- **ProvisionedConcurrencyUtilization**: If using provisioned concurrency

## Troubleshooting

### Common Issues

**Lambda Function Errors:**
- Check CloudWatch Logs for detailed error messages
- Verify the Lambda function has the correct IAM permissions
- Ensure the Lambda is deployed in private subnets with NAT Gateway access
- Check that the Lambda timeout is sufficient for your operations

**DynamoDB Connection Errors:**
- Verify the Lambda execution role has DynamoDB permissions
- Check that the DynamoDB table exists in the specified region
- Ensure Lambda is in VPC with route to DynamoDB (via VPC endpoint or NAT)

**ALB Health Check Failures:**
- Verify the health check path is correct: `/api/q/health`
- Check Lambda function logs for startup errors
- Ensure Lambda timeout is longer than ALB health check timeout
- Verify security groups allow traffic between ALB and Lambda

**Cold Start Issues:**
- Monitor Duration metrics in CloudWatch
- Consider enabling provisioned concurrency
- Check if memory allocation needs adjustment
- Verify native image build completed successfully

**Tests Failing:**
- Ensure Docker is running (required for Testcontainers)
- Check that no other process is using port 8080

**Build Errors:**
- Run `./gradlew clean build` to clean build artifacts
- Ensure Java 17+ is installed and set as JAVA_HOME
- For native builds, ensure Docker has sufficient resources (8GB+ RAM recommended)

## Contributing

1. Follow the Google Java Style Guide
2. Run `./gradlew spotlessApply` before committing
3. Ensure all tests pass: `./gradlew test`
4. Maintain test coverage above 80%

## License

Copyright (c) 2026 Descope. All rights reserved.
