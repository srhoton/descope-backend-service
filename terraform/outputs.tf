# Output values for Unit Management Service Infrastructure

output "vpc_id" {
  description = "ID of the VPC"
  value       = module.vpc.vpc_id
}

output "private_subnet_ids" {
  description = "IDs of private subnets"
  value       = module.vpc.private_subnet_ids
}

output "public_subnet_ids" {
  description = "IDs of public subnets"
  value       = module.vpc.public_subnet_ids
}

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = module.alb.alb_dns_name
}

output "alb_zone_id" {
  description = "Zone ID of the Application Load Balancer"
  value       = module.alb.alb_zone_id
}

output "alb_arn" {
  description = "ARN of the Application Load Balancer"
  value       = module.alb.alb_arn
}

output "alb_security_group_id" {
  description = "Security group ID for ALB"
  value       = module.security_groups.alb_security_group_id
}

output "lambda_function_arn" {
  description = "ARN of the Lambda function"
  value       = module.lambda.lambda_function_arn
}

output "lambda_function_name" {
  description = "Name of the Lambda function"
  value       = module.lambda.lambda_function_name
}

output "lambda_function_url" {
  description = "URL of the Lambda function (if enabled)"
  value       = module.lambda.lambda_function_url
}

output "lambda_security_group_id" {
  description = "Security group ID for Lambda function"
  value       = module.security_groups.lambda_security_group_id
}

output "lambda_log_group_name" {
  description = "Name of the CloudWatch log group for Lambda"
  value       = module.lambda.lambda_log_group_name
}

# ECS outputs (commented out - keeping for reference if needed)
# output "ecs_cluster_id" {
#   description = "ID of the ECS cluster"
#   value       = module.ecs.cluster_id
# }
#
# output "ecs_cluster_name" {
#   description = "Name of the ECS cluster"
#   value       = module.ecs.cluster_name
# }
#
# output "ecs_service_name" {
#   description = "Name of the ECS service"
#   value       = module.ecs.service_name
# }
#
# output "ecs_security_group_id" {
#   description = "Security group ID for ECS tasks"
#   value       = module.security_groups.ecs_security_group_id
# }

output "dynamodb_table_name" {
  description = "Name of the DynamoDB table"
  value       = module.dynamodb.table_name
}

output "dynamodb_table_arn" {
  description = "ARN of the DynamoDB table"
  value       = module.dynamodb.table_arn
}

# ECR outputs (commented out - not needed for ZIP deployment)
# output "ecr_repository_url" {
#   description = "URL of the ECR repository"
#   value       = module.ecr.repository_url
# }
#
# output "ecr_repository_arn" {
#   description = "ARN of the ECR repository"
#   value       = module.ecr.repository_arn
# }

output "lambda_execution_role_arn" {
  description = "ARN of the Lambda execution role"
  value       = module.iam.lambda_execution_role_arn
}

# ECS IAM outputs (commented out - keeping for reference if needed)
# output "task_execution_role_arn" {
#   description = "ARN of the ECS task execution role"
#   value       = module.iam.task_execution_role_arn
# }
#
# output "task_role_arn" {
#   description = "ARN of the ECS task role"
#   value       = module.iam.task_role_arn
# }
#
# output "cloudwatch_log_group" {
#   description = "Name of the CloudWatch log group"
#   value       = module.ecs.log_group_name
# }

output "api_endpoint" {
  description = "API endpoint URL (HTTP)"
  value       = "http://${module.alb.alb_dns_name}/api"
}

output "health_check_endpoint" {
  description = "Health check endpoint URL"
  value       = "http://${module.alb.alb_dns_name}/api/q/health"
}

output "aws_region" {
  description = "AWS region where resources are deployed"
  value       = var.aws_region
}
