# SDLC Plan: Unit Management Service

## Status: Updated - Converted to ZIP Deployment
## Created: 2026-01-21T10:00:00Z
## Last Updated: 2026-01-21T19:00:00Z

## Original Request
> Build a Quarkus application that exposes a REST endpoint to manage a list of units. It will sit behind an ALB and will use DynamoDB as the data store. A unit can just have an id and a name. The application should support the following operations:
> - Create a unit
> - Get a unit by id
> - Update a unit by id
> - Delete a unit by id

## Updated Request
> Convert the existing ECS-based Quarkus application to run on AWS Lambda instead, maintaining the ALB integration and DynamoDB data store.

## Clarifications
- **Feature Branch**: `feature/unit-management-service`
- **DynamoDB Configuration**: Production table name `units-table`, provisioned externally
- **ID Generation**: UUIDv7 for auto-generated IDs
- **ALB Health Checks**: Use `/q/health` endpoint
- **Error Handling**: Standard HTTP codes (404 for not found, 409 for conflicts)
- **AWS Configuration**: Environment variables for credentials and region
- **Testing**: Unit tests with mocked DynamoDB, integration tests with Testcontainers/LocalStack
- **Documentation**: README with setup instructions and API documentation

## Architecture Overview
This is a single-component Quarkus REST API service that provides CRUD operations for units stored in AWS DynamoDB, deployed as an AWS Lambda function. The service:
- Runs as an AWS Lambda function using ZIP package deployment
- Exposes RESTful endpoints for unit management via ALB
- Uses Quarkus Lambda REST extension for seamless integration
- Uses Quarkus DynamoDB extension for data access
- Supports health checks for ALB
- Uses UUIDv7 for unique identifiers
- Follows clean architecture with proper separation of concerns
- Uses uber-JAR packaging for simplified deployment without Docker

## Components

### Component: Unit Management REST API (Lambda)
- **Type**: backend
- **Technology**: Java/Quarkus/Gradle/AWS Lambda
- **Subagent**: java-quarkus-agent (implemented directly by routing agent)
- **Status**: Approved - Converted to Lambda ZIP Deployment
- **Dependencies**: None (standalone service)
- **Description**: Complete Quarkus application with REST endpoints, DynamoDB integration, error handling, validation, comprehensive testing, and Lambda ZIP deployment support
- **Files**:
  - `build.gradle` - Gradle build configuration with Quarkus Lambda dependencies and uber-jar packaging
  - `settings.gradle` - Gradle settings
  - `gradle.properties` - Gradle properties
  - `src/main/java/com/descope/units/model/Unit.java` - Domain model
  - `src/main/java/com/descope/units/model/UnitDao.java` - DynamoDB DAO model
  - `src/main/java/com/descope/units/dto/CreateUnitRequest.java` - Create request DTO
  - `src/main/java/com/descope/units/dto/UpdateUnitRequest.java` - Update request DTO
  - `src/main/java/com/descope/units/dto/UnitResponse.java` - Response DTO
  - `src/main/java/com/descope/units/dto/ErrorResponse.java` - Error response DTO
  - `src/main/java/com/descope/units/repository/UnitRepository.java` - Repository interface
  - `src/main/java/com/descope/units/repository/DynamoDbUnitRepository.java` - DynamoDB repository implementation
  - `src/main/java/com/descope/units/service/UnitService.java` - Business logic service
  - `src/main/java/com/descope/units/resource/UnitResource.java` - REST endpoint controller
  - `src/main/java/com/descope/units/exception/UnitNotFoundException.java` - Custom exception
  - `src/main/java/com/descope/units/exception/GlobalExceptionHandler.java` - Global exception handler
  - `src/main/resources/application.properties` - Application configuration (updated for Lambda ZIP)
  - `src/test/java/com/descope/units/service/UnitServiceTest.java` - Unit tests for service
  - `src/test/java/com/descope/units/repository/DynamoDbUnitRepositoryTest.java` - Unit tests for repository
  - `src/test/java/com/descope/units/resource/UnitResourceTest.java` - Unit tests for REST resource
  - `src/test/java/com/descope/units/integration/UnitResourceIntegrationTest.java` - Integration tests with Testcontainers
  - `README.md` - Setup and API documentation (updated for Lambda ZIP deployment)
  - `.gitignore` - Git ignore file
  - `Dockerfile` - Original container image definition (kept for reference)
  - `Dockerfile.lambda` - Lambda native container image definition (kept for reference)
  - `spotless.gradle` - Code formatting configuration
  - `terraform/modules/lambda/` - Lambda deployment Terraform module (updated for ZIP)
  - `terraform/deploy.sh` - Updated deployment script for Lambda ZIP
