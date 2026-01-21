# Lambda Deployment Quick Start

## Prerequisites

- AWS CLI configured
- Docker installed and running (8GB+ RAM recommended)
- Terraform installed
- Java 17+
- Gradle (included via wrapper)

## Quick Deployment

### 1. Initialize Terraform

```bash
cd terraform
terraform init
```

### 2. Configure Variables

```bash
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your settings
```

**Key Variables to Set:**
```hcl
environment = "dev"           # or "staging", "prod"
aws_region  = "us-east-1"     # your preferred region

# Lambda configuration
lambda_memory_size = 512      # MB (128-10240)
lambda_timeout     = 30       # seconds (1-900)

# Optional: Enable for consistent low latency
lambda_provisioned_concurrent_executions = 0  # Set > 0 to enable
```

### 3. Deploy Infrastructure

```bash
terraform plan   # Review changes
terraform apply  # Deploy
```

This creates:
- VPC with public/private subnets
- Application Load Balancer
- Lambda function (placeholder)
- DynamoDB table
- ECR repository
- All necessary IAM roles and security groups

### 4. Build and Deploy Application

```bash
cd ..  # Back to project root
./terraform/deploy.sh
```

This will:
1. Build native Lambda image (~5-10 minutes)
2. Push to ECR
3. Update Lambda function
4. Wait for update to complete

### 5. Test the Deployment

Get the ALB DNS name:
```bash
cd terraform
terraform output alb_dns_name
```

Test the API:
```bash
ALB_DNS=$(cd terraform && terraform output -raw alb_dns_name)

# Health check
curl http://$ALB_DNS/api/q/health

# Create a unit
curl -X POST http://$ALB_DNS/api/units \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Unit"}'

# Get all units
curl http://$ALB_DNS/api/units
```

## Development Workflow

### Local Development

Run in development mode (non-Lambda):
```bash
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=your-key
export AWS_SECRET_ACCESS_KEY=your-secret

./gradlew quarkusDev
```

Access locally at: http://localhost:8080/api

### Run Tests

```bash
# All tests
./gradlew test

# Unit tests only
./gradlew test --tests '*Test'

# Integration tests only
./gradlew test --tests '*IntegrationTest'
```

### Code Formatting

```bash
# Check formatting
./gradlew spotlessCheck

# Apply formatting
./gradlew spotlessApply
```

### Deploy Updates

After making code changes:
```bash
./terraform/deploy.sh [optional-tag]
```

## Monitoring

### CloudWatch Logs

```bash
# Stream Lambda logs
aws logs tail /aws/lambda/unit-management-dev-function --follow

# Recent errors
aws logs filter-events \
  --log-group-name /aws/lambda/unit-management-dev-function \
  --filter-pattern "ERROR"
```

### Lambda Metrics

```bash
# Recent invocations
aws lambda get-function \
  --function-name unit-management-dev-function

# View metrics in AWS Console
https://console.aws.amazon.com/cloudwatch/
```

### X-Ray Traces

View distributed traces in AWS X-Ray console to debug performance issues.

## Troubleshooting

### Lambda Function Not Responding

```bash
# Check Lambda function status
aws lambda get-function-configuration \
  --function-name unit-management-dev-function

# View recent errors
aws logs tail /aws/lambda/unit-management-dev-function \
  --since 10m --filter-pattern "ERROR"
```

### Health Check Failing

```bash
# Test health endpoint directly
curl -v http://$ALB_DNS/api/q/health

# Check ALB target health
aws elbv2 describe-target-health \
  --target-group-arn $(cd terraform && terraform output -raw alb_target_group_arn)
```

### DynamoDB Access Issues

```bash
# Verify table exists
aws dynamodb describe-table --table-name units-table

# Check IAM permissions
aws iam get-role-policy \
  --role-name unit-management-dev-lambda-execution-role \
  --policy-name unit-management-dev-lambda-dynamodb-access-policy
```

### Cold Start Performance

