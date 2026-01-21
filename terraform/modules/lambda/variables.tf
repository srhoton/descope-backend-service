# Lambda Module Variables

variable "name_prefix" {
  description = "Prefix for resource names"
  type        = string
}

variable "lambda_zip_file" {
  description = "Path to the Lambda deployment ZIP file"
  type        = string
}

variable "lambda_execution_role_arn" {
  description = "ARN of the Lambda execution role"
  type        = string
}

variable "private_subnet_ids" {
  description = "List of private subnet IDs for Lambda VPC configuration"
  type        = list(string)
}

variable "lambda_security_group_id" {
  description = "Security group ID for Lambda function"
  type        = string
}

variable "aws_region" {
  description = "AWS region"
  type        = string
}

variable "dynamodb_table_name" {
  description = "Name of the DynamoDB table"
  type        = string
}

variable "memory_size" {
  description = "Amount of memory in MB for Lambda function"
  type        = number
  default     = 512
}

variable "timeout" {
  description = "Function timeout in seconds"
  type        = number
  default     = 30
}

variable "log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 7
}

variable "enable_function_url" {
  description = "Enable Lambda Function URL for direct invocation"
  type        = bool
  default     = false
}

variable "provisioned_concurrent_executions" {
  description = "Number of provisioned concurrent executions (0 to disable)"
  type        = number
  default     = 0
}

variable "max_provisioned_concurrent_executions" {
  description = "Maximum number of provisioned concurrent executions for auto-scaling"
  type        = number
  default     = 10
}

variable "alb_target_group_arn" {
  description = "ARN of the ALB target group to attach Lambda to"
  type        = string
  default     = ""
}

variable "enable_alb_integration" {
  description = "Whether to attach Lambda to ALB target group"
  type        = bool
  default     = true
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
