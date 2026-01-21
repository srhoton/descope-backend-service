# Deployment Guide

This document provides comprehensive instructions for deploying the Unit Management Service to AWS.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                           AWS Cloud                                  │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                     VPC (10.0.0.0/16)                        │   │
│  │                                                               │   │
│  │  ┌─────────────────────┐      ┌──────────────────────┐     │   │
│  │  │   Public Subnet     │      │   Public Subnet      │     │   │
│  │  │   (10.0.101.0/24)   │      │  (10.0.102.0/24)     │     │   │
│  │  │   AZ us-east-1a     │      │  AZ us-east-1b       │     │   │
│  │  │                     │      │                      │     │   │
│  │  │  ┌──────────────┐   │      │  ┌──────────────┐   │     │   │
│  │  │  │     ALB      │   │      │  │  NAT Gateway │   │     │   │
│  │  │  │  (Port 80)   │◄──┼──────┼──┤              │   │     │   │
│  │  │  └──────┬───────┘   │      │  └──────────────┘   │     │   │
│  │  │         │           │      │                      │     │   │
│  │  └─────────┼───────────┘      └──────────────────────┘     │   │
│  │            │                                                │   │
│  │            │                                                │   │
│  │  ┌─────────▼───────────┐      ┌──────────────────────┐     │   │
│  │  │  Private Subnet     │      │  Private Subnet      │     │   │
│  │  │  (10.0.1.0/24)      │      │  (10.0.2.0/24)       │     │   │
│  │  │  AZ us-east-1a      │      │  AZ us-east-1b       │     │   │
│  │  │                     │      │                      │     │   │
│  │  │  ┌──────────────┐   │      │  ┌──────────────┐   │     │   │
│  │  │  │  ECS Fargate │   │      │  │ ECS Fargate  │   │     │   │
│  │  │  │    Task 1    │   │      │  │   Task 2     │   │     │   │
│  │  │  │  Port 8080   │   │      │  │  Port 8080   │   │     │   │
│  │  │  └──────┬───────┘   │      │  └──────┬───────┘   │     │   │
│  │  │         │           │      │         │           │     │   │
│  │  └─────────┼───────────┘      └─────────┼───────────┘     │   │
│  │            │                            │                  │   │
│  └────────────┼────────────────────────────┼──────────────────┘   │
│               │                            │                      │
│               ├────────────────────────────┤                      │
│               │                            │                      │
│        ┌──────▼────────┐           ┌──────▼────────┐            │
│        │   DynamoDB    │           │  CloudWatch   │            │
│        │  units-table  │           │     Logs      │            │
│        │   (KMS Enc)   │           │               │            │
│        └───────────────┘           └───────────────┘            │
│                                                                   │
│        ┌───────────────┐                                         │
│        │      ECR      │                                         │
│        │   Container   │                                         │
│        │   Registry    │                                         │
│        └───────────────┘                                         │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘

     Internet
        ▲
        │
        │ HTTPS/HTTP
        │
     Users
```

## Prerequisites

### Required Tools

1. **AWS CLI** (version 2.x or higher)
   ```bash
   aws --version
   ```

2. **Terraform** (version 1.0 or higher)
   ```bash
   terraform version
   ```

3. **Docker** (for building images)
   ```bash
   docker --version
   ```

4. **Java 17** (for building application)
   ```bash
   java -version
   ```

5. **Gradle** (wrapper included)
   ```bash
   ./gradlew --version
   ```

### AWS Permissions

Your AWS user/role needs permissions to create:
- VPC, Subnets, Route Tables, Internet Gateway, NAT Gateway
- Security Groups
- Application Load Balancer and Target Groups
- ECS Cluster, Services, Task Definitions
- ECR Repository
- DynamoDB Tables
- IAM Roles and Policies
- CloudWatch Log Groups
- KMS Keys

## Deployment Steps

### Step 1: Configure AWS Credentials

```bash
aws configure
# Enter your AWS Access Key ID
# Enter your AWS Secret Access Key
# Enter your preferred region (e.g., us-east-1)
# Enter output format (json)
```

### Step 2: Initialize Terraform

```bash
cd terraform
terraform init
```

This will:
- Download required provider plugins
- Initialize the backend
- Prepare modules

### Step 3: Create Configuration

```bash
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars` with your desired configuration:

```hcl
# Example for development environment
environment = "dev"
aws_region  = "us-east-1"

