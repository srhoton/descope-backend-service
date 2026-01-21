# Unit Management Service - Terraform Infrastructure

This directory contains Terraform configuration for deploying the Unit Management Service to AWS.

## Architecture Overview

The infrastructure includes:

- **VPC**: Custom VPC with public and private subnets across multiple availability zones
- **Application Load Balancer (ALB)**: Internet-facing load balancer for distributing traffic
- **ECS Fargate**: Containerized application deployment with auto-scaling
- **DynamoDB**: NoSQL database for storing units
- **ECR**: Container registry for Docker images
- **CloudWatch**: Logging and monitoring
- **Security Groups**: Network security controls
- **IAM Roles**: Least-privilege access policies

## Prerequisites

Before deploying, ensure you have:

1. **AWS CLI** configured with appropriate credentials
2. **Terraform** >= 1.0 installed
3. **Docker** image built and ready to push to ECR
4. **AWS Account** with appropriate permissions

## Quick Start

### 1. Initialize Terraform

```bash
cd terraform
terraform init
```

### 2. Configure Variables

Copy the example variables file and customize it:

```bash
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars` with your specific values:

```hcl
aws_region  = "us-east-1"
environment = "dev"
project_name = "unit-management"

# Customize other values as needed
```

### 3. Review the Plan

Preview the infrastructure changes:

```bash
terraform plan
```

### 4. Deploy Infrastructure

Apply the Terraform configuration:

```bash
terraform apply
```

Type `yes` when prompted to confirm.

### 5. Build and Push Docker Image

After infrastructure is deployed, build and push your Docker image:

```bash
# Get ECR login
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build the application (from project root)
cd ..
./gradlew build

# Build Docker image
docker build -t unit-management-dev:latest .

# Tag image
docker tag unit-management-dev:latest <ecr-repository-url>:latest

# Push to ECR
docker push <ecr-repository-url>:latest
```

### 6. Access the Application

After deployment completes, Terraform will output the ALB DNS name:

```bash
terraform output alb_dns_name
```

Access the API at:
- API: `http://<alb-dns-name>/api`
- Health Check: `http://<alb-dns-name>/q/health`

## Module Structure

```
terraform/
├── main.tf                    # Root module configuration
├── variables.tf               # Input variables
├── outputs.tf                 # Output values
├── terraform.tfvars.example   # Example variable values
├── .gitignore                 # Git ignore file
└── modules/
    ├── vpc/                   # VPC and networking
    ├── security/              # Security groups
    ├── dynamodb/              # DynamoDB table
    ├── ecr/                   # ECR repository
    ├── iam/                   # IAM roles and policies
    ├── alb/                   # Application Load Balancer
    └── ecs/                   # ECS cluster and service
```

## Configuration Variables

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `environment` | Environment name | `dev`, `staging`, `prod` |

### Networking Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `vpc_cidr` | VPC CIDR block | `10.0.0.0/16` |
| `availability_zones` | List of AZs | `["us-east-1a", "us-east-1b"]` |
| `private_subnet_cidrs` | Private subnet CIDRs | `["10.0.1.0/24", "10.0.2.0/24"]` |
| `public_subnet_cidrs` | Public subnet CIDRs | `["10.0.101.0/24", "10.0.102.0/24"]` |
| `enable_nat_gateway` | Enable NAT Gateway | `true` |
| `single_nat_gateway` | Use single NAT (cost savings) | `false` |

### DynamoDB Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `dynamodb_table_name` | Table name | `units-table` |
| `dynamodb_billing_mode` | Billing mode | `PAY_PER_REQUEST` |
| `dynamodb_read_capacity` | Read capacity (if PROVISIONED) | `5` |
| `dynamodb_write_capacity` | Write capacity (if PROVISIONED) | `5` |

### ECS Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `container_cpu` | CPU units (256, 512, 1024, etc.) | `512` |
| `container_memory` | Memory in MB | `1024` |
| `ecs_desired_count` | Desired task count | `2` |
| `ecs_min_capacity` | Min tasks for auto-scaling | `2` |
| `ecs_max_capacity` | Max tasks for auto-scaling | `10` |

### ALB Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `alb_certificate_arn` | ACM certificate ARN for HTTPS | `""` (HTTP only) |
| `alb_enable_deletion_protection` | Protect ALB from deletion | `true` |

## Outputs

After deployment, Terraform provides these outputs:

| Output | Description |
|--------|-------------|
| `alb_dns_name` | DNS name of the load balancer |
| `api_endpoint` | Full API endpoint URL |
| `health_check_endpoint` | Health check URL |
| `ecr_repository_url` | ECR repository URL for pushing images |
| `ecs_cluster_name` | Name of the ECS cluster |
| `dynamodb_table_name` | Name of the DynamoDB table |

View all outputs:

```bash
terraform output
```

## Environments

### Development

For development environments, you can reduce costs:

```hcl
single_nat_gateway             = true
ecs_desired_count              = 1
ecs_min_capacity               = 1
alb_enable_deletion_protection = false
dynamodb_billing_mode          = "PAY_PER_REQUEST"
```

### Production