Monitor Duration metric in CloudWatch. If cold starts are an issue:

1. Increase memory (more CPU):
   ```hcl
   lambda_memory_size = 1024  # in terraform.tfvars
   ```

2. Enable provisioned concurrency:
   ```hcl
   lambda_provisioned_concurrent_executions = 2  # Keep 2 warm
   ```

3. Apply changes:
   ```bash
   cd terraform
   terraform apply
   ```

## Cost Optimization

### For Development

```hcl
# terraform.tfvars
single_nat_gateway = true                     # Use single NAT (saves ~$30/month)
lambda_provisioned_concurrent_executions = 0  # No provisioned concurrency
log_retention_days = 7                        # Short log retention
```

### For Production

```hcl
# terraform.tfvars
single_nat_gateway = false                    # Multi-AZ NAT for HA
lambda_provisioned_concurrent_executions = 2  # Keep instances warm
log_retention_days = 30                       # Longer log retention
```

## Useful Commands

### Terraform

```bash
# View current state
terraform show

# View outputs
terraform output

# Destroy everything (careful!)
terraform destroy
```

### AWS CLI

```bash
# Update function code manually
aws lambda update-function-code \
  --function-name unit-management-dev-function \
  --image-uri <ecr-url>:latest

# Invoke function directly (bypass ALB)
aws lambda invoke \
  --function-name unit-management-dev-function \
  --payload '{"path":"/api/q/health"}' \
  response.json
```

### Docker

```bash
# Build native image locally
docker build -f Dockerfile.lambda -t unit-management:latest .

# Run native image locally (for testing)
docker run -p 9000:8080 unit-management:latest
```

## Production Checklist

Before deploying to production:

- [ ] Update `environment = "prod"` in terraform.tfvars
- [ ] Set `alb_enable_deletion_protection = true`
- [ ] Configure HTTPS with `alb_certificate_arn`
- [ ] Increase `log_retention_days` to 30 or 90
- [ ] Consider enabling provisioned concurrency
- [ ] Set up CloudWatch alarms with SNS notifications
- [ ] Configure backup for DynamoDB table
- [ ] Review and adjust Lambda memory and timeout
- [ ] Set up WAF rules for ALB (if needed)
- [ ] Enable access logging for ALB
- [ ] Test disaster recovery procedures

## Support

- **Documentation**: See README.md and LAMBDA_MIGRATION.md
- **Issues**: Check CloudWatch Logs first
- **AWS Documentation**: https://docs.aws.amazon.com/lambda/

## Common Patterns

### Update Single Configuration

```bash
cd terraform
terraform apply -var="lambda_memory_size=1024"
```

### Change Log Retention

```bash
cd terraform
terraform apply -var="log_retention_days=30"
```

### Enable Function URL (Direct Lambda Access)

```bash
cd terraform
terraform apply -var="lambda_enable_function_url=true"
```

Then access via:
```bash
FUNCTION_URL=$(terraform output -raw lambda_function_url)
curl $FUNCTION_URL/api/q/health
```

## Performance Tuning

### Memory vs Cost vs Performance

| Memory | vCPU | Cold Start | Cost/1M Requests* |
|--------|------|------------|-------------------|
| 128 MB | 0.08 | ~800ms     | ~$2               |
| 512 MB | 0.33 | ~400ms     | ~$8               |
| 1024 MB| 0.67 | ~300ms     | ~$16              |
| 2048 MB| 1.33 | ~200ms     | ~$32              |

*Approximate, assumes 50ms average duration

**Recommendation**: Start with 512 MB, monitor, and adjust based on Duration metrics.

## Next Steps

1. **Monitor Performance**: Watch CloudWatch metrics for 1-2 weeks
2. **Optimize**: Adjust memory/timeout based on actual usage
3. **Scale**: Enable provisioned concurrency if needed
4. **Secure**: Add WAF, configure HTTPS, set up proper alerts
5. **Backup**: Configure DynamoDB point-in-time recovery for production
