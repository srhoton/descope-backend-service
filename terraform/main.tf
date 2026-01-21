# Main Terraform configuration for Unit Management Service
# This file orchestrates all infrastructure components

terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Backend configuration should be customized per environment
  # backend "s3" {
  #   bucket         = "your-terraform-state-bucket"
  #   key            = "unit-management-service/terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  #   dynamodb_table = "terraform-state-lock"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}

# Local variables
locals {
  name_prefix = "${var.project_name}-${var.environment}"

  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "Terraform"
    Application = "unit-management-service"
  }
}

# VPC Module - Uses existing VPC
module "vpc" {
  source = "./modules/vpc"

  name_prefix     = local.name_prefix
  existing_vpc_id = var.existing_vpc_id

  tags = local.common_tags
}

# Security Groups Module
module "security_groups" {
  source = "./modules/security"

  name_prefix = local.name_prefix
  vpc_id      = module.vpc.vpc_id
  vpc_cidr    = module.vpc.vpc_cidr

  tags = local.common_tags
}

# DynamoDB Module
module "dynamodb" {
  source = "./modules/dynamodb"

  table_name     = var.dynamodb_table_name
  billing_mode   = var.dynamodb_billing_mode
  read_capacity  = var.dynamodb_read_capacity
  write_capacity = var.dynamodb_write_capacity

  tags = local.common_tags
}

# ECR Repository Module (commented out - not needed for ZIP deployment)
# module "ecr" {
#   source = "./modules/ecr"
#
#   repository_name       = "${var.project_name}-${var.environment}"
#   image_tag_mutability  = var.ecr_image_tag_mutability
#   scan_on_push          = var.ecr_scan_on_push
#   image_retention_count = var.ecr_image_retention_count
#
#   tags = local.common_tags
# }

# IAM Module
module "iam" {
  source = "./modules/iam"

  name_prefix          = local.name_prefix
  dynamodb_table_arn   = module.dynamodb.table_arn
  dynamodb_kms_key_arn = module.dynamodb.kms_key_arn

  tags = local.common_tags
}

# Application Load Balancer Module
# Uses existing ALB instead of creating a new one
module "alb" {
  source = "./modules/alb"

  name_prefix           = local.name_prefix
  existing_alb_arn      = var.existing_alb_arn
  vpc_id                = module.vpc.vpc_id
  certificate_arn       = var.alb_certificate_arn
  enable_http_listener  = false  # Existing ALB only has HTTPS
  enable_https_listener = true   # Use the HTTPS listener
  listener_priority     = var.alb_listener_priority

  tags = local.common_tags
}

# Lambda Function Module
module "lambda" {
  source = "./modules/lambda"

  name_prefix                           = local.name_prefix
  lambda_zip_file                       = var.lambda_zip_file
  lambda_execution_role_arn             = module.iam.lambda_execution_role_arn
  private_subnet_ids                    = module.vpc.private_subnet_ids
  lambda_security_group_id              = module.security_groups.lambda_security_group_id
  aws_region                            = var.aws_region
  dynamodb_table_name                   = var.dynamodb_table_name
  memory_size                           = var.lambda_memory_size
  timeout                               = var.lambda_timeout
  log_retention_days                    = var.log_retention_days
  enable_function_url                   = var.lambda_enable_function_url
  provisioned_concurrent_executions     = var.lambda_provisioned_concurrent_executions
  max_provisioned_concurrent_executions = var.lambda_max_provisioned_concurrent_executions
  alb_target_group_arn                  = module.alb.target_group_arn
  enable_alb_integration                = true

  tags = local.common_tags
}

# ECS Cluster Module (commented out - keeping for reference if needed)
# module "ecs" {
#   source = "./modules/ecs"
#
#   name_prefix             = local.name_prefix
#   vpc_id                  = module.vpc.vpc_id
#   private_subnet_ids      = module.vpc.private_subnet_ids
#   ecs_security_group_id   = module.security_groups.ecs_security_group_id
#   task_execution_role_arn = module.iam.task_execution_role_arn
#   task_role_arn           = module.iam.task_role_arn
#   alb_target_group_arn    = module.alb.target_group_arn
#   ecr_repository_url      = module.ecr.repository_url
#   container_image_tag     = var.container_image_tag
#   container_cpu           = var.container_cpu
#   container_memory        = var.container_memory
#   desired_count           = var.ecs_desired_count
#   min_capacity            = var.ecs_min_capacity
#   max_capacity            = var.ecs_max_capacity
#   aws_region              = var.aws_region
#   dynamodb_table_name     = var.dynamodb_table_name
#   log_retention_days      = var.log_retention_days
#
#   tags = local.common_tags
# }