For production environments, prioritize reliability:

```hcl
single_nat_gateway             = false
ecs_desired_count              = 3
ecs_min_capacity               = 2
ecs_max_capacity               = 20
alb_enable_deletion_protection = true
dynamodb_billing_mode          = "PAY_PER_REQUEST" # or PROVISIONED with higher capacity
log_retention_days             = 90
```

## Cost Optimization

To reduce costs in non-production environments:

1. **Use a single NAT Gateway** instead of one per AZ
2. **Reduce ECS task count** to minimum (1 for dev)
3. **Use smaller container sizes** (256 CPU, 512 MB memory)
4. **Reduce log retention** to 3-7 days
5. **Use DynamoDB on-demand pricing** instead of provisioned

## Security Considerations

### Network Security

- ECS tasks run in private subnets with no direct internet access
- ALB is the only public-facing component
- Security groups follow principle of least privilege
- VPC Flow Logs enabled for network monitoring

### Data Security

- DynamoDB encrypted at rest with KMS
- ECS task communication encrypted in transit
- ALB supports HTTPS with ACM certificates
- Secrets can be stored in AWS Systems Manager Parameter Store

### Access Control

- IAM roles follow least-privilege principle
- Task execution role for pulling images and writing logs
- Task role for accessing DynamoDB only
- No hardcoded credentials

## Monitoring and Alarms

The infrastructure includes CloudWatch alarms for:

- **ECS Service**: CPU and memory utilization, task count
- **ALB**: Response time, unhealthy targets, 5XX errors
- **DynamoDB**: Read/write throttle events

## Auto-Scaling

Auto-scaling is configured for:

### ECS Tasks

- **CPU-based**: Scales at 70% CPU utilization
- **Memory-based**: Scales at 80% memory utilization
- Scale-out cooldown: 60 seconds
- Scale-in cooldown: 300 seconds

### DynamoDB (if PROVISIONED mode)

- **Read capacity**: Scales at 70% utilization
- **Write capacity**: Scales at 70% utilization

## Disaster Recovery

### Backups

- **DynamoDB**: Point-in-time recovery enabled
- **ECS Tasks**: Stateless, can be recreated from images
- **Logs**: CloudWatch Logs retained per configuration

### High Availability

- Multi-AZ deployment across 2+ availability zones
- ALB distributes traffic across healthy targets
- ECS service maintains desired task count
- Automatic task replacement on failure

## Troubleshooting

### Common Issues

**Terraform initialization fails:**
```bash
rm -rf .terraform
terraform init
```

**Insufficient IAM permissions:**
- Ensure your AWS credentials have permissions to create VPCs, ECS, DynamoDB, etc.
- Review the error message for specific missing permissions

**ECS tasks fail to start:**
- Check CloudWatch Logs: `/ecs/<environment>/unit-management-<environment>`
- Verify ECR image exists and is accessible
- Check task execution role permissions
- Review security group rules

**Application not accessible:**
- Verify ALB target health in AWS Console
- Check security group rules allow traffic
- Ensure ECS tasks are running
- Review application logs in CloudWatch

### Useful Commands

```bash
# View current state
terraform show

# View specific output
terraform output alb_dns_name

# Refresh state
terraform refresh

# Destroy infrastructure
terraform destroy

# Format Terraform files
terraform fmt -recursive

# Validate configuration
terraform validate

# View logs
aws logs tail /ecs/<environment>/unit-management-<environment> --follow
```

## Maintenance

### Updating Infrastructure

1. Modify variables or module configurations
2. Run `terraform plan` to preview changes
3. Run `terraform apply` to apply changes

### Updating Application

1. Build new Docker image
2. Push to ECR with new tag
3. Update `container_image_tag` variable
4. Run `terraform apply` to deploy new image

### Rotating Secrets

If using secrets in Parameter Store:

1. Update secret in AWS Systems Manager
2. Restart ECS tasks to pick up new values:
   ```bash
   aws ecs update-service \
     --cluster <cluster-name> \
     --service <service-name> \
     --force-new-deployment
   ```

## Backend Configuration

For production use, configure remote state:

```hcl
terraform {
  backend "s3" {
    bucket         = "my-terraform-state-bucket"
    key            = "unit-management-service/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "terraform-state-lock"
  }
}
```

Create the S3 bucket and DynamoDB table first:

```bash
# Create S3 bucket
aws s3 mb s3://my-terraform-state-bucket --region us-east-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket my-terraform-state-bucket \
  --versioning-configuration Status=Enabled

# Create DynamoDB table for locking
aws dynamodb create-table \
  --table-name terraform-state-lock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

## Clean Up

To destroy all infrastructure:

```bash
# Preview what will be destroyed
terraform plan -destroy

# Destroy infrastructure
terraform destroy
```

**Warning**: This will delete all resources including the DynamoDB table. Make sure to backup any important data first.

## Support

For issues or questions:

1. Check CloudWatch Logs for application errors
2. Review Terraform state: `terraform show`
3. Verify AWS resource status in Console
4. Check security group and IAM policies

## License

Copyright (c) 2026 Descope. All rights reserved.
