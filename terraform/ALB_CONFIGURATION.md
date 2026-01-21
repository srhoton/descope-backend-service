# ALB Configuration - Using Existing Application Load Balancer

## Overview

This Terraform configuration has been designed to use an **existing Application Load Balancer** instead of creating a new one. This approach allows multiple services to share the same ALB while maintaining isolation through separate target groups and listener rules.

## Configuration Details

### Default Existing ALB ARN

```
arn:aws:elasticloadbalancing:us-west-2:345594586248:loadbalancer/app/external-private-alb/720e2b5474d3d602
```

This can be overridden by setting the `existing_alb_arn` variable in your terraform.tfvars file or via environment variables.

## What Gets Created

Even though we use an existing ALB, the following resources are still created:

1. **Target Group** - A Lambda target group specific to this service
2. **Listener Rules** - Rules attached to the existing ALB's listeners to route traffic to our target group
3. **Lambda Permissions** - Allows the ALB to invoke our Lambda function
4. **Target Group Attachment** - Attaches the Lambda function to the target group
5. **CloudWatch Alarms** - Monitoring for the target group

## What Does NOT Get Created

1. **Application Load Balancer** - Uses existing ALB via data source
2. **ALB Listeners** - Uses existing listeners on the ALB
3. **ALB Security Groups** - Managed separately for the existing ALB

## Architecture

```
┌─────────────────────────────────────┐
│   Existing ALB                      │
│   (external-private-alb)            │
│                                     │
│   ┌─────────────────────────────┐  │
│   │  HTTP Listener (Port 80)    │  │
│   │  ┌──────────────────────┐   │  │
│   │  │  Listener Rule       │   │  │
│   │  │  Priority: 100       │───┼──┼──> Target Group
│   │  │  Path: /api/*        │   │  │    (Lambda)
│   │  └──────────────────────┘   │  │
│   └─────────────────────────────┘  │
│                                     │
│   ┌─────────────────────────────┐  │
│   │  HTTPS Listener (Port 443)  │  │
│   │  ┌──────────────────────┐   │  │
│   │  │  Listener Rule       │   │  │
│   │  │  Priority: 100       │───┼──┼──> Target Group
│   │  │  Path: /api/*        │   │  │    (Lambda)
│   │  └──────────────────────┘   │  │
│   └─────────────────────────────┘  │
└─────────────────────────────────────┘
                  │
                  │
                  ▼
        ┌──────────────────┐
        │  Lambda Function │
        │  (Unit Service)  │
        └──────────────────┘
```

## Key Configuration Variables

### Required Variables

- `existing_alb_arn` - ARN of the existing Application Load Balancer
  - **Default**: `arn:aws:elasticloadbalancing:us-west-2:345594586248:loadbalancer/app/external-private-alb/720e2b5474d3d602`
  - **Type**: string
  - **Validation**: Must be a valid ALB ARN format

### Optional Variables

- `listener_priority` - Priority for the listener rule (1-50000)
  - **Default**: `100`
  - **Type**: number
  - **Note**: Must be unique across all rules on the listener

- `path_pattern` - Path pattern for routing to the Lambda function
  - **Default**: `["/api/*"]`
  - **Type**: list(string)
  - **Note**: All requests matching this pattern will be routed to the Lambda

- `alb_certificate_arn` - ARN of ACM certificate for HTTPS listener
  - **Default**: `""`
  - **Type**: string
  - **Note**: If provided, HTTPS listener rule will be created

## Usage

### Basic Usage

```hcl
module "alb" {
  source = "./modules/alb"

  name_prefix         = "my-service-dev"
  existing_alb_arn    = var.existing_alb_arn
  vpc_id              = module.vpc.vpc_id
  lambda_function_arn = module.lambda.lambda_function_arn

  tags = {
    Environment = "dev"
    Service     = "my-service"
  }
}
```

### Custom Listener Priority

If you have multiple services sharing the same ALB, ensure each service uses a unique listener priority:

```hcl
module "alb" {
  source = "./modules/alb"

  name_prefix         = "my-service-dev"
  existing_alb_arn    = var.existing_alb_arn
  vpc_id              = module.vpc.vpc_id
  lambda_function_arn = module.lambda.lambda_function_arn
  listener_priority   = 200  # Custom priority

  tags = local.common_tags
}
```

### Custom Path Pattern

Route specific paths to your Lambda function:

```hcl
module "alb" {
  source = "./modules/alb"

  name_prefix         = "my-service-dev"
  existing_alb_arn    = var.existing_alb_arn
  vpc_id              = module.vpc.vpc_id
  lambda_function_arn = module.lambda.lambda_function_arn
  path_pattern        = ["/api/v1/*", "/api/v2/*"]  # Multiple patterns

  tags = local.common_tags
}
```