# Cost optimization for dev
single_nat_gateway = true
ecs_desired_count  = 1
ecs_min_capacity   = 1

# DynamoDB on-demand pricing
dynamodb_billing_mode = "PAY_PER_REQUEST"
```

### Step 4: Review Infrastructure Plan

```bash
terraform plan
```

Review the output to understand what resources will be created.

### Step 5: Deploy Infrastructure

```bash
terraform apply
```

Type `yes` when prompted. This will take approximately 10-15 minutes.

### Step 6: Build and Push Docker Image

After infrastructure is deployed, use the deployment script:

```bash
cd ..
./terraform/deploy.sh
```

Or manually:

```bash
# Build application
./gradlew clean build

# Get ECR URL
ECR_URL=$(cd terraform && terraform output -raw ecr_repository_url && cd ..)

# Get AWS region
AWS_REGION=$(cd terraform && terraform output -raw aws_region && cd ..)

# Login to ECR
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin $ECR_URL

# Build Docker image
docker build -t unit-management-service:latest .

# Tag image
docker tag unit-management-service:latest $ECR_URL:latest

# Push to ECR
docker push $ECR_URL:latest
```

### Step 7: Update ECS Service

Force a new deployment to use the new image:

```bash
cd terraform

CLUSTER_NAME=$(terraform output -raw ecs_cluster_name)
SERVICE_NAME=$(terraform output -raw ecs_service_name)
AWS_REGION=$(terraform output -raw aws_region)

aws ecs update-service \
  --cluster $CLUSTER_NAME \
  --service $SERVICE_NAME \
  --force-new-deployment \
  --region $AWS_REGION
```

### Step 8: Verify Deployment

Get the ALB DNS name:

```bash
cd terraform
terraform output alb_dns_name
```

Test the health check endpoint:

```bash
ALB_DNS=$(cd terraform && terraform output -raw alb_dns_name && cd ..)
curl http://$ALB_DNS/q/health
```

Test the API:

```bash
# Create a unit
curl -X POST http://$ALB_DNS/api/units \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Unit"}'

# Get all units
curl http://$ALB_DNS/api/units
```

## Environment-Specific Configurations

### Development

```hcl
environment                    = "dev"
single_nat_gateway             = true
ecs_desired_count              = 1
ecs_min_capacity               = 1
ecs_max_capacity               = 4
alb_enable_deletion_protection = false
log_retention_days             = 7
dynamodb_billing_mode          = "PAY_PER_REQUEST"
```

### Staging

```hcl
environment                    = "staging"
single_nat_gateway             = false
ecs_desired_count              = 2
ecs_min_capacity               = 2
ecs_max_capacity               = 8
alb_enable_deletion_protection = true
log_retention_days             = 30
dynamodb_billing_mode          = "PAY_PER_REQUEST"
```

### Production

```hcl
environment                    = "prod"
single_nat_gateway             = false
ecs_desired_count              = 3
ecs_min_capacity               = 2
ecs_max_capacity               = 20
alb_enable_deletion_protection = true
log_retention_days             = 90
dynamodb_billing_mode          = "PAY_PER_REQUEST"
container_cpu                  = 1024
container_memory               = 2048
```

## Monitoring

### CloudWatch Logs

View application logs:

```bash
aws logs tail /ecs/<environment>/unit-management-<environment> --follow
```

### CloudWatch Metrics

The deployment includes alarms for:
- ECS CPU utilization > 85%
- ECS Memory utilization > 90%
- ALB unhealthy targets > 0
- ALB 5XX errors > 10
- ALB response time > 1 second
- DynamoDB read/write throttle events

View alarms in AWS Console:
```
CloudWatch → Alarms
```

### ECS Service Health

Check service status:

```bash
aws ecs describe-services \
  --cluster <cluster-name> \
  --services <service-name> \
  --region <region>
