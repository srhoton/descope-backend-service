# Infrastructure Overview

## Summary

This Terraform infrastructure deploys a production-ready, highly available Unit Management Service on AWS using best practices for security, scalability, and cost optimization.

## Infrastructure Components

### Networking (VPC Module)
- **Custom VPC** with configurable CIDR block (default: 10.0.0.0/16)
- **Multi-AZ deployment** across 2+ availability zones
- **Public subnets** for load balancer (10.0.101.0/24, 10.0.102.0/24)
- **Private subnets** for ECS tasks (10.0.1.0/24, 10.0.2.0/24)
- **Internet Gateway** for public subnet internet access
- **NAT Gateways** for private subnet outbound connectivity
- **VPC Flow Logs** for network traffic monitoring
- **Route tables** configured for proper traffic routing

### Security (Security Module)
- **ALB Security Group**:
  - Ingress: HTTP (80), HTTPS (443) from internet
  - Egress: Port 8080 to ECS tasks
- **ECS Security Group**:
  - Ingress: Port 8080 from ALB only
  - Egress: HTTPS (443) for AWS API calls
- **VPC Endpoints Security Group**:
  - For secure access to AWS services
- **Least-privilege principle** applied to all security groups

### Load Balancer (ALB Module)
- **Application Load Balancer**:
  - Internet-facing
  - Cross-zone load balancing enabled
  - HTTP/2 enabled
  - Optional HTTPS with ACM certificate
- **Target Group**:
  - Health checks on `/q/health`
  - Interval: 30 seconds
  - Healthy threshold: 2
  - Unhealthy threshold: 3
  - Deregistration delay: 30 seconds
- **CloudWatch Alarms**:
  - High response time (>1 second)
  - Unhealthy targets (>0)
  - 5XX errors (>10 in 5 minutes)

### Container Service (ECS Module)
- **ECS Fargate Cluster**:
  - Container Insights enabled
  - Execute Command enabled for debugging
- **Task Definition**:
  - Network mode: awsvpc
  - Configurable CPU and memory
  - Container health checks
  - CloudWatch Logs integration
- **ECS Service**:
  - Fargate launch type
  - Multi-AZ deployment
  - Rolling deployments
  - Circuit breaker with automatic rollback
  - Health check grace period: 60 seconds
- **Auto-scaling**:
  - CPU-based scaling (target: 70%)
  - Memory-based scaling (target: 80%)
  - Configurable min/max capacity
- **CloudWatch Alarms**:
  - High CPU utilization (>85%)
  - High memory utilization (>90%)
  - Low task count (<min_capacity)

### Database (DynamoDB Module)
- **DynamoDB Table**:
  - Primary key: `id` (String)
  - Billing mode: On-demand or Provisioned
  - DynamoDB Streams enabled
  - Point-in-time recovery enabled
  - KMS encryption at rest
- **Auto-scaling** (if Provisioned mode):
  - Read capacity: 70% target utilization
  - Write capacity: 70% target utilization
- **CloudWatch Alarms**:
  - Read throttle events (>10)
  - Write throttle events (>10)

### Container Registry (ECR Module)
- **ECR Repository**:
  - AES256 encryption
  - Image scanning on push
  - Lifecycle policy:
    - Keep last 10 tagged images
    - Expire untagged images after 7 days
- **Repository Policy**:
  - ECS task pull permissions

### IAM (IAM Module)
- **Task Execution Role**:
  - ECR pull permissions
  - CloudWatch Logs write permissions
  - ECS managed policy attached
- **Task Role**:
  - DynamoDB table access (CRUD operations)
  - CloudWatch Logs write permissions
  - X-Ray tracing permissions
  - SSM Parameter Store read permissions
- **Least-privilege policies** for all operations

## File Structure

