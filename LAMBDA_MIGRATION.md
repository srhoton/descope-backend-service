# Lambda Migration Summary

## Overview

This document summarizes the conversion of the Unit Management Service from AWS ECS Fargate deployment to AWS Lambda deployment. The migration maintains all existing functionality while leveraging Lambda's benefits of cost efficiency, auto-scaling, and simplified operations.

## Migration Date

January 21, 2026

## Key Changes

### 1. Application Code

#### build.gradle
- **Added**: `io.quarkus:quarkus-amazon-lambda-rest` dependency
- **Purpose**: Enables Quarkus to run as a Lambda function with REST endpoint support
- **Impact**: No changes to business logic required

#### application.properties
- **Added**: Lambda-specific configuration
  - `quarkus.lambda.enable-polling-jvm-mode=true`
  - `quarkus.lambda.handler=io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler`
- **Added**: Native build configuration for Lambda
  - Container build settings
  - Mandrel builder image specification
- **Impact**: Application can now run in both development mode and Lambda

#### Java Code
- **Changes**: NONE
- **Reason**: Quarkus Lambda extension transparently handles Lambda integration
- **Benefit**: Existing tests, business logic, and REST endpoints work unchanged

### 2. Container Images

#### Dockerfile.lambda (New)
- **Purpose**: Build native Lambda container image
- **Architecture**: Multi-stage build
  1. Stage 1: Compile Quarkus app to native executable using GraalVM Mandrel
  2. Stage 2: Package into AWS Lambda provided runtime image
- **Benefits**:
  - Fast cold starts (sub-second)
  - Small image size
  - Optimized for Lambda execution

#### Dockerfile (Kept)
- **Status**: Retained for reference
- **Purpose**: Original JVM-based containerization
- **Use Case**: Can still be used for non-Lambda deployments if needed

### 3. Infrastructure (Terraform)

#### New Modules

##### terraform/modules/lambda/
- **main.tf**: Lambda function, CloudWatch logs, alarms, auto-scaling
- **variables.tf**: Lambda configuration variables
- **outputs.tf**: Lambda resource outputs
- **Key Resources**:
  - Lambda function with container image
  - VPC configuration for DynamoDB access
  - CloudWatch log group
  - Lambda function URL (optional)
  - Provisioned concurrency (optional)
  - Auto-scaling policies
  - CloudWatch alarms for errors, throttles, and duration

#### Modified Modules

##### terraform/modules/iam/
- **Added**: Lambda execution role with policies
  - Basic execution (logs, VPC)
  - DynamoDB access
  - X-Ray tracing
- **Kept**: ECS roles for reference

##### terraform/modules/security/
- **Added**: Lambda security group
  - Egress rules for HTTPS (AWS APIs)
  - Egress rules for HTTP (optional)
- **Modified**: ALB security group for Lambda integration
- **Kept**: ECS security group for reference

##### terraform/modules/alb/
- **Modified**: Target group for Lambda
  - Changed target type from "ip" to "lambda"
  - Updated health check path to "/api/q/health"
- **Added**: Lambda permission for ALB invocation
- **Added**: Target group attachment for Lambda

#### Main Configuration

##### terraform/main.tf
- **Replaced**: ECS module with Lambda module
- **Added**: Lambda function ARN to ALB module
- **Commented**: ECS module (kept for reference)

##### terraform/variables.tf
- **Added**: Lambda configuration variables
  - `lambda_memory_size` (default: 512 MB)
  - `lambda_timeout` (default: 30 seconds)
  - `lambda_enable_function_url` (default: false)
  - `lambda_provisioned_concurrent_executions` (default: 0)
  - `lambda_max_provisioned_concurrent_executions` (default: 10)
- **Kept**: ECS variables for reference

##### terraform/outputs.tf
- **Replaced**: ECS outputs with Lambda outputs
  - Lambda function ARN, name, URL
  - Lambda log group name
  - Lambda security group ID
- **Commented**: ECS outputs (kept for reference)

##### terraform/terraform.tfvars.example
- **Updated**: Lambda configuration examples
- **Commented**: ECS configuration examples

### 4. Deployment

#### terraform/deploy.sh
- **Purpose**: Completely rewritten for Lambda deployment
- **New Workflow**:
  1. Build native Lambda image using Dockerfile.lambda
  2. Login to ECR
  3. Tag and push image to ECR
  4. Update Lambda function with new image
  5. Wait for function update to complete
- **Old Workflow** (removed):
  - Build JVM JAR
  - Build Docker image
  - Update ECS service

### 5. Documentation

#### README.md
- **Updated**: Description to mention Lambda deployment
- **Added**: Lambda deployment architecture section
- **Added**: Cold start optimization details
- **Added**: Lambda configuration guidelines
- **Added**: Lambda monitoring metrics
- **Updated**: Build and deployment instructions
- **Updated**: Troubleshooting for Lambda-specific issues
- **Removed**: ECS-specific instructions

#### sdlc-plan.md
- **Updated**: Status to reflect Lambda conversion
- **Added**: Updated request section
- **Added**: Lambda conversion details
- **Added**: Benefits and migration notes
- **Updated**: Architecture overview
- **Updated**: Component description with Lambda details

## Architecture Comparison

### Before (ECS Fargate)

```
Internet → ALB → ECS Service (Fargate Tasks) → DynamoDB
                 ↓
           CloudWatch Logs
```

- **Pros**:
  - Predictable performance
  - Always warm
  - Familiar container deployment