- **Review History**:
  - 2026-01-21 16:00 Functional Review: Pass - All requirements implemented correctly
  - 2026-01-21 16:00 Quality Review: Pass - Code meets all quality standards for production
  - 2026-01-21 17:30 Lambda Conversion: Complete - Successfully converted to Lambda deployment
  - 2026-01-21 19:00 ZIP Conversion: Complete - Converted from container image to ZIP deployment

## Implementation Order
1. Unit Management REST API - Single standalone component

## Commits
- [x] Unit Management REST API: Add Quarkus unit management service with DynamoDB integration (commit: 9118c39)

## Current Phase
**Phase**: Complete - Lambda Conversion
**Current Component**: Unit Management REST API (Lambda)
**Current Action**: Lambda conversion complete, ready for testing and deployment

## Lambda Conversion Details

### Changes Made
1. **Application Code**:
   - Added `quarkus-amazon-lambda-rest` dependency to build.gradle
   - Updated application.properties with Lambda-specific configuration
   - No changes required to Java code (Quarkus Lambda extension handles integration)

2. **Container Image**:
   - Created Dockerfile.lambda for native Lambda container builds
   - Multi-stage build using GraalVM Mandrel builder
   - Optimized for AWS Lambda provided runtime

3. **Infrastructure (Terraform)**:
   - Created new `terraform/modules/lambda/` module
   - Updated `terraform/modules/iam/` to include Lambda execution roles
   - Updated `terraform/modules/security/` to add Lambda security group
   - Updated `terraform/modules/alb/` to support Lambda targets
   - Modified `terraform/main.tf` to use Lambda instead of ECS
   - Added Lambda-specific variables (memory, timeout, provisioned concurrency)
   - Updated outputs to expose Lambda resources

4. **Deployment**:
   - Updated `terraform/deploy.sh` for Lambda deployment workflow
   - Script now builds native image and updates Lambda function
   - Includes wait for function update completion

5. **Documentation**:
   - Updated README with Lambda deployment instructions
   - Added Lambda architecture and monitoring sections
   - Updated troubleshooting for Lambda-specific issues
   - Updated SDLC plan with conversion details

### Benefits of Lambda Deployment
- **Cost Efficiency**: Pay only for execution time
- **Auto-Scaling**: Scales from 0 to thousands of concurrent executions
- **No Server Management**: AWS manages runtime and scaling
- **Fast Cold Starts**: Native compilation provides sub-second startup
- **Integration**: Native ALB and DynamoDB integration

### Migration Notes
- ECS modules and configuration kept in codebase (commented out) for reference
- All existing Java code remains unchanged
- Tests continue to work without modification
- API endpoints remain the same from client perspective

## ALB Configuration Update (2026-01-21)

### Changes Made
The infrastructure was updated to use an existing Application Load Balancer instead of creating a new one:

1. **Root Variables**:
   - Added `existing_alb_arn` variable with default value: `arn:aws:elasticloadbalancing:us-west-2:345594586248:loadbalancer/app/external-private-alb/720e2b5474d3d602`
   - Added validation for proper ALB ARN format