```
terraform/
├── main.tf                          # Root module orchestration
├── variables.tf                     # Input variables with validation
├── outputs.tf                       # Output values
├── terraform.tfvars.example         # Example configuration
├── .gitignore                       # Git ignore patterns
├── README.md                        # Usage documentation
├── deploy.sh                        # Deployment automation script
└── modules/
    ├── vpc/                         # Networking module
    │   ├── main.tf                  # VPC, subnets, routing
    │   ├── variables.tf
    │   └── outputs.tf
    ├── security/                    # Security groups module
    │   ├── main.tf                  # Security group rules
    │   ├── variables.tf
    │   └── outputs.tf
    ├── dynamodb/                    # Database module
    │   ├── main.tf                  # DynamoDB table, KMS
    │   ├── variables.tf
    │   └── outputs.tf
    ├── ecr/                         # Container registry module
    │   ├── main.tf                  # ECR repository, policies
    │   ├── variables.tf
    │   └── outputs.tf
    ├── iam/                         # IAM roles module
    │   ├── main.tf                  # Roles, policies
    │   ├── variables.tf
    │   └── outputs.tf
    ├── alb/                         # Load balancer module
    │   ├── main.tf                  # ALB, target groups, listeners
    │   ├── variables.tf
    │   └── outputs.tf
    └── ecs/                         # Container service module
        ├── main.tf                  # Cluster, service, task definition
        ├── variables.tf
        └── outputs.tf
```

## Key Features

### High Availability
- Multi-AZ deployment across 2+ availability zones
- Load balancer distributes traffic across healthy targets
- Auto-healing: failed tasks automatically replaced
- Circuit breaker prevents bad deployments

### Security
- **Network Security**:
  - Private subnets for application tier
  - Security groups with least-privilege rules
  - No direct internet access for ECS tasks
- **Data Security**:
  - DynamoDB encryption at rest with KMS
  - In-transit encryption via HTTPS
  - Optional ALB HTTPS with ACM certificates
- **Access Control**:
  - IAM roles with minimal required permissions
  - No hardcoded credentials
  - Support for AWS Secrets Manager/Parameter Store

### Scalability
- **Horizontal scaling**:
  - ECS auto-scaling based on CPU/memory
  - Configurable min/max capacity
- **Database scaling**:
  - DynamoDB on-demand (automatic)
  - Or provisioned with auto-scaling
- **Load balancing**:
  - ALB automatically distributes traffic
  - Cross-zone load balancing enabled

### Monitoring
- **CloudWatch Logs**:
  - Application logs
  - VPC Flow Logs
  - Configurable retention
- **CloudWatch Alarms**:
  - ECS metrics (CPU, memory, task count)
  - ALB metrics (response time, errors, health)
  - DynamoDB metrics (throttling)
- **Container Insights**:
  - Detailed ECS metrics
  - Performance monitoring

### Cost Optimization
- **Flexible configurations**:
  - Single NAT Gateway option for dev/staging
  - Configurable ECS task sizes
  - Right-sized container resources
- **Resource efficiency**:
  - Fargate only charges for running time
  - DynamoDB on-demand pricing option
  - Lifecycle policies for ECR images
- **Auto-scaling**:
  - Scale down during low traffic
  - Scale up during high traffic

## Resource Count

When fully deployed, this infrastructure creates approximately:

- **Networking**: 15-20 resources
  - 1 VPC
  - 4 Subnets (2 public, 2 private)
  - 1 Internet Gateway
  - 1-2 NAT Gateways
  - 3-4 Route Tables
  - Route table associations
  - VPC Flow Logs

- **Security**: 3 Security Groups
  - ALB security group
  - ECS security group
  - VPC endpoints security group

- **Compute**: 10-15 resources
  - 1 ECS Cluster
  - 1 ECS Service
  - 1 Task Definition
  - 2+ ECS Tasks (based on desired count)
  - Auto-scaling policies

- **Load Balancing**: 5-7 resources
  - 1 Application Load Balancer
  - 1 Target Group
  - 2 Listeners (HTTP, optionally HTTPS)

- **Database**: 3-5 resources
  - 1 DynamoDB Table
  - 1 KMS Key
  - Auto-scaling policies (if provisioned mode)

- **IAM**: 4-6 resources
  - 2 IAM Roles (task execution, task)
  - 4-6 IAM Policies

- **Monitoring**: 10-15 resources
  - CloudWatch Log Groups
  - CloudWatch Alarms

- **Container Registry**: 2-3 resources
  - 1 ECR Repository
  - Lifecycle policies

**Total**: ~50-70 AWS resources

## Environment Variables in ECS Task

The following environment variables are automatically configured:

