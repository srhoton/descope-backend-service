# SDLC Plan: Unit Management Service

## Status: Review
## Created: 2026-01-21T10:00:00Z
## Last Updated: 2026-01-21T16:00:00Z

## Original Request
> Build a Quarkus application that exposes a REST endpoint to manage a list of units. It will sit behind an ALB and will use DynamoDB as the data store. A unit can just have an id and a name. The application should support the following operations:
> - Create a unit
> - Get a unit by id
> - Update a unit by id
> - Delete a unit by id

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
This is a single-component Quarkus REST API service that provides CRUD operations for units stored in AWS DynamoDB. The service will:
- Expose RESTful endpoints for unit management
- Use Quarkus DynamoDB extension for data access
- Support health checks for ALB
- Use UUIDv7 for unique identifiers
- Follow clean architecture with proper separation of concerns

## Components

### Component: Unit Management REST API
- **Type**: backend
- **Technology**: Java/Quarkus/Gradle
- **Subagent**: java-quarkus-agent (implemented directly by routing agent)
- **Status**: Approved
- **Dependencies**: None (standalone service)
- **Description**: Complete Quarkus application with REST endpoints, DynamoDB integration, error handling, validation, and comprehensive testing
- **Files**:
  - `build.gradle` - Gradle build configuration with Quarkus dependencies
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
  - `src/main/resources/application.properties` - Application configuration
  - `src/test/java/com/descope/units/service/UnitServiceTest.java` - Unit tests for service
  - `src/test/java/com/descope/units/repository/DynamoDbUnitRepositoryTest.java` - Unit tests for repository
  - `src/test/java/com/descope/units/resource/UnitResourceTest.java` - Unit tests for REST resource
  - `src/test/java/com/descope/units/integration/UnitResourceIntegrationTest.java` - Integration tests with Testcontainers
  - `README.md` - Setup and API documentation
  - `.gitignore` - Git ignore file
  - `Dockerfile` - Container image definition
  - `spotless.gradle` - Code formatting configuration
- **Review History**:
  - 2026-01-21 16:00 Functional Review: Pass - All requirements implemented correctly
  - 2026-01-21 16:00 Quality Review: Pass - Code meets all quality standards for production

## Implementation Order
1. Unit Management REST API - Single standalone component

## Commits
- [ ] Unit Management REST API: Add Quarkus unit management service with DynamoDB integration

## Current Phase
**Phase**: 4-Commit
**Current Component**: Unit Management REST API
**Current Action**: Committing approved implementation

## Error Log
None