## Outputs

The module provides the following outputs:

- `alb_arn` - ARN of the existing Application Load Balancer
- `alb_dns_name` - DNS name of the existing ALB
- `alb_zone_id` - Route53 zone ID for the ALB
- `target_group_arn` - ARN of the created target group
- `target_group_name` - Name of the target group
- `http_listener_arn` - ARN of the HTTP listener
- `https_listener_arn` - ARN of the HTTPS listener (if configured)
- `http_listener_rule_arn` - ARN of the HTTP listener rule
- `https_listener_rule_arn` - ARN of the HTTPS listener rule (if configured)

## Important Considerations

### 1. Listener Rule Priority Conflicts

Each listener rule must have a unique priority within the same listener. If you deploy multiple services to the same ALB, coordinate the priorities:

- Service A: Priority 100
- Service B: Priority 200
- Service C: Priority 300

### 2. Path Pattern Conflicts

Ensure path patterns don't overlap between services. More specific patterns should have higher priority (lower number):

- `/api/units/*` - Priority 100 (more specific)
- `/api/*` - Priority 200 (more general)

### 3. Target Group Health Checks

The Lambda target group uses the following health check configuration:

- **Path**: `/api/q/health`
- **Interval**: 30 seconds
- **Timeout**: 5 seconds
- **Healthy Threshold**: 2
- **Unhealthy Threshold**: 3
- **Matcher**: 200

Ensure your Lambda function responds to health checks at this path.

### 4. Security Groups

The existing ALB's security groups are managed separately. Ensure they allow:

- Inbound HTTP (port 80) and HTTPS (port 443) from appropriate sources
- Outbound traffic to invoke Lambda functions (port 443)

### 5. ALB Limits

AWS has limits on the number of rules per listener (default: 100). Monitor your usage if deploying many services to the same ALB.

## Monitoring

The module creates CloudWatch alarms for:

1. **Target Response Time** - Alert if response time > 1 second
2. **Unhealthy Target Count** - Alert if any targets are unhealthy
3. **5XX Error Count** - Alert if more than 10 5XX errors in 5 minutes

## Troubleshooting

### Issue: Listener rule priority conflict

**Error**: `A listener rule with priority X already exists`

**Solution**: Change the `listener_priority` variable to a unique value.

### Issue: No traffic reaching Lambda

**Symptoms**: ALB responds but Lambda not invoked

**Check**:
1. Listener rule path pattern matches your requests
2. Lambda permission allows ALB invocation
3. Target group attachment is successful
4. Lambda is in correct subnets with network access

### Issue: Health checks failing

**Symptoms**: Target group shows unhealthy targets

**Check**:
1. Lambda responds to `/api/q/health` with 200 status
2. Lambda execution time < 5 seconds (health check timeout)
3. Lambda has necessary permissions and network access

## Migration from Creating ALB to Using Existing

If you previously had Terraform creating the ALB, follow these steps:

1. **Export existing ALB state** (if managed by Terraform):
   ```bash
   terraform state show module.alb.aws_lb.main
   ```

2. **Remove ALB from state** (if you want to manage it separately):
   ```bash
   terraform state rm module.alb.aws_lb.main
   terraform state rm module.alb.aws_lb_listener.http
   terraform state rm module.alb.aws_lb_listener.https
   ```

3. **Update variables** with existing ALB ARN

4. **Apply the new configuration**:
   ```bash
   terraform plan
   terraform apply
   ```

## Cost Implications

Using an existing ALB instead of creating a new one provides cost savings:

- **ALB Cost**: ~$16.20/month per ALB (eliminated if shared)
- **LCU Cost**: Shared across all services using the ALB
- **Target Group**: Minimal cost
- **Listener Rules**: No additional cost

For multiple services, sharing an ALB can save significant costs compared to one ALB per service.

## Best Practices

1. **Use descriptive target group names** including service and environment
2. **Document listener rule priorities** in your infrastructure documentation
3. **Use path-based routing** to keep related APIs on the same path prefix
4. **Monitor CloudWatch alarms** for target group health
5. **Use separate listener priorities** for dev, staging, and production if sharing ALB
6. **Tag all resources** consistently for cost tracking and management
7. **Test health checks** before deploying to production

## Additional Resources

- [AWS ALB Documentation](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/)
- [Lambda Target Groups](https://docs.aws.amazon.com/lambda/latest/dg/services-alb.html)
- [ALB Listener Rules](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/listener-update-rules.html)