- **Cons**:
  - Fixed cost (even with no traffic)
  - Manual scaling configuration
  - More complex infrastructure

### After (Lambda)

```
Internet → ALB → Lambda Function → DynamoDB
                  ↓
            CloudWatch Logs
```

- **Pros**:
  - Pay only for requests
  - Auto-scales from 0 to thousands
  - Simplified infrastructure
  - No server management

- **Cons**:
  - Potential cold starts (mitigated by native image)
  - 15-minute max execution time (more than sufficient for CRUD)
  - Learning curve for Lambda-specific concepts

## Performance Considerations

### Cold Starts
- **Native Image**: ~300-500ms first invocation
- **Warm Invocations**: ~5-50ms
- **Mitigation**: Can enable provisioned concurrency for guaranteed warm instances

### Memory Configuration
- **Default**: 512 MB
- **Recommendation**: Monitor CloudWatch metrics and adjust as needed
- **Impact**: More memory = more CPU power and higher cost

### Timeout
- **Default**: 30 seconds
- **Recommendation**: Sufficient for CRUD operations
- **ALB Limit**: 60 seconds maximum

## Cost Comparison (Example)

### ECS Fargate (Previous)
- 2 tasks running 24/7
- 0.5 vCPU, 1 GB memory per task
- **Cost**: ~$30-40/month (even with zero traffic)

### Lambda (Current)
- 512 MB memory, 30 second timeout
- Average 50ms duration per request
- **Example Costs**:
  - 1,000 requests/month: ~$0.20
  - 10,000 requests/month: ~$2.00
  - 100,000 requests/month: ~$20.00
  - 1,000,000 requests/month: ~$200.00

**Note**: Costs vary by region and configuration. Use AWS Pricing Calculator for accurate estimates.

## Testing Impact

- **Unit Tests**: No changes required
- **Integration Tests**: No changes required
- **API Contract**: No changes required
- **Reason**: Application code unchanged, Quarkus Lambda extension handles integration

## Rollback Plan

If rollback to ECS is needed:

1. Uncomment ECS module in `terraform/main.tf`
2. Comment out Lambda module
3. Revert ALB module changes to use ECS target group
4. Run `terraform apply`
5. Update deployment script to use original ECS workflow
6. Remove Lambda configuration from application.properties

All ECS code is preserved (commented) in the repository for easy rollback.

## Monitoring and Observability

### CloudWatch Metrics
- **Duration**: Execution time per invocation
- **Errors**: Failed invocations
- **Throttles**: Requests rejected due to concurrency limits
- **ConcurrentExecutions**: Number of simultaneous executions
- **ProvisionedConcurrencyUtilization**: If provisioned concurrency enabled

### CloudWatch Logs
- Function execution logs at `/aws/lambda/{function-name}`
- Same application logging as before

### X-Ray Tracing
- Enabled by default
- Provides distributed tracing across Lambda, DynamoDB, and other services

### Alarms (Configured)
1. **Lambda Errors**: Triggers if > 5 errors in 1 minute
2. **Lambda Throttles**: Triggers if > 5 throttles in 1 minute
3. **Lambda Duration**: Triggers if average duration > 80% of timeout

## Security

### IAM Permissions
- Lambda execution role has minimal required permissions:
  - CloudWatch Logs (write)
  - VPC networking (ENI management)
  - DynamoDB (CRUD operations)
  - X-Ray (tracing)

### Network Security
- Lambda runs in private subnets
- No public IP assigned
- Access to DynamoDB via VPC endpoint or NAT Gateway
- Security group allows only necessary egress traffic

### Secrets Management
- Environment variables for configuration
- AWS Systems Manager Parameter Store for secrets (if needed)
- No hardcoded credentials

## Operational Considerations

### Deployment
- **Build Time**: 5-10 minutes for native image
- **Deployment Time**: 1-2 minutes for Lambda update
- **Zero Downtime**: Lambda handles version transitions automatically

### Scaling
- **Automatic**: Lambda scales based on incoming requests
- **Burst Concurrency**: 500-3000 (region-dependent)
- **Account Limits**: Can be increased via AWS Support

### Maintenance
- **No Patching**: AWS manages runtime and security patches
- **Container Updates**: Only when application changes

## Best Practices Implemented

1. **Native Compilation**: Reduces cold start time significantly
2. **VPC Configuration**: Secure access to DynamoDB
3. **CloudWatch Integration**: Comprehensive logging and monitoring
4. **X-Ray Tracing**: Distributed tracing for debugging
5. **IAM Least Privilege**: Minimal necessary permissions
6. **Provisioned Concurrency**: Available if needed for consistent latency
7. **CloudWatch Alarms**: Proactive monitoring and alerting

## Next Steps

1. **Test Deployment**: Deploy to dev environment and test thoroughly
2. **Performance Testing**: Validate cold start times and throughput
3. **Cost Monitoring**: Track actual costs vs. projections
4. **Optimize Memory**: Adjust Lambda memory based on metrics
5. **Consider Provisioned Concurrency**: If consistent low latency is required
6. **Production Rollout**: Gradual rollout to production with monitoring

## References

- [Quarkus Amazon Lambda HTTP](https://quarkus.io/guides/amazon-lambda-http)
- [AWS Lambda Container Images](https://docs.aws.amazon.com/lambda/latest/dg/images-create.html)
- [Lambda with Application Load Balancer](https://docs.aws.amazon.com/lambda/latest/dg/services-alb.html)
- [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)

## Contact

For questions about this migration, please contact the development team.