```

## Updating the Application

### Method 1: Using Deploy Script

```bash
./terraform/deploy.sh v1.0.1
```

### Method 2: Manual Update

1. Build new image with version tag
2. Push to ECR
3. Update Terraform variable
4. Apply changes

```bash
# Build and push
docker build -t unit-management-service:v1.0.1 .
docker tag unit-management-service:v1.0.1 $ECR_URL:v1.0.1
docker push $ECR_URL:v1.0.1

# Update terraform.tfvars
container_image_tag = "v1.0.1"

# Apply changes
cd terraform
terraform apply
```

## Rollback

To rollback to a previous version:

```bash
# Update image tag to previous version
cd terraform

# Edit terraform.tfvars
container_image_tag = "v1.0.0"

# Apply
terraform apply
```

## Scaling

### Manual Scaling

Update desired count in `terraform.tfvars`:

```hcl
ecs_desired_count = 5
```

Apply changes:

```bash
cd terraform
terraform apply
```

### Auto-Scaling

Auto-scaling is configured automatically:
- CPU > 70% triggers scale-out
- Memory > 80% triggers scale-out
- Scales between min_capacity and max_capacity

## Troubleshooting

### Tasks Not Starting

1. Check CloudWatch Logs:
   ```bash
   aws logs tail /ecs/<environment>/unit-management-<environment>
   ```

2. Verify ECR image exists:
   ```bash
   aws ecr describe-images --repository-name unit-management-<environment>
   ```

3. Check task execution role permissions

### ALB Health Checks Failing

1. Verify security group allows traffic from ALB to ECS
2. Check application is listening on port 8080
3. Verify health check endpoint `/q/health` returns 200
4. Check CloudWatch Logs for application errors

### DynamoDB Access Errors

1. Verify IAM task role has DynamoDB permissions
2. Check security group allows HTTPS egress
3. Verify table name matches configuration

### High Costs

For development environments:
- Use `single_nat_gateway = true`
- Reduce `ecs_desired_count` to 1
- Use smaller container sizes
- Reduce log retention

## Disaster Recovery

### Backup Strategy

- **DynamoDB**: Point-in-time recovery enabled automatically
- **ECR Images**: Lifecycle policy retains last 10 images
- **Terraform State**: Store in S3 with versioning enabled

### Restore Procedure

1. Infrastructure is reproducible from Terraform code
2. DynamoDB can be restored from point-in-time recovery
3. Container images available in ECR

## Security Best Practices

1. **Use HTTPS**: Provide ACM certificate ARN in `alb_certificate_arn`
2. **Enable WAF**: Add AWS WAF to ALB for additional protection
3. **Rotate Credentials**: Use IAM roles, not access keys
4. **Enable GuardDuty**: Monitor for threats
5. **Review Security Groups**: Follow principle of least privilege
6. **Enable CloudTrail**: Audit all API calls

## Clean Up

To destroy all infrastructure:

```bash
cd terraform
terraform destroy
```

**Warning**: This will delete:
- All ECS tasks and services
- Load balancer
- VPC and networking
- DynamoDB table (with data)
- ECR repository (with images)

Ensure you have backups before destroying production environments.

## Cost Estimation

Approximate monthly costs (us-east-1):

### Development (1 task)
- ECS Fargate (0.5 vCPU, 1GB): ~$15
- ALB: ~$22
- NAT Gateway (single): ~$32
- DynamoDB (on-demand, low traffic): ~$5
- Data transfer: ~$5
- **Total: ~$80/month**

### Production (3 tasks, multi-AZ)
- ECS Fargate (1 vCPU, 2GB) x 3: ~$120
- ALB: ~$22
- NAT Gateway (2): ~$64
- DynamoDB (on-demand): varies with usage
- Data transfer: varies with traffic
- CloudWatch: ~$10
- **Total: ~$220/month + usage**

## Support

For issues with deployment:

1. Check CloudWatch Logs
2. Review Terraform output
3. Verify AWS permissions
4. Check security group configurations
5. Review application configuration

## Additional Resources

- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [Quarkus on AWS](https://quarkus.io/guides/deploying-to-aws)
- [DynamoDB Best Practices](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html)
