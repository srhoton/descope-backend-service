# Variables for Unit Management Service Infrastructure

variable "aws_region" {
  description = "AWS region where resources will be created"
  type        = string
  default     = "us-east-1"

  validation {
    condition     = can(regex("^[a-z]{2}-[a-z]+-\\d{1}$", var.aws_region))
    error_message = "Must be a valid AWS region format (e.g., us-east-1)."
  }
}

variable "project_name" {
  description = "Name of the project"
  type        = string
  default     = "unit-management"

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.project_name))
    error_message = "Project name must contain only lowercase letters, numbers, and hyphens."
  }
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be dev, staging, or prod."
  }
}

# VPC Configuration - Use existing VPC
variable "existing_vpc_id" {
  description = "ID of the existing VPC to use (fb-vpc)"
  type        = string
  default     = "vpc-03163f35ccd0fc6a9"

  validation {
    condition     = can(regex("^vpc-[a-z0-9]+$", var.existing_vpc_id))
    error_message = "VPC ID must be in the format vpc-xxxxxxxxx."
  }
}

# DynamoDB Configuration
variable "dynamodb_table_name" {
  description = "Name of the DynamoDB table for units"
  type        = string
  default     = "units-table"
}

variable "dynamodb_billing_mode" {
  description = "DynamoDB billing mode (PROVISIONED or PAY_PER_REQUEST)"
  type        = string
  default     = "PAY_PER_REQUEST"

  validation {
    condition     = contains(["PROVISIONED", "PAY_PER_REQUEST"], var.dynamodb_billing_mode)
    error_message = "Billing mode must be PROVISIONED or PAY_PER_REQUEST."
  }
}

variable "dynamodb_read_capacity" {
  description = "Read capacity units (only used if billing_mode is PROVISIONED)"
  type        = number
  default     = 5

  validation {
    condition     = var.dynamodb_read_capacity >= 1
    error_message = "Read capacity must be at least 1."
  }
}

variable "dynamodb_write_capacity" {
  description = "Write capacity units (only used if billing_mode is PROVISIONED)"
  type        = number
  default     = 5

  validation {
    condition     = var.dynamodb_write_capacity >= 1
    error_message = "Write capacity must be at least 1."
  }
}

# ECR Configuration (commented out - not needed for ZIP deployment)
# variable "ecr_image_tag_mutability" {
#   description = "Image tag mutability setting for ECR repository"
#   type        = string
#   default     = "MUTABLE"
#
#   validation {
#     condition     = contains(["MUTABLE", "IMMUTABLE"], var.ecr_image_tag_mutability)
#     error_message = "Image tag mutability must be MUTABLE or IMMUTABLE."
#   }
# }
#
# variable "ecr_scan_on_push" {
#   description = "Enable image scanning on push to ECR"
#   type        = bool
#   default     = true
# }
#
# variable "ecr_image_retention_count" {
#   description = "Number of images to retain in ECR"
#   type        = number
#   default     = 10
#
#   validation {
#     condition     = var.ecr_image_retention_count >= 1
#     error_message = "Image retention count must be at least 1."
#   }
# }

# Lambda Configuration
variable "lambda_zip_file" {
  description = "Path to the Lambda deployment ZIP file"
  type        = string
  default     = "../build/function.zip"

  validation {
    condition     = can(regex("\\.zip$", var.lambda_zip_file))
    error_message = "Lambda ZIP file must have a .zip extension."
  }
}

variable "lambda_memory_size" {
  description = "Amount of memory in MB for Lambda function"
  type        = number
  default     = 512

  validation {
    condition     = var.lambda_memory_size >= 128 && var.lambda_memory_size <= 10240
    error_message = "Lambda memory must be between 128 and 10240 MB."
  }
}

variable "lambda_timeout" {
  description = "Function timeout in seconds"
  type        = number
  default     = 30

  validation {
    condition     = var.lambda_timeout >= 1 && var.lambda_timeout <= 900
    error_message = "Lambda timeout must be between 1 and 900 seconds."
  }
}

variable "lambda_enable_function_url" {
  description = "Enable Lambda Function URL for direct invocation (in addition to ALB)"
  type        = bool
  default     = false
}

variable "lambda_provisioned_concurrent_executions" {
  description = "Number of provisioned concurrent executions (0 to disable)"
  type        = number
  default     = 0

  validation {
    condition     = var.lambda_provisioned_concurrent_executions >= 0
    error_message = "Provisioned concurrent executions must be non-negative."
  }
}

variable "lambda_max_provisioned_concurrent_executions" {
  description = "Maximum number of provisioned concurrent executions for auto-scaling"
  type        = number
  default     = 10

  validation {
    condition     = var.lambda_max_provisioned_concurrent_executions >= 1
    error_message = "Max provisioned concurrent executions must be at least 1."
  }
}

# ECS Configuration (kept for reference if needed)
variable "container_cpu" {
  description = "CPU units for the container (1024 = 1 vCPU)"
  type        = number
  default     = 512

  validation {
    condition     = contains([256, 512, 1024, 2048, 4096], var.container_cpu)
    error_message = "CPU must be one of: 256, 512, 1024, 2048, 4096."
  }
}

variable "container_memory" {
  description = "Memory for the container in MB"
  type        = number
  default     = 1024

  validation {
    condition     = var.container_memory >= 512 && var.container_memory <= 30720
    error_message = "Memory must be between 512 and 30720 MB."
  }
}

variable "ecs_desired_count" {
  description = "Desired number of ECS tasks"
  type        = number
  default     = 2

  validation {
    condition     = var.ecs_desired_count >= 1
    error_message = "Desired count must be at least 1."
  }
}

variable "ecs_min_capacity" {
  description = "Minimum number of ECS tasks for autoscaling"
  type        = number
  default     = 2

  validation {
    condition     = var.ecs_min_capacity >= 1
    error_message = "Minimum capacity must be at least 1."
  }
}

variable "ecs_max_capacity" {
  description = "Maximum number of ECS tasks for autoscaling"
  type        = number
  default     = 10

  validation {
    condition     = var.ecs_max_capacity >= var.ecs_min_capacity
    error_message = "Maximum capacity must be greater than or equal to minimum capacity."
  }
}

# ALB Configuration
variable "existing_alb_arn" {
  description = "ARN of the existing Application Load Balancer"
  type        = string
  default     = "arn:aws:elasticloadbalancing:us-west-2:345594586248:loadbalancer/app/external-private-alb/720e2b5474d3d602"

  validation {
    condition     = can(regex("^arn:aws:elasticloadbalancing:[a-z0-9-]+:[0-9]+:loadbalancer/app/[a-zA-Z0-9-]+/[a-z0-9]+$", var.existing_alb_arn))
    error_message = "Must be a valid ALB ARN."
  }
}

variable "alb_certificate_arn" {
  description = "ARN of ACM certificate for HTTPS listener (optional)"
  type        = string
  default     = ""
}

variable "alb_listener_priority" {
  description = "Priority for the ALB listener rule (must be unique per listener)"
  type        = number
  default     = 200

  validation {
    condition     = var.alb_listener_priority >= 1 && var.alb_listener_priority <= 50000
    error_message = "Listener priority must be between 1 and 50000."
  }
}

variable "alb_enable_deletion_protection" {
  description = "Enable deletion protection for ALB"
  type        = bool
  default     = true
}

# Logging Configuration
variable "log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 7

  validation {
    condition = contains([
      1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180,
      365, 400, 545, 731, 1827, 3653
    ], var.log_retention_days)
    error_message = "Log retention days must be a valid CloudWatch Logs retention period."
  }
}