| Variable | Value | Source |
|----------|-------|--------|
| `AWS_REGION` | Configured region | Terraform variable |
| `dynamodb.table.units` | Table name | Terraform variable |
| `JAVA_OPTS` | JVM settings | Task definition |

Additional variables can be added via:
- Task definition environment section
- AWS Systems Manager Parameter Store
- AWS Secrets Manager

## Outputs

After deployment, Terraform provides these outputs:

| Output | Description | Usage |
|--------|-------------|-------|
| `alb_dns_name` | Load balancer DNS | Access application |
| `api_endpoint` | Full API URL | API testing |
| `health_check_endpoint` | Health check URL | Monitoring |
| `ecr_repository_url` | ECR URL | Push Docker images |
| `ecs_cluster_name` | Cluster name | AWS CLI operations |
| `ecs_service_name` | Service name | Deployments |
| `dynamodb_table_name` | Table name | Database access |
| `vpc_id` | VPC ID | Network configuration |
| `cloudwatch_log_group` | Log group name | Log viewing |

## Dependencies Between Modules

```
┌─────────┐
│   VPC   │
└────┬────┘
     │
     ├──────────────┬──────────────┬──────────────┐
     │              │              │              │
┌────▼────┐   ┌────▼────┐   ┌────▼────┐   ┌────▼────┐
│Security │   │  ALB    │   │   ECS   │   │DynamoDB │
└────┬────┘   └────┬────┘   └────┬────┘   └────┬────┘
     │              │              │              │
     │              │         ┌────▼────┐         │
     └──────────────┴─────────│   IAM   │─────────┘
                              └─────────┘
                                   │
                              ┌────▼────┐
                              │   ECR   │
                              └─────────┘
```

## Best Practices Implemented

### Terraform Best Practices
- ✅ Module-based architecture for reusability
- ✅ Input validation for all variables
- ✅ Descriptive variable names and documentation
- ✅ Snake_case naming convention
- ✅ Common tags on all resources
- ✅ Output values for important resources
- ✅ .gitignore for sensitive files
- ✅ Formatted with `terraform fmt`

### AWS Best Practices
- ✅ Multi-AZ deployment for high availability
- ✅ Private subnets for application tier
- ✅ Least-privilege IAM policies
- ✅ Encryption at rest and in transit
- ✅ CloudWatch monitoring and alarms
- ✅ Auto-scaling for compute and database
- ✅ Health checks for all services
- ✅ VPC Flow Logs for security monitoring
- ✅ Resource tagging for organization
- ✅ Lifecycle policies for cost optimization

### Security Best Practices
- ✅ Security groups with minimal required access
- ✅ No public IP addresses on ECS tasks
- ✅ KMS encryption for DynamoDB
- ✅ IAM roles instead of access keys
- ✅ Container image scanning enabled
- ✅ VPC endpoints for AWS services (configurable)
- ✅ Deletion protection for critical resources
- ✅ Point-in-time recovery for database

## Next Steps

1. **Review Configuration**: Examine `terraform.tfvars.example` and customize for your needs
2. **Initialize**: Run `terraform init` to download providers and modules
3. **Plan**: Run `terraform plan` to preview changes
4. **Deploy**: Run `terraform apply` to create infrastructure
5. **Build & Push**: Use `deploy.sh` to build and deploy application
6. **Monitor**: Check CloudWatch Logs and Alarms
7. **Scale**: Adjust variables as needed and re-apply

## Support Resources

- **Terraform Documentation**: [terraform/README.md](./README.md)
- **Deployment Guide**: [../DEPLOYMENT.md](../DEPLOYMENT.md)
- **Application README**: [../README.md](../README.md)
- **AWS ECS Best Practices**: https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/
- **Terraform AWS Provider**: https://registry.terraform.io/providers/hashicorp/aws/latest/docs

## Maintenance

### Regular Updates
- Keep Terraform provider versions updated
- Review and update security group rules
- Monitor CloudWatch Alarms
- Review auto-scaling metrics
- Update container images regularly

### Cost Review
- Monitor AWS Cost Explorer
- Review DynamoDB usage patterns
- Evaluate NAT Gateway usage
- Check CloudWatch Logs retention
- Review ECR image count

### Security Audits
- Review IAM policies quarterly
- Update security groups as needed
- Rotate access keys (if used)
- Review VPC Flow Logs
- Scan container images