2. **ALB Module Variables**:
   - Replaced `public_subnet_ids` with `existing_alb_arn` parameter
   - Removed `alb_security_group_id` parameter (not needed for existing ALB)
   - Removed `enable_deletion_protection` parameter (managed by existing ALB)
   - Added `listener_priority` parameter (default: 100) for listener rule priority
   - Added `path_pattern` parameter (default: ["/api/*"]) for routing configuration

3. **ALB Module Implementation**:
   - Removed `aws_lb` resource that created a new ALB
   - Added `data.aws_lb.existing` data source to look up existing ALB
   - Added `data.aws_lb_listener.http` to find existing HTTP listener
   - Added `data.aws_lb_listener.https` to find existing HTTPS listener (optional)
   - Created `aws_lb_listener_rule` resources to attach target group to existing listeners
   - Updated CloudWatch alarms to reference the existing ALB
   - Target group creation remains the same

4. **Main Configuration**:
   - Updated module call to pass `existing_alb_arn` instead of creating ALB resources
   - Removed parameters for ALB creation (subnets, security groups, deletion protection)

5. **Outputs**:
   - Updated ALB outputs to reference data source instead of created resource
   - Added outputs for listener rule ARNs

### Benefits
- Reuses existing infrastructure (cost savings)
- Multiple services can share the same ALB
- Centralized ALB management
- Reduced resource creation complexity
- Only creates target group and listener rules specific to this service

## ZIP Deployment Conversion (2026-01-21)

### Changes Made
The infrastructure was updated to use ZIP package deployment instead of container images:

1. **Build Configuration**:
   - Updated `build.gradle` to configure uber-jar packaging
   - Modified `application.properties` to remove native build settings
   - Configured Quarkus to use `package.type=uber-jar`

2. **Lambda Module**:
   - Changed `package_type` from "Image" to "Zip"
   - Updated to use `filename` and `source_code_hash` instead of `image_uri`
   - Added `runtime = "java17"` and explicit handler configuration
   - Removed ECR-related variables (`ecr_repository_url`, `container_image_tag`)
   - Added `lambda_zip_file` variable for ZIP package path

3. **Infrastructure**:
   - Commented out ECR module in `terraform/main.tf`
   - Removed ECR variables from `terraform/variables.tf`
   - Updated Lambda module call to use ZIP file instead of ECR
   - Commented out ECR outputs in `terraform/outputs.tf`

4. **Deployment Script**:
   - Removed Docker build steps
   - Removed ECR login and push steps
   - Added Gradle build step to create uber-jar
   - Added ZIP packaging step for Lambda deployment
   - Updated to deploy via Terraform apply instead of direct Lambda update
   - Removed Docker as a prerequisite

5. **Documentation**:
   - Updated README to reflect ZIP deployment
   - Removed references to native image compilation
   - Updated deployment instructions for ZIP packaging
   - Clarified that Docker is only needed for tests
   - Updated cold start optimization section

### Benefits of ZIP Deployment
- **No Docker Required**: Simplified build process without container dependencies
- **Faster Builds**: Building a JAR takes 1-2 minutes vs 5-10 minutes for native images
- **Simpler CI/CD**: No need to manage container registries (ECR)
- **Cost Savings**: No ECR storage costs
- **Easier Debugging**: JVM-based Lambda functions provide better error messages
- **Standard Java Runtime**: Uses AWS-managed Java 17 runtime

### Migration Notes
- Container image Dockerfiles kept in codebase for reference
- ECR module commented out but preserved in Terraform for potential future use
- All existing Java code remains unchanged
- Tests continue to work without modification
- API endpoints remain the same from client perspective
- Cold start times may be slightly longer than native images but still acceptable

## Pull Request
- **URL**: https://github.com/srhoton/descope-backend-service/pull/1
- **Branch**: feature/unit-management-service
- **Status**: Open - Pending update with ZIP deployment changes
- **Title**: Add Quarkus Unit Management Service with Lambda ZIP Deployment

## Error Log
None
